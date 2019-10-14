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
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/*
    Display a score of a users top score list that was recently made
 */
public class cmdRecentBest implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        number = 1;
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (number > 100) {
            event.getChannel().sendMessage("The number must be between 1 and 100").queue();
            return;
        }
        // Check for mode in arguments
        GameMode mode = GameMode.STANDARD;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m") || args[i].equals("-mode")) {
                if (i+1 < args.length) {
                    switch (args[i+1]) {
                        case "standard":
                        case "std":
                        case "s": mode = GameMode.STANDARD; break;
                        case "tko":
                        case "t": mode = GameMode.TAIKO; break;
                        case "ctb":
                        case "c":
                            event.getChannel().sendMessage(help(5)).queue();
                            return;
                        case "mania":
                        case "mna":
                        case "m": mode = GameMode.MANIA; break;
                        default:
                            event.getChannel().sendMessage(help(4)).queue();
                            return;
                    }
                } else {
                    event.getChannel().sendMessage(help(4)).queue();
                    return;
                }
            }
        }
        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        int delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }
        // Get name either from arguments or from database link
        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
        } else if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getChannel().sendMessage(help(0)).queue();
            return;
        } else {
            name = String.join(" ", argList);
        }
        // Check if name is given as mention
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                event.getChannel().sendMessage("The mentioned user is not linked, I don't know who you mean").queue();
                return;
            }
        }
        // Retrieve osu user data
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(mode).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        // Retrieve user's top scores
        Collection<OsuScore> topPlays;
        try {
            topPlays = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        // Sort scores by date
        ArrayList<OsuScore> topPlaysByDate = new ArrayList<>(topPlays);
        topPlaysByDate.sort(Comparator.comparing(OsuScore::getDate).reversed());
        final Iterator<OsuScore> itr = topPlaysByDate.iterator();
        OsuScore rbScore = itr.next();
        // Get the appropriate score
        while(itr.hasNext() && --number > 0)
            rbScore = itr.next();
        // Retrieve the score's map
        OsuBeatmap map;
        try {
            if (!secrets.WITH_DB)
                throw new SQLException();
            map = DBProvider.getBeatmap(rbScore.getBeatmapID());
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = rbScore.getBeatmap().get();
            } catch (OsuAPIException e1) {
                event.getChannel().sendMessage("Could not retrieve map").queue();
                return;
            }
            try {
                if (secrets.WITH_DB)
                    DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        // Retrieve the global leaderboard of the map
        Collection<OsuScore> globalPlays;
        try {
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).build());
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve global scores").queue();
            return;
        }
        // Construct the message
        new BotMessage(event.getChannel(), BotMessage.MessageType.RECENTBEST).user(user).map(map).osuscore(rbScore).mode(mode)
                .topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "recentbest -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "recentbest[number] [-m <s/t/c/m for mode>] [osu name]` "
                        + "to make me respond with the users selected best recent performance."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + "rb8`, I will skip the most recent 7 top scores "
                        + "and show the 8-th score, defaults to 1."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
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
