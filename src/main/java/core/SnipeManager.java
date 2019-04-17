package main.java.core;

import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class SnipeManager {

    private Logger logger;
    private static SnipeManager snipeManager;
    private TreeSet<Integer> mapIDs;
    private boolean isUpdatingIDs = false;
    private boolean interruptIdUpdating = false;
    private Osu osu;

    private SnipeManager(Osu osu) {
        this.osu = osu;
        logger = Logger.getLogger(this.getClass());
        try {
            mapIDs = DBProvider.getMapIds();
        } catch (SQLException | ClassNotFoundException e) {
            mapIDs = new TreeSet<>();
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
            if (mapIDs.size() > 0) {
                try {
                    sinceDate = osu.beatmaps.query(
                            new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(mapIDs.last()).setLimit(1).build()
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
                            .filter(map -> !mapIDs.contains(map.getID()))
                            .sorted(Comparator.comparingLong(map -> map.getApprovedDate().toEpochSecond()))
                            .map(OsuBeatmap::getID)
                            .collect(Collectors.toList());
                }
                try {
                    if (newMapIDs.size() > 0) {
                        sinceDate = newMaps.get(newMaps.size() - 1).getApprovedDate();
                        DBProvider.addMaps(newMapIDs);
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
}
