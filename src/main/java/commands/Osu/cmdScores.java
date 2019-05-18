package main.java.commands.Osu;

import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class cmdScores implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length < 1 || args[0].equals("-h") || args[0].equals("-help")) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        Pattern p = Pattern.compile("((https:\\/\\/osu\\.ppy\\.sh\\/b\\/)([0-9]{1,8})(.*))|((https:\\/\\/osu\\.ppy\\.sh\\/beatmapsets\\/[0-9]*\\#[a-z]*\\/)([0-9]{1,8})(.*))");
        String mapID = "-1";
        try {
            Matcher m = p.matcher(args[0]);
            if (m.find()) {
                mapID = m.group(3);
                if (mapID == null) mapID = m.group(7);
            }
            if (mapID.equals("-1")) mapID = Integer.parseInt(args[0]) + "";
        } catch (Exception e) {
            if (args[0].contains("/s/") || !args[0].contains("#"))
            new BotMessage(event, BotMessage.MessageType.TEXT).send("I think you specified a mapset, try a specific beatmap instead");
            else
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(2));
            return;
        }
        if (mapID.equals("-1")) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve map id from the command. Have you specified the map id and not only the map set id?");
            return;
        }
        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(LinkedList::new));
        String name = argList.size() > 1
                ? String.join(" ", argList.subList(1, argList.size()))
                : Main.discLink.getOsu(event.getAuthor().getId());
        if (name == null) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
            return;
        }
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("The mentioned user is not linked, I don't know who you mean");
                return;
            }
        }
        OsuBeatmap map;
        try {
            map = DBProvider.getBeatmap(Integer.parseInt(mapID));
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = Main.osu.beatmaps.query(
                        new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(Integer.parseInt(mapID)).build()
                ).get(0);
            } catch (OsuAPIException e1) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve beatmap");
                return;
            }
            try {
                DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(map.getMode()).build());
        } catch (Exception e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find osu user `" + name + "`");
            return;
        }
        Collection<OsuScore> scores = null;
        try {
            scores = Main.osu.scores.query(
                    new EndpointScores.ArgumentsBuilder(Integer.parseInt(mapID)).setUserName(name).setMode(map.getMode()).build()
            );
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve scores");
            return;
        }
        if (scores.size() == 0) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find any scores of `" + name
                    + "` on beatmap id `" + mapID + "`");
            return;
        }
        new BotMessage(event, BotMessage.MessageType.SCORES).user(user).map(map).osuscores(scores).mode(map.getMode()).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "scores -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "scores <beatmap url or beatmap id> [osu name]` to make me show the user's "
                        + "top scores for each mod combination of the specified map.\nBeatmap urls from both the new " +
                        "and old website are supported.\nIf no player name is specified, your discord must be linked to " +
                        "an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name as second argument or link your discord to an osu profile via `" +
                        statics.prefix + "link <osu name>" + "`" + help;
            case 2:
                return "The first argument must either be the link to a beatmap e.g. `https://osu.ppy.sh/b/1613091&m=0`, or just the id of the beatmap" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
