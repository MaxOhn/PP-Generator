package main.java.core;

import de.maxikg.osuapi.client.DefaultOsuClient;
import de.maxikg.osuapi.model.Beatmap;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SnipeManager {

    private Logger logger;
    private static SnipeManager snipeManager;
    private TreeSet<Integer> mapIDs;
    private boolean isUpdatingIDs;
    private boolean interruptIdUpdating;
    private DefaultOsuClient osu;

    private SnipeManager(DefaultOsuClient osu) {
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

    public static SnipeManager getInstance(DefaultOsuClient osu) {
        if (snipeManager == null) snipeManager = new SnipeManager(osu);
        return snipeManager;
    }

    public void setInterruptIdUpdating() {
        if (isUpdatingIDs) this.interruptIdUpdating = true;
    }

    public void updateMapIds() {
        isUpdatingIDs = true;
        final Thread t = new Thread(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, 2007);
            calendar.set(Calendar.MONTH, 0);
            calendar.set(Calendar.DATE, 1);
            Date sinceDate = calendar.getTime();
            if (mapIDs.size() > 0) {
                try {
                    sinceDate = osu.getBeatmaps()
                            .beatmapId(mapIDs.last())
                            .limit(1)
                            .query()
                            .iterator()
                            .next()
                            .getApprovedDate();
                } catch (Exception ignored) {}
            }
            List<Integer> newMaps = new ArrayList<>();
            boolean success = true;
            do {
                if (interruptIdUpdating) {
                    interruptIdUpdating = false;
                    return;
                }
                if (success) {
                    success = false;
                    System.out.println(sinceDate.toString());
                    Collection<Beatmap> temp = osu.getBeatmaps()
                            .since(sinceDate)
                            .limit(5)
                            .query();
                    newMaps = temp.stream()
                            .map(Beatmap::getBeatmapId)
                            .filter(map -> mapIDs.contains(map))
                            .collect(Collectors.toList());
                }
                try {
                    if (newMaps.size() > 0) {
                        DBProvider.addMaps(newMaps);
                        success = true;
                        Thread.sleep(1500);
                    }
                } catch (InterruptedException | SQLException | ClassNotFoundException ignored) {
                    logger.warn("Error while adding new map id, trying again now");
                    // success is still false, next iteration will try to add newMaps to DB again
                }
            } while (newMaps.size() > 0);
            isUpdatingIDs = false;
        });
        t.start();
    }
}
