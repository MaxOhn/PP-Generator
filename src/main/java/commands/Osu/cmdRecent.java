package main.java.commands.Osu;

import de.maxikg.osuapi.model.*;
import main.java.commands.INumberedICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class cmdRecent implements INumberedICommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (number > 50) {
            event.getTextChannel().sendMessage("The number must be between 1 and 50").queue();
            return;
        }

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

        ArrayList<UserGame> userRecents;
        UserGame recent;
        User user;
        try {
            userRecents = new ArrayList<>(Main.osu.getUserRecentByUsername(name).mode(getMode()).limit(50).query());
            recent = userRecents.get(0);
            while (--number > 0 && userRecents.size() > 1) {
                userRecents.remove(0);
                recent = userRecents.get(0);
            }
            if (number > 0) {
                event.getTextChannel().sendMessage("User's recent history doesn't go that far back").queue();
                return;
            }
            user = Main.osu.getUserByUsername(name).mode(getMode()).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found or no recent plays").queue();
            return;
        }
        Beatmap map;
        try {
            map = DBProvider.getBeatmap(recent.getBeatmapId());
        } catch (SQLException | ClassNotFoundException e) {
            map = Main.osu.getBeatmaps().beatmapId(recent.getBeatmapId()).mode(getMode()).limit(1).query().iterator().next();
            try {
                DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        Collection<UserScore> topPlays = Main.osu.getUserBestByUsername(name).mode(getMode()).limit(50).query();
        Collection<BeatmapScore> globalPlays = Main.osu.getScores(map.getBeatmapId()).mode(getMode()).query();
        new BotMessage(event, BotMessage.MessageType.RECENT)
                .user(user)
                .map(map)
                .usergame(recent)
                .history(userRecents)
                .mode(getMode())
                .topplays(topPlays, globalPlays)
                .buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
              return "Enter `" + statics.prefix + getName() + " [osu name]` to make me respond with info about the players last play."
                      + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }

    @Override
    public cmdRecent setNumber(int number) {
        this.number = number;
        return this;
    }

    GameMode getMode() {
        return GameMode.STANDARD;
    }

    String getName() {
        return "recent";
    }
}
