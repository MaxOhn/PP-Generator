package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_flag;

public class cmdMapLeaderboard implements ICommand {
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
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
            return;
        }
        if (mapID.equals("-1")) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve map id from the command. Have you specified the map id and not only the map set id?");
            return;
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

        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(LinkedList::new));
        boolean withMods = false;
        GameMod[] mods = new GameMod[] {};
        p = Pattern.compile("\\+.*");
        int mIdx = -1;
        for (String s : argList)
            if (p.matcher(s).matches())
                mIdx = argList.indexOf(s);
        if (mIdx != -1) {
            mods = GameMod.get(mods_flag(argList.get(mIdx).substring(1).toUpperCase()));
            argList.remove(mIdx);
            withMods = true;
        }

        Collection<OsuScore> scores;
        try {
            boolean finalWithMods = withMods;
            GameMod[] finalMods = mods;
            scores = Main.customOsu.getScores(mapID).stream().limit(withMods ? 100 : 10)
                    .filter(s -> !finalWithMods || Arrays.equals(s.getEnabledMods(), finalMods))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve scores of the beatmap, blame bade");
            e.printStackTrace();
            return;
        }
        new BotMessage(event, BotMessage.MessageType.LEADERBOARD).map(map).osuscores(scores).mode(map.getMode()).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "leaderboard -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "leaderboard <beatmap url or beatmap id>` to make me show the beatmap's "
                        + " national top 5 scores.\nBeatmap urls from both the new " +
                        "and old website are supported.";
            case 1:
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
