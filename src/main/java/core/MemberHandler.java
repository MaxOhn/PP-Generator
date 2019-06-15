package main.java.core;

import com.google.common.collect.BiMap;
import main.java.util.secrets;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.GuildController;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

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
        runRegularChecks();
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
                uncheckedUsers.remove(userID);
                try {
                    if (secrets.WITH_DB)
                        DBProvider.removeUncheckedUser(userID);
                } catch (ClassNotFoundException | SQLException e1) {
                    logger.error("Could not remove kicked user from DB:");
                    e1.printStackTrace();
                }
                Main.jda.getGuildById(guildID).getController()
                        .kick(userID).reason("No provision of osu profile to a moderator within " + uncheckedKickDelay + " days")
                        .queue();
                logger.info("Kicked unchecked user " + userID);
            } catch (IllegalArgumentException e) {
                logger.warn("User " + userID + " was no longer on the server " + guildID + ", could not be kicked");
            } catch (Exception e) {
                logger.error("Could not kick user " + userID + " from server " + guildID + ":");
                e.printStackTrace();
            }
        }
    }

    private void runRegularChecks() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final Runnable kickerIterator = this::regularIteration;
        scheduler.scheduleAtFixedRate(kickerIterator, 0, 1, DAYS);
    }

    private void regularIteration() {
        final Thread t = new Thread(() -> {

            // Kick unchecked members
            ZonedDateTime now = ZonedDateTime.now();
            for (String discord : uncheckedUsers.keySet()) {
                if (now.isAfter(uncheckedUsers.get(discord).plusDays(uncheckedKickDelay))) {
                    kickUser(discord, secrets.mainGuildID);
                }
            }

            // Check top player roles
            if (!secrets.WITH_DB) return;
            BiMap<String, String> links;
            try {
                links = DBProvider.getManualLinks();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Could not retrieve manual links:");
                e.printStackTrace();
                return;
            }
            List<String> topPlayers;
            try {
                topPlayers = Main.customOsu.getRankings("be").subList(0, 10);
            } catch (IOException e) {
                logger.error("Could not retrieve rankings:");
                e.printStackTrace();
                return;
            }
            Guild mainGuild = Main.jda.getGuildById(secrets.mainGuildID);
            Role topRole = mainGuild.getRolesByName("Top", false).get(0);
            List<Member> topMembers = mainGuild.getMembers().stream()
                    .filter(m -> m.getRoles().contains(topRole))
                    .collect(Collectors.toList());
            GuildController controller = mainGuild.getController();
            for (Member member : topMembers) {
                if (!topPlayers.contains(links.get(member.getUser().getId()))) {
                    controller.removeSingleRoleFromMember(member, topRole).queue();
                    logger.info(member.getEffectiveName() + "(" + links.get(member.getUser().getId()
                            + ") not in  top 10, removed top role"));
                }
            }
            for (String player : topPlayers) {
                Member member = mainGuild.getMemberById(links.inverse().get(player));
                if (member != null && !topMembers.contains(member)) {
                    controller.addSingleRoleToMember(member, topRole).queue();
                    logger.info(member.getEffectiveName() + "(" + links.get(member.getUser().getId()
                            + ") is in  top 10, added top role"));
                }
            }
        });
        t.start();
    }
}
