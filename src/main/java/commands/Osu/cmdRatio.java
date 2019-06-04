package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class cmdRatio implements INumberedCommand {

    private int number = 100;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (number > 100) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("The number must be between 1 and 100");
            return;
        }

        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

        /*
        boolean weighted = argList.stream().anyMatch(arg -> arg.equals("-w") || arg.equals("-weighted"));
        int delIndex = Math.max(argList.indexOf("-w"), argList.indexOf("-weighted"));
        if (delIndex > -1)
            argList.remove(delIndex);
        //*/

        GameMode mode = GameMode.MANIA;

        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
                return;
            }
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
        List<OsuScore> scores;
        try {
            scores = user.getTopScores(number).get();
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve top scores");
            return;
        }

        if (scores.size() == 0) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find any scores of `" + name
                    + "` in " + mode.getName());
            return;
        }
        new BotMessage(event, BotMessage.MessageType.RATIO).user(user).mode(mode).osuscores(scores).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "ratio -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "ratio[number] [osu name]` to make me summarize "
                        + "the player's average Geki/300 ratios of the personal top [number] __mania__ scores."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + "ratio8`, I will take the average of the user's top 8 scores, defaults to 100."
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
    public cmdRatio setNumber(int number) {
        this.number = number;
        return this;
    }
}
