package main.java.commands;

import de.maxikg.osuapi.model.*;
import main.java.core.Main;
import main.java.util.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.*;

public class cmdRecentMania implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String name;
        if (args.length == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return;
            }
        } else if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        } else
            name = String.join(" ", args);
        Collection<UserGame> userRecents;
        UserGame recent;
        User user;
        try {
            user = Main.osu.getUserByUsername(name).mode(GameMode.OSU_MANIA).query().iterator().next();
            userRecents = Main.osu.getUserRecentByUsername(name).limit(50).mode(GameMode.OSU_MANIA).query();
            recent = userRecents.iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found or no recent mania plays").queue();
            return;
        }
        Beatmap map = Main.osu.getBeatmaps().beatmapId(recent.getBeatmapId()).mode(GameMode.OSU_MANIA).limit(1).query().iterator().next();
        Collection<UserScore> topPlays = Main.osu.getUserBestByUsername(name).mode(GameMode.OSU_MANIA).limit(50).query();
        Collection<BeatmapScore> globalPlays = Main.osu.getScores(map.getBeatmapId()).mode(GameMode.OSU_MANIA).query();
        if (!recent.getRank().equals("F")) {
            Collection<BeatmapScore> scores = Main.osu.getScores(map.getBeatmapId()).username(name).mode(GameMode.OSU_MANIA).query();
            boolean foundScore = false;
            for (BeatmapScore s: scores) {
                if (utilOsu.beatmapScoreIsUserGame(s, recent)) {
                    scoreEmbed.embedScoreRecentMania(event, user, map, recent, userRecents, topPlays, globalPlays, s);
                    foundScore = true;
                    break;
                }
            }
            if (!foundScore)
                scoreEmbed.embedScoreRecentMania(event, user, map, recent, userRecents, topPlays, globalPlays);
        } else
            scoreEmbed.embedScoreRecentMania(event, user, map, recent, userRecents, topPlays, globalPlays);
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "recentmania -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "recentmania [osu name]` to make me respond with info about the players last mania play."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }
}
