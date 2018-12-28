package main.java.core;

import com.mb3364.twitch.api.Twitch;
import com.mb3364.twitch.api.handlers.StreamResponseHandler;
import com.mb3364.twitch.api.models.Stream;
import main.java.util.secrets;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class TwitchHook {

    private Twitch twitch;
    private static HashMap<String, ArrayList<String>> streamers;
    private static ArrayList<String> isOnline = new ArrayList<>();
    private static Logger logger = Logger.getLogger("TwitchHook");

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
            streamers = DBProvider.getTwitch();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not load streamers: " + e);
            streamers = new HashMap<>();
        }
    }

    private void trackStreamers() {
        int trackDelay = 11;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable twitchIterator = () -> {
            for (String streamer : streamers.keySet()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warn("Thread.sleep in trackStreamers was interrupted: " + e);
                }
                twitch.streams().get(streamer, new StreamResponseHandler() {
                    @Override
                    public void onSuccess(Stream stream) {
                        if (stream != null && stream.isOnline()) {
                            for (String channelID : streamers.get(streamer)) {
                                if (!isOnline.contains(streamer)) {
                                    isOnline.add(streamer);
                                    logger.info(stream.getChannel().getName() + " now playing: " + stream.getGame());
                                    streamMessage(stream, channelID);
                                }
                            }
                        } else
                            isOnline.remove(streamer);
                    }

                    @Override
                    public void onFailure(int i, String s, String s1) {
                        logger.info("onFailure: " + i + ", " + s + ", " + s1 + " (remove " + streamer + ")");
                        isOnline.remove(streamer);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.info("onFailure: " + throwable.getMessage() + " (remove " + streamer + ")");
                        isOnline.remove(streamer);
                    }
                });
            }
        };
        scheduler.scheduleAtFixedRate(twitchIterator, trackDelay, trackDelay, MINUTES);
    }

    public boolean addStreamer(String streamer, String channelID) {
        try {
            DBProvider.addStreamer(streamer, channelID);
            if (streamers.containsKey(streamer))
                streamers.get(streamer).add(channelID);
            else
                streamers.put(streamer, new ArrayList<>(Collections.singletonList(channelID)));
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not add streamer \"" + streamer + "\": " + e);
        }
        return false;
    }

    public boolean removeStreamer(String streamer, String channelID) {
        boolean removedFromHashMap = false;
        try {
            DBProvider.removeStreamer(streamer, channelID);
            if (streamers.keySet().contains(streamer) && streamers.get(streamer).contains(channelID)) {
                if (streamers.get(streamer).size() == 1)
                    removedFromHashMap = streamers.remove(streamer, streamers.get(streamer));
                else
                    removedFromHashMap = streamers.get(streamer).remove(channelID);
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

    public boolean isTracked(String streamer) {
        return streamers.keySet().contains(streamer);
    }

    private void streamMessage(Stream stream, String channelID) {
        String name = stream.getChannel().getName();
        String game = stream.getGame();
        String filler = !game.equals("")? " is playing " + game: " now live";
        String url = stream.getChannel().getUrl();
        Main.streamerOnline("[Now online]: `" + name + "`" + filler + " on " + url, channelID);
    }
}
