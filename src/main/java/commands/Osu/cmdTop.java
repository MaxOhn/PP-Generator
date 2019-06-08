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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

public class cmdTop implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (number > 100) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("The number must be between 1 and 100");
            return;
        }

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
                            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(5));
                            return;
                        case "mania":
                        case "mna":
                        case "m": mode = GameMode.MANIA; break;
                        default:
                            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(4));
                            return;
                    }
                } else {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send(help(4));
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
        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
                return;
            }
        } else if (args[0].equals("-h") || args[0].equals("-help")) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
            return;
        } else {
            name = String.join(" ", argList);
        }
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("The mentioned user is not linked, I don't know who you mean");
                return;
            }
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(mode).build());
        } catch (Exception e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("`" + name + "` was not found");
            return;
        }
        Collection<OsuScore> topPlays = null;
        try {
            topPlays = user.getTopScores(number).get();
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve top scores");
            return;
        }
        final Iterator<OsuScore> itr = topPlays.iterator();
        OsuScore rbScore = itr.next();
        while(itr.hasNext() && --number > 0)
            rbScore = itr.next();
        OsuBeatmap map;
        try {
            if (!secrets.WITH_DB)
                throw new SQLException();
            map = DBProvider.getBeatmap(rbScore.getBeatmapID());
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = rbScore.getBeatmap().get();
            } catch (OsuAPIException e1) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve map");
                return;
            }
            try {
                if (secrets.WITH_DB)
                    DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        Collection<OsuScore> globalPlays = null;
        try {
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).build());
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve global scores");
            return;
        }
        new BotMessage(event, BotMessage.MessageType.SINGLETOP).user(user).map(map).osuscore(rbScore)
                .mode(mode).topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "best -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "top[number] [-m <s/t/c/m for mode>] [osu name]` to make me respond with the users selected best performance."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + "top8`, I will give the user's 8th best score, defaults to 1."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify a osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
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
    public cmdTop setNumber(int number) {
        this.number = number;
        return this;
    }
}
