package main.java.commands;

import de.maxikg.osuapi.model.*;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Collection;

public class cmdRecentTaiko implements Command {
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
            user = Main.osu.getUserByUsername(name).mode(GameMode.TAIKO).query().iterator().next();
            userRecents = Main.osu.getUserRecentByUsername(name).limit(50).mode(GameMode.TAIKO).query();
            recent = userRecents.iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found or no recent taiko plays").queue();
            return;
        }
        Beatmap map = Main.osu.getBeatmaps().beatmapId(recent.getBeatmapId()).mode(GameMode.TAIKO).limit(1).query().iterator().next();
        Collection<UserScore> topPlays = Main.osu.getUserBestByUsername(name).mode(GameMode.TAIKO).limit(50).query();
        Collection<BeatmapScore> globalPlays = Main.osu.getScores(map.getBeatmapId()).mode(GameMode.TAIKO).query();
        new BotMessage(event, BotMessage.MessageType.RECENT).user(user).map(map).usergame(recent).history(userRecents)
                .mode(GameMode.TAIKO).topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "recenttaiko -h` for more help)";
        switch (hCode) {
            case 0:
                return "Enter `" + statics.prefix + "recenttaiko [osu name]` to make me respond with info about the players last taiko play."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }
}