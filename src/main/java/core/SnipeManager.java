package main.java.core;

import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SnipeManager {

    private Logger logger;
    private static SnipeManager snipeManager;
    private TreeSet<Integer> mapIDs;
    private boolean isUpdatingIDs;
    private boolean interruptIdUpdating;
    private Osu osu;

    private SnipeManager(Osu osu) {
        this.osu = osu;
        isUpdatingIDs = false;
        interruptIdUpdating = false;
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
            ZonedDateTime sinceDate = ZonedDateTime.parse("2007-1-1 00:00:00");
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
                    System.out.println(sinceDate.toString());
                    try {
                        newMaps = osu.beatmaps.query(
                                new EndpointBeatmaps.ArgumentsBuilder().setSince(sinceDate).setLimit(500).build()
                        );
                    } catch (OsuAPIException e) {
                        continue;
                        // sinceDate stays the same, next iteration will try to retrieve the same maps again
                    }
                    sinceDate = newMaps.get(newMaps.size() - 1).getApprovedDate();
                    newMapIDs = newMaps.stream()
                            .map(OsuBeatmap::getID)
                            .filter(map -> mapIDs.contains(map))
                            .collect(Collectors.toList());
                }
                try {
                    if (newMapIDs.size() > 0) {
                        DBProvider.addMaps(newMapIDs);
                        success = true;
                        //Thread.sleep(1500);
                    }
                } catch (SQLException | ClassNotFoundException ignored) {
                    logger.warn("Error while adding new map id, trying again now");
                    // success is still false, next iteration will try to add newMapIDs to DB again
                }
            } while (newMapIDs.size() > 0);
            isUpdatingIDs = false;
        });
        t.start();
    }
}
