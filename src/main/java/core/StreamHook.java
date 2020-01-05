package main.java.core;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.google.common.util.concurrent.RateLimiter;
import com.mixer.api.MixerAPI;
import com.mixer.api.resource.channel.MixerChannel;
import com.mixer.api.services.impl.ChannelsService;
import com.netflix.hystrix.HystrixCommand;
import main.java.util.secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

public class StreamHook {

    private TwitchClient twitch;
    private MixerAPI mixer;
    private static HashMap<String, ArrayList<String>> twitchStreamers;
    private static HashMap<Long, User> twitchUsers = new HashMap<>();
    private static HashMap<String, ArrayList<String>> mixerStreamers;
    private static HashSet<String> isOnline = new HashSet<>();
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private RateLimiter limiter = RateLimiter.create(2.5);

    public StreamHook() {
        twitch = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withClientId(secrets.twitchClientID)
                .withClientSecret(secrets.twitchSecret)
                .build();
        twitch.getErrorTrackingManager().setErrorTrackers(new HashSet<>()); // Who needs error msgs anyway
        mixer = new MixerAPI(secrets.mixerClientID);
        loadStreamers();
        trackStreamers();
    }

    // Return a hashmap of all streamer names and the channels which are notified once the streamer comes online
    public HashMap<String, ArrayList<String>> getStreamers() {
        HashMap<String, ArrayList<String>> allStreamers = new HashMap<>();
        for (String streamer : twitchStreamers.keySet()) {
            allStreamers.put(streamer, twitchStreamers.get(streamer));
        }
        for (String streamer : mixerStreamers.keySet()) {
            if (allStreamers.containsKey(streamer))
                allStreamers.get(streamer).addAll(mixerStreamers.get(streamer));
            else
                allStreamers.put(streamer, mixerStreamers.get(streamer));
        }
        return allStreamers;
    }

    // Returns hashset of streamer names who are currently online
    public HashSet<String> getIsOnline() {
        return isOnline;
    }

