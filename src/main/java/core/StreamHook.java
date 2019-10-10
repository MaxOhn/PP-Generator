package main.java.core;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.common.events.channel.ChannelGoLiveEvent;
import com.github.twitch4j.common.events.channel.ChannelGoOfflineEvent;
import com.mixer.api.MixerAPI;
import main.java.util.secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class StreamHook {

    private TwitchClient twitch;
    private MixerAPI mixer;
    private static HashMap<String, ArrayList<String>> twitchStreamers;
    private static HashMap<String, ArrayList<String>> mixerStreamers;
    private static ArrayList<String> isOnline = new ArrayList<>();
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    //private RateLimiter limiter = RateLimiter.create(2.5);

    public StreamHook() {
        twitch = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withClientId(secrets.twitchClientID)
                .withClientSecret(secrets.twitchSecret)
                .build();
        twitch.getEventManager().onEvent(ChannelGoLiveEvent.class).subscribe(event -> {
            for (String channelID : twitchStreamers.get(event.getChannel().getName())) {
                Main.jda.getTextChannelById(channelID).sendMessage("`" + event.getChannel().getName()
                        + "` now online on https://www.twitch.tv/" + event.getChannel().getName() + "\nTitle: `" + event.getTitle() + "`").queue();
                logger.info("Twitch user " + event.getChannel().getName() + " just went online");
                isOnline.add(event.getChannel().getName());
            }
        });
        twitch.getEventManager().onEvent(ChannelGoOfflineEvent.class).subscribe(event -> {
            logger.info("Twitch user " + event.getChannel().getName() + " now offline again");
            isOnline.remove(event.getChannel().getName());
        });
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
        for (String twitchName: twitchStreamers.keySet())
            twitch.getClientHelper().enableStreamEventListener(twitchName);
        for (String mixerName: mixerStreamers.keySet()) {
            // TODO
        }
    }

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
}
