package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.*;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class cmdRecent implements INumberedCommand {

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

        ArrayList<OsuScore> userRecents;
        OsuScore recent;
        OsuUser user;
        try {
            userRecents = new ArrayList<>(Main.osu.userRecents.query(
                    new EndpointUserRecents.ArgumentsBuilder(name).setMode(getMode()).setLimit(50).build())
            );
            recent = userRecents.get(0);
            while (--number > 0 && userRecents.size() > 1) {
                userRecents.remove(0);
                recent = userRecents.get(0);
            }
            if (number > 0) {
                event.getTextChannel().sendMessage("User's recent history doesn't go that far back").queue();
                return;
            }
            user = recent.getUser().get();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found or no recent plays").queue();
            return;
        }
        OsuBeatmap map;
        try {
            map = DBProvider.getBeatmap(recent.getBeatmapID());
        } catch (SQLException | ClassNotFoundException e) {
            try {
                //map = recent.getBeatmap().get();
                map = Main.osu.beatmaps.query(
                        new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(recent.getBeatmapID()).setMode(getMode()).setLimit(1).build()
                ).get(0);
            } catch (OsuAPIException e1) {
                event.getTextChannel().sendMessage("Could not retrieve beatmap id `" + recent.getBeatmapID() + "`").queue();
                return;
            }
            try {
                DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        Collection<OsuScore> topPlays;
        Collection<OsuScore> globalPlays;
        try {
            topPlays = user.getTopScores(50).get();
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).setMode(getMode()).build());
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        new BotMessage(event, BotMessage.MessageType.RECENT)
                .user(user)
                .map(map)
                .osuscore(recent)
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
                      + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
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
