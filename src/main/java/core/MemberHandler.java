package main.java.core;

import main.java.util.secrets;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.DAYS;

public class MemberHandler {

    private Logger logger = Logger.getLogger(this.getClass());
    private HashMap<String, ZonedDateTime> uncheckedUsers = new HashMap<>();
    private final int uncheckedKickDelay = 10;

    public MemberHandler() {
        try {
            if (secrets.WITH_DB)
                uncheckedUsers = DBProvider.getUncheckedUsers();
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not retrieve unchecked users:");
            e.printStackTrace();
        }
        trackUncheckedUsers();
    }

    public void addUncheckedUser(String discord, ZonedDateTime date) {
        uncheckedUsers.put(discord, date);
        try {
            if (secrets.WITH_DB)
                DBProvider.addUncheckedUser(discord, date);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not add unchecked user to DB:");
            e.printStackTrace();
        }
    }

    public void checkedUser(String discord) {
        uncheckedUsers.remove(discord);
        try {
            if (secrets.WITH_DB)
                DBProvider.removeUncheckedUser(discord);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not remove unchecked user from DB:");
            e.printStackTrace();
        }
    }

    public void kickUser(String userID, String guildID) {
        while (Main.jda == null);
        if (secrets.RELEASE) {
            try {
                Main.jda.getGuildById(guildID).getController()
                        .kick(userID).reason("No provision of osu profile to a moderator within " + uncheckedKickDelay + " days")
                        .queue();
                uncheckedUsers.remove(userID);
                try {
                    if (secrets.WITH_DB)
                        DBProvider.removeUncheckedUser(userID);
                } catch (ClassNotFoundException | SQLException e1) {
                    logger.error("Could not remove kicked user from DB:");
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                logger.error("Could not kick user " + userID + " from server " + guildID + ":");
                e.printStackTrace();
            }
        }
    }

    private void trackUncheckedUsers() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable kickerIterator = this::kickCheckIteration;
        scheduler.scheduleAtFixedRate(kickerIterator, 0, 1, DAYS);
    }

    private void kickCheckIteration() {
        final Thread t = new Thread(() -> {
            ZonedDateTime now = ZonedDateTime.now();
            for (String discord : uncheckedUsers.keySet()) {
                if (now.isAfter(uncheckedUsers.get(discord).plusDays(uncheckedKickDelay))) {
                    kickUser(discord, secrets.mainGuildID);
                    logger.info("Kicked unchecked user " + discord);
                }
            }
        });
        t.start();
    }
}
