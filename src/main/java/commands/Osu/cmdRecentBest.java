package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.*;

public class cmdRecentBest implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (number > 100) {
            event.getTextChannel().sendMessage("The number must be between 1 and 100").queue();
            return;
        }

        GameMode mode = GameMode.STANDARD;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m") || args[i].equals("-mode")) {
                if (i+1 < args.length) {
                    switch (args[i+1]) {
                        case "s": mode = GameMode.STANDARD; break;
                        case "t": mode = GameMode.TAIKO; break;
                        case "c":
                            event.getTextChannel().sendMessage(help(5)).queue();
                            return;
                        case "m": mode = GameMode.MANIA; break;
                        default:
                            event.getTextChannel().sendMessage(help(4)).queue();
                            return;
                    }
                } else {
                    event.getTextChannel().sendMessage(help(4)).queue();
                    return;
                }
            }
        }
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(args));
        int delIndex = Math.max(argList.indexOf("-n"), argList.indexOf("-number"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }
        delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }
        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return;
            }
        } else if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        } else {
            name = String.join(" ", argList);
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(mode).build());
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        Collection<OsuScore> topPlays = null;
        try {
            topPlays = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        ArrayList<OsuScore> topPlaysByDate = new ArrayList<>(topPlays);
        topPlaysByDate.sort(Comparator.comparing(OsuScore::getDate).reversed());
        final Iterator<OsuScore> itr = topPlaysByDate.iterator();
        OsuScore rbScore = itr.next();
        while(itr.hasNext() && --number > 0)
            rbScore = itr.next();
        OsuBeatmap map;
        try {
            map = DBProvider.getBeatmap(rbScore.getBeatmapID());
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = rbScore.getBeatmap().get();
            } catch (OsuAPIException e1) {
                event.getTextChannel().sendMessage("Could not retrieve map").queue();
                return;
            }
            try {
                DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        Collection<OsuScore> globalPlays;
        try {
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).build());
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve global scores").queue();
            return;
        }
        new BotMessage(event, BotMessage.MessageType.RECENTBEST).user(user).map(map).osuscore(rbScore).mode(mode)
                .topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "recentbest -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "recentbest [-n <number 1-100>] [-m <s/t/c/m for mode>] [osu name]` to make me respond with the users selected best recent performance."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            case 3:
                return "Specify a number after '-n'" + help;
            case 4:
                return "After '-m' specify either 's' for standard, 't' for taiko, 'c' for CtB, or 'm' for mania" + help;
            case 5:
                return "CtB is not yet supported" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }

    @Override
    public cmdRecentBest setNumber(int number) {
        this.number = number;
        return this;
    }
}