    // Load all registered streamers from the database
    private void loadStreamers() {
        try {
            if (secrets.WITH_DB) {
                twitchStreamers = DBProvider.getTwitch();
                mixerStreamers = DBProvider.getMixer();
            } else {
                twitchStreamers = new HashMap<>();
                mixerStreamers = new HashMap<>();
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not load streamers:", e);
            twitchStreamers = new HashMap<>();
            mixerStreamers = new HashMap<>();
        }
        UserList users = twitch.getHelix().getUsers(null, null, new ArrayList<>(twitchStreamers.keySet())).execute();
        users.getUsers().forEach(user -> {
            if (!twitchUsers.containsKey(user.getId()))
                twitchUsers.put(user.getId(), user);
        });
    }

    // Prepare the scheduler to check for changes in online-activity of all streamers in regular intervals
    private void trackStreamers() {
        int trackDelay = 10;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable twitchIterator = this::streamerCheckIteration;
        scheduler.scheduleAtFixedRate(twitchIterator, trackDelay, trackDelay, MINUTES);
    }

    public void streamerCheckIteration() {
        new Thread(() -> {

            // Handling twitch
            if (platformIsAvailable("twitch")) {
                HystrixCommand hystrixGetAllStreams = twitch.getHelix().getStreams(null,null, null, twitchStreamers.size(),
                        null, null, null, null, new ArrayList<>(twitchStreamers.keySet()));
                List<Stream> streams = ((StreamList)hystrixGetAllStreams.execute()).getStreams();
                twitchUsers.values().forEach(user -> {
                    Optional<Stream> stream = streams.stream().filter(s -> s.getUserId().equals(user.getId())).findFirst();
                    if (stream.isPresent()) {
                        if (!isOnline.contains(user.getDisplayName())) {
                            for (String channelID : twitchStreamers.get(user.getDisplayName().toLowerCase())) {
                                Main.jda.getTextChannelById(channelID).sendMessage("`" + user.getDisplayName()
                                        + "` now online on https://www.twitch.tv/" + user.getDisplayName() + "\nTitle: `"
                                        + stream.get().getTitle().trim() + "`").queue();
                            }
                            logger.info("Twitch user " + user.getDisplayName() + " just went online");
                            isOnline.add(user.getDisplayName());
                        }
                    } else {
                        if (isOnline.contains(user.getDisplayName())) {
                            isOnline.remove(user.getDisplayName());
                            logger.info("Twitch user " + user.getDisplayName() + " now offline again");
                        }
                    }
                });
            } else logger.warn("Could not reach twitch.tv, internet down?");

            // Handling mixer
            if (platformIsAvailable("mixer")) {
                for (String streamer : mixerStreamers.keySet()) {
                    limiter.acquire();
                    try {
                        MixerChannel channel = mixer.use(ChannelsService.class).findOneByToken(streamer).get();
                        if (channel.online) {
                            if (!isOnline.contains(streamer)) {
                                for (String channelID : mixerStreamers.get(streamer)) {
                                    Main.jda.getTextChannelById(channelID).sendMessage("`" + channel.token
                                            + "` now online on https://www.mixer.com/" + channel.token + "\nTitle: `" + channel.name.trim() + "`").queue();
                                }
                                logger.info("Mixer user " + streamer + " just went online");
                                isOnline.add(streamer);
                            }
                        } else if (isOnline.contains(streamer)) {
                            logger.info("Mixer user " + streamer + " now offline again");
                            isOnline.remove(streamer);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.warn("Exception while getting channel of mixer user " + streamer + ", internet down?");
                    }
                }
            } else logger.warn("Could not reach mixer.com, internet down?");
        }).start();
    }

    // Simple check if the bot can connect to the internet / the platforms
    private static boolean platformIsAvailable(String platform) {
        try {
            final URL url;
            switch (platform) {
                case "twitch": url = new URL("https://www.twitch.tv"); break;
                case "mixer": url = new URL("http://www.mixer.com"); break;
                default: throw new RuntimeException("Platform must be 'twitch' or 'mixer'");
            }
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return false;
        }
    }

    // Add new streamer / notification channel for streamer to the database
    public boolean addStreamer(String streamer, String channelID, String platform) {
        String streamerLower = streamer.toLowerCase();
        try {
            if (secrets.WITH_DB)
                DBProvider.addStreamer(streamerLower, channelID, platform);
            if (platform.equals("twitch")) {
                if (twitchStreamers.containsKey(streamerLower))
                    twitchStreamers.get(streamerLower).add(channelID);
                else {
                    twitch.getClientHelper().enableStreamEventListener(streamerLower);
                    twitchStreamers.put(streamerLower, new ArrayList<>(Collections.singletonList(channelID)));
                }
            } else if (platform.equals("mixer")) {
                if (mixerStreamers.containsKey(streamerLower))
                    mixerStreamers.get(streamerLower).add(channelID);
                else
                    mixerStreamers.put(streamerLower, new ArrayList<>(Collections.singletonList(channelID)));
            }
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not add streamer \"" + streamer + "\" (" + platform + "):");
            e.printStackTrace();
        }
        return false;
    }

    // Remove streamer-channel combo from database so that the channel is no longer notified when the streamer comes online
    public boolean removeStreamer(String streamer, String channelID, String platform) {
        String streamerLower = streamer.toLowerCase();
        boolean removedFromHashMap = false;
        try {
            if (secrets.WITH_DB)
                DBProvider.removeStreamer(streamerLower, channelID, platform);
            if (platform.equals("twitch")) {
                if (twitchStreamers.containsKey(streamerLower) && twitchStreamers.get(streamerLower).contains(channelID)) {
                    if (twitchStreamers.get(streamerLower).size() == 1)
                        removedFromHashMap = twitchStreamers.remove(streamerLower, twitchStreamers.get(streamerLower));
                    else
                        removedFromHashMap = twitchStreamers.get(streamerLower).remove(channelID);
                }
            } else if (platform.equals("mixer")) {
                if (mixerStreamers.containsKey(streamerLower) && mixerStreamers.get(streamerLower).contains(channelID)) {
                    if (mixerStreamers.get(streamerLower).size() == 1)
                        removedFromHashMap = mixerStreamers.remove(streamerLower, mixerStreamers.get(streamerLower));
                    else
                        removedFromHashMap = mixerStreamers.get(streamerLower).remove(channelID);
                }
            }
            return removedFromHashMap;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not remove streamer \"" + streamer + "\" (" + platform + "):");
            e.printStackTrace();
        }
        return false;
    }

    // Returns a string of all streamer names tracked in the given channel
    public String trackedStreamers(String channelID) {
        ArrayList<String> output = new ArrayList<>();
        for (String streamer: twitchStreamers.keySet()) {
            if (twitchStreamers.get(streamer).contains(channelID))
                output.add(streamer);
        }
        Collections.sort(output);
        String outputStr = "Twitch: " + output.toString() + "\n";
        output = new ArrayList<>();
        for (String streamer:mixerStreamers.keySet()) {
            if (mixerStreamers.get(streamer).contains(channelID))
                output.add(streamer);
        }
        Collections.sort(output);
        return outputStr + "Mixer: " + output.toString();
    }

    // Check whether a given streamer is being tracked in a given channel
    public boolean isTracked(String streamer, String channelID) {
        return twitchStreamers.containsKey(streamer) && twitchStreamers.get(streamer).contains(channelID)
                || mixerStreamers.containsKey(streamer) && mixerStreamers.get(streamer).contains(channelID);
    }
}
