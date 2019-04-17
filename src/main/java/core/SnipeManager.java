package main.java.core;

import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SnipeManager {

    private static SnipeManager snipeManager;

    private TreeMap<Integer, String[]> rankings;
    private List<Integer> failedIds = new ArrayList<>();

    private boolean isUpdatingIDs = false;
    private boolean isUpdatingRankings = false;
    private boolean interruptIdUpdating = false;
    private boolean interruptRankingUpdating = false;

    private Logger logger;
    private Osu osu;
    private CustomRequester scoreRequester;

    private SnipeManager(Osu osu) {
        this.osu = osu;
        logger = Logger.getLogger(this.getClass());
        scoreRequester = new CustomRequester();
        try {
            rankings = DBProvider.getRankings();
        } catch (SQLException | ClassNotFoundException e) {
            rankings = new TreeMap<>();
            logger.error("Something went wrong while retrieving mapRanking map id's:");
            e.printStackTrace();
        }
    }

    public static SnipeManager getInstance(Osu osu) {
        if (snipeManager == null) snipeManager = new SnipeManager(osu);
        return snipeManager;
    }

    public void setInterruptIdUpdating() {
        if (isUpdatingIDs) this.interruptIdUpdating = true;
    }

    public void updateMapIds() {
        isUpdatingIDs = true;
        final Thread t = new Thread(() -> {
            ZonedDateTime sinceDate = ZonedDateTime.of(2007,1,1,0,0,0,0, ZoneId.of("UTC+0"));
            if (rankings.size() > 0) {
                try {
                    sinceDate = osu.beatmaps.query(
                            new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(rankings.lastKey()).setLimit(1).build()
                    ).get(0).getApprovedDate();
                } catch (OsuAPIException ignored) {}
            }
            List<OsuBeatmap> newMaps = new ArrayList<>();
            List<Integer> newMapIDs = new ArrayList<>();
            boolean success = true;
            do {
                if (interruptIdUpdating) {
                    interruptIdUpdating = false;
                    return;
                }
                if (success) {
                    success = false;
                    try {
                        newMaps = osu.beatmaps.query(
                                new EndpointBeatmaps.ArgumentsBuilder().setSince(sinceDate).setLimit(500).build()
                        );
                    } catch (OsuAPIException e) {
                        continue;
                        // sinceDate stays the same, next iteration will try to retrieve the same maps again
                    }
                    newMapIDs = newMaps.stream()
                            .filter(map -> !rankings.keySet().contains(map.getID()))
                            .sorted(Comparator.comparingLong(map -> map.getApprovedDate().toEpochSecond()))
                            .map(OsuBeatmap::getID)
                            .collect(Collectors.toList());
                }
                try {
                    if (newMapIDs.size() > 0) {
                        sinceDate = newMaps.get(newMaps.size() - 1).getApprovedDate();
                        DBProvider.addMaps(newMapIDs);
                        for (Integer id : newMapIDs) rankings.put(id, new String[10]);
                        success = true;
                        logger.info("Current date: " + sinceDate.toString());
                    }
                } catch (SQLException | ClassNotFoundException ignored) {
                    logger.warn("Error while adding new map id, trying again now");
                    // success is still false, next iteration will try to add newMapIDs to DB again
                }
            } while (newMapIDs.size() > 0);
            logger.info("Done updating beatmap ids");
            isUpdatingIDs = false;
        });
        t.start();
    }

    public void updateRankings() {
        updateRankings(rankings.firstKey());
    }

    public void setInterruptRankingUpdating() {
        if (isUpdatingRankings) this.interruptRankingUpdating = true;
    }

    public void updateRankings(int startingID) {
        isUpdatingRankings = true;
        final Thread t = new Thread(() -> {
            Iterator it = rankings.keySet().iterator();
            while (it.hasNext()) {
                if ((Integer)it.next() == startingID)
                    break;
            }
            while (it.hasNext()) {
                if (interruptRankingUpdating) {
                    interruptRankingUpdating = false;
                    return;
                }
                String mapID = it.next() + "";
                try {
                    String[] scores = getScores(mapID);

                    if (scores.length > 0)
                        logger.info("User " + scores[0] + " is first on map id " + mapID);
                    else
                        logger.info("No one is first place on map id " + mapID);

                    /*
                     *
                     * /!\ CHECK HERE IF SCORES WERE SNIPED /!\
                     *
                     */

                    DBProvider.updateRanking(mapID, scores);
                } catch (IOException | JSONException e) {
                    logger.warn("Data retrieval error for mapID " + mapID);
                    failedIds.add(Integer.parseInt(mapID));
                } catch (SQLException | ClassNotFoundException e) {
                    logger.warn("Database error while updating ranking of mapID " + mapID);
                    failedIds.add(Integer.parseInt(mapID));
                }
            }
            logger.info("Done updating rankings");
            isUpdatingRankings = false;
        });
        t.start();
    }

    private String[] getScores(String mapID) throws IOException {
        JSONArray scores = scoreRequester.getScores(mapID);
        List<String> scoreList = new ArrayList<>();
        for (int i = 0, n = Math.min(10, scores.length()); i < n; i++) {
            scoreList.add(scores.getJSONObject(i).getInt("user_id") + "");
        }
        return scoreList.toArray(new String[0]);
    }
}
