package main.java.core;

import com.google.common.collect.BiMap;
import com.oopsjpeg.osu4j.GameMode;
import main.java.util.secrets;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.GuildController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.DAYS;

/*
    - Automatically kicking unchecked users after some time
    - Updating the Top-role distributation regularly
 */
public class MemberHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
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

    // New user joined, put them in database
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

    // User lost Unchecked-role, remove from database
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

    // Kick a member of a server
    public void kickUser(String userID, String guildID) {
        // Wait till Main's jda is ready
        while (Main.jda == null);
        try {
            uncheckedUsers.remove(userID);
            // Try to remove user from database
            try {
                if (secrets.WITH_DB)
                    DBProvider.removeUncheckedUser(userID);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Could not remove kicked user from DB:");
                e1.printStackTrace();
            }
            // Kick member
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

    // Prepare the scheduler to execute tasks in regular time intervals
    private void runRegularChecks() {
        if (secrets.RELEASE) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            final Runnable kickerIterator = this::regularIteration;
            scheduler.scheduleAtFixedRate(kickerIterator, 0, 1, DAYS);
        }
    }

    private void regularIteration() {
        final Thread t = new Thread(() -> {

            // Kick unchecked members
            ZonedDateTime now = ZonedDateTime.now();
            new ArrayList<>(uncheckedUsers.keySet()).forEach(discord -> {
                if (now.isAfter(uncheckedUsers.get(discord).plusDays(uncheckedKickDelay)))
                    kickUser(discord, secrets.mainGuildID);
            });

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
            int TOP_OSU = 10;
            int TOP_TAIKO = 3;
            int TOP_CTB = 3;
            int TOP_MANIA = 5;
            HashSet<String> topPlayers = new HashSet<>();
            try {
                topPlayers.addAll(Main.customOsu.getRankings(GameMode.STANDARD, "be").subList(0, TOP_OSU));
                topPlayers.addAll(Main.customOsu.getRankings(GameMode.TAIKO, "be").subList(0, TOP_TAIKO));
                topPlayers.addAll(Main.customOsu.getRankings(GameMode.CATCH_THE_BEAT, "be").subList(0, TOP_CTB));
                topPlayers.addAll(Main.customOsu.getRankings(GameMode.MANIA, "be").subList(0, TOP_MANIA));
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
                    logger.info(member.getEffectiveName()
                            + (links.get(member.getUser().getId()) != null ? " (" + links.get(member.getUser().getId()) + ")" : "")
                            + " not in top 10, removed top role");
                }
            }
            for (String player : topPlayers) {
                String discordID = links.inverse().get(player);
                if (discordID == null) continue;
                Member member = mainGuild.getMemberById(discordID);
                if (member != null && !topMembers.contains(member)) {
                    controller.addSingleRoleToMember(member, topRole).queue();
                    logger.info(member.getEffectiveName() + " (" + links.get(member.getUser().getId())
                            + ") satisfies top condition, added top role");
                }
            }
        });
        t.start();
    }
}
