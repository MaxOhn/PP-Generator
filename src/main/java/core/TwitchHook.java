package main.java.core;

import com.google.common.util.concurrent.RateLimiter;
import com.mb3364.twitch.api.Twitch;
import com.mb3364.twitch.api.handlers.StreamResponseHandler;
import com.mb3364.twitch.api.models.Stream;
import main.java.util.secrets;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

public class TwitchHook {

    private Twitch twitch;
    private static HashMap<String, ArrayList<String>> streamers;
    private static ArrayList<String> isOnline = new ArrayList<>();
    private static Logger logger = Logger.getLogger("TwitchHook");
    private RateLimiter limiter = RateLimiter.create(2.5);

    public TwitchHook() {
        twitch = new Twitch();
        twitch.setClientId(secrets.twitchClientID);
        loadStreamers();
        trackStreamers();
    }

    public HashMap<String, ArrayList<String>> getStreamers() {
        return streamers;
    }

    public ArrayList<String> getIsOnline() {
        return isOnline;
    }

    private void loadStreamers() {
        try {
                streamers = secrets.WITH_DB ? DBProvider.getTwitch() : new HashMap<>();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not load streamers:");
            e.printStackTrace();
            streamers = new HashMap<>();
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
            for (String streamer : streamers.keySet()) {
                limiter.acquire();
                twitch.streams().get(streamer, new StreamResponseHandler() {
                    @Override
                    public void onSuccess(Stream stream) {
                        if (stream != null && stream.isOnline()) {
                            if (!isOnline.contains(streamer)) {
                                isOnline.add(streamer);
                                logger.info(stream.getChannel().getName() + " now playing: " + stream.getGame());
                                for (String channelID : streamers.get(streamer)) {
                                    streamMessage(stream, channelID);
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
        });
        t.start();
    }

    public boolean addStreamer(String streamer, String channelID) {
        String streamerLower = streamer.toLowerCase();
        try {
            if (secrets.WITH_DB)
                DBProvider.addStreamer(streamerLower, channelID);
            if (streamers.containsKey(streamerLower))
                streamers.get(streamerLower).add(channelID);
            else
                streamers.put(streamerLower, new ArrayList<>(Collections.singletonList(channelID)));
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not add streamer \"" + streamer + "\": " + e);
        }
        return false;
    }

    public boolean removeStreamer(String streamer, String channelID) {
        String streamerLower = streamer.toLowerCase();
        boolean removedFromHashMap = false;
        try {
            if (secrets.WITH_DB)
                DBProvider.removeStreamer(streamerLower, channelID);
            if (streamers.keySet().contains(streamerLower) && streamers.get(streamerLower).contains(channelID)) {
                if (streamers.get(streamerLower).size() == 1)
                    removedFromHashMap = streamers.remove(streamerLower, streamers.get(streamerLower));
                else
                    removedFromHashMap = streamers.get(streamerLower).remove(channelID);
            }
            return removedFromHashMap;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not remove streamer \"" + streamer + "\": " + e);
        }
        return false;
    }

    public String trackedStreamers(String channelID) {
        ArrayList<String> output = new ArrayList<>();
        for (String streamer: streamers.keySet()) {
            if (streamers.get(streamer).contains(channelID))
                output.add(streamer);
        }
        Collections.sort(output);
        return output.toString();
    }

    public boolean isTracked(String streamer, String channelID) {
        return streamers.keySet().contains(streamer) && streamers.get(streamer).contains(channelID);
    }

    private void streamMessage(Stream stream, String channelID) {
        String name = stream.getChannel().getName();
        String game = stream.getGame();
        String filler = !game.equals("")? " is playing " + game: " now live";
        String url = stream.getChannel().getUrl();
        Main.jda.getTextChannelById(channelID).sendMessage("[Now online]: `" + name + "`" + filler + " on " + url).queue();
    }
}
