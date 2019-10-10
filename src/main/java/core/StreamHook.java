package main.java.core;

import com.google.common.util.concurrent.RateLimiter;
import com.mb3364.twitch.api.Twitch;
import com.mb3364.twitch.api.handlers.StreamResponseHandler;
import com.mb3364.twitch.api.models.Stream;
import com.mixer.api.MixerAPI;
import main.java.util.secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

public class StreamHook {

    private Twitch twitch;
    private MixerAPI mixer;
    private static HashMap<String, ArrayList<String>> twitchStreamers;
    private static HashMap<String, ArrayList<String>> mixerStreamers;
    private static ArrayList<String> isOnline = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private RateLimiter limiter = RateLimiter.create(2.5);

    public StreamHook() {
        twitch = new Twitch();
        twitch.setClientId(secrets.twitchClientID);
        mixer = new MixerAPI(secrets.mixerClientID);
        loadStreamers();
        trackStreamers();
    }

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

    public ArrayList<String> getIsOnline() {
        return isOnline;
    }

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
            logger.error("Could not load streamers:");
            e.printStackTrace();
            twitchStreamers = new HashMap<>();
            mixerStreamers = new HashMap<>();
        }
    }

    private void trackStreamers() {
        int trackDelay = 10;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable twitchIterator = this::streamerCheckIteration;
        scheduler.scheduleAtFixedRate(twitchIterator, trackDelay, trackDelay, MINUTES);
    }

    public void streamerCheckIteration() {
        final Thread t = new Thread(() -> {
            for (String streamer : twitchStreamers.keySet()) {
                limiter.acquire();
                twitch.streams().get(streamer, new StreamResponseHandler() {
                    @Override
                    public void onSuccess(Stream stream) {
                        if (stream != null && stream.isOnline()) {
                            if (!isOnline.contains(streamer)) {
                                isOnline.add(streamer);
                                logger.info(stream.getChannel().getName() + " now playing: " + stream.getGame());
                                for (String channelID : twitchStreamers.get(streamer)) {
                                    streamMessage(stream, channelID, "twitch");
                                }
                            }
                        } else isOnline.remove(streamer);
                    }

                    @Override
                    public void onFailure(int i, String s, String s1) {}

                    @Override
                    public void onFailure(Throwable throwable) {}
                });
            }
            /* // TODO
            for (String streamer : mixerStreamers.keySet()) {
                try {
                    MixerChannel mc = mixer.use(ChannelsService.class).findOneByToken(streamer).get();
                    MixerUser mu;
                    if (mc.online) {
                        for (String channelID : twitchStreamers.get(streamer)) {
                            streamMessage(mc., channelID, "mixer");
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            /*/
        });
        t.start();
    }

    public boolean addStreamer(String streamer, String channelID, String platform) {
        String streamerLower = streamer.toLowerCase();
        try {
            if (secrets.WITH_DB)
                DBProvider.addStreamer(streamerLower, channelID, platform);
            if (platform.equals("twitch")) {
                if (twitchStreamers.containsKey(streamerLower))
                    twitchStreamers.get(streamerLower).add(channelID);
                else
                    twitchStreamers.put(streamerLower, new ArrayList<>(Collections.singletonList(channelID)));
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

    public boolean isTracked(String streamer, String channelID) {
        return twitchStreamers.containsKey(streamer) && twitchStreamers.get(streamer).contains(channelID)
                || mixerStreamers.containsKey(streamer) && mixerStreamers.get(streamer).contains(channelID);
    }

    private void streamMessage(Stream stream, String channelID, String platform) {
        String name = stream.getChannel().getName();
        String game = stream.getGame();
        String filler = !game.equals("")? " is playing " + game: " now live";
        String url = stream.getChannel().getUrl();
        Main.jda.getTextChannelById(channelID).sendMessage("[Now online]: `" + name + "`" + filler + " on " + url).queue();
    }
}
