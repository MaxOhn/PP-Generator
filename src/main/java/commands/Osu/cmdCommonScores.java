package main.java.commands.Osu;

import com.oopsjpeg.osu4j.*;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_flag;

public class cmdCommonScores implements ICommand {
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
        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        String name1 = "";
        String name2 = "";
        GameMode mode = GameMode.STANDARD;

        // Get the names inbetween quotes
        if (argList.stream().anyMatch(w -> w.contains("\""))) {
            String argString = String.join(" ", args);
            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(argString);
            while (m.find()) {
                if (name1.equals("")) name1 = m.group(1);
                else if (name2.equals("")) name2 = m.group(1);
                else {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send(help(5));
                    return;
                }
                argString = argString.replace("\"" + m.group(1) + "\"", "");
            }
            argList = Arrays.stream(argString.split(" "))
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // Get the mode by checking for -m or -mode
        for (int i = 0; i < argList.size(); i++) {
            if (argList.get(i).equals("-m") || args[i].equals("-mode")) {
                if (i+1 < args.length) {
                    switch (argList.get(i+1)) {
                        case "s": mode = GameMode.STANDARD; break;
                        case "t": mode = GameMode.TAIKO; break;
                        case "c":
                            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(2));
                            return;
                        case "m": mode = GameMode.MANIA; break;
                        default:
                            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(3));
                            return;
                    }
                } else {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send(help(3));
                    return;
                }
            }
        }
        int delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }

        // If names not yet found, get them as single words now
        if (name1.equals("") && argList.isEmpty()) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(4));
            return;
        } else if (name1.equals("")) {
            name1 = argList.get(0);
            argList.remove(0);
        }
        if (name2.equals("") && argList.isEmpty()) {
            name2 = Main.discLink.getOsu(event.getAuthor().getId());
            if (name2 == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
                return;
            }
        } else if (name2.equals("")) {
            name2 = argList.get(0);
        }

        // Get the mods by checking for +...
        boolean withMods = false;
        GameMod[] mods = new GameMod[] {};
        Pattern p = Pattern.compile("\\+.*");
        int mIdx = -1;
        for (String s : argList)
            if (p.matcher(s).matches())
                mIdx = argList.indexOf(s);
        if (mIdx != -1) {
            mods = GameMod.get(mods_flag(argList.get(mIdx).substring(1).toUpperCase()));
            argList.remove(mIdx);
            withMods = true;
        }

        // Retrieve users
        OsuUser user1;
        OsuUser user2;
        try {
            user1 = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name1).setMode(mode).build());
            user2 = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name2).setMode(mode).build());
        } catch (Exception e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find at least one of the players (`"
                    + name1 + "` / `" + name2 + "`)");
            return;
        }

        // Retrieve top scores
        Collection<OsuScore> scores1;
        Collection<OsuScore> scores2;
        try {
            scores1 = user1.getTopScores(100).get();
            scores2 = user2.getTopScores(100).get();
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve top scores of at least "
                    + "one of the players (`" + name1 + "` / `" + name2 + "`)");
            return;
        }

        // Combine common scores
        boolean finalWithMods = withMods;
        GameMod[] finalMods = mods;
        //*
        List<OsuScore> common = scores1.stream()

                // Keep only those in scores1 for which the same beatmapid appears in scores2
                .filter(s1 -> (!finalWithMods || Arrays.equals(s1.getEnabledMods(), finalMods)) &&
                        scores2.stream().anyMatch(s2 -> s1.getBeatmapID() == s2.getBeatmapID() &&
                                (!finalWithMods || Arrays.equals(s2.getEnabledMods(), finalMods))))

                // Map each score of scores1 to an array containing the scores1 score and the corresponding scores2 score
                .map(s1 -> new OsuScore[] {s1, scores2.stream().filter(s2 -> s1.getBeatmapID() == s2.getBeatmapID() &&
                        (!finalWithMods || Arrays.equals(s2.getEnabledMods(), finalMods))).findFirst().get()})

                // Flatten everything into a singly stream again and collect into a list
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        //*/

        // Retrieve maps of common scores
        ArrayList<OsuBeatmap> maps = new ArrayList<>();
        for (int i = 0; i < Math.min(common.size(), 30); i += 2) {
            OsuBeatmap map;
            int mapID = common.get(i).getBeatmapID();
            try {
                map = DBProvider.getBeatmap(mapID);
            } catch (SQLException | ClassNotFoundException e) {
                try {
                    map = common.get(i).getBeatmap().get();
                } catch (OsuAPIException e1) {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve beatmap with id `" + mapID + "`");
                    return;
                }
                try {
                    DBProvider.addBeatmap(map);
                } catch (ClassNotFoundException | SQLException e1) {
                    e1.printStackTrace();
                }
            }
            maps.add(map);
        }

        new BotMessage(event, BotMessage.MessageType.COMMONSCORES).maps(maps).mode(mode).osuscores(common)
                .users(Arrays.asList(user1, user2)).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "common -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "common <osu name 1> <osu name 2> [-m <s/t/c/m for mode>]` to make "
                        + "me list the maps appearing in both player's top 100 scores.\nIf you're linked via `"
                        + statics.prefix + "link <osu name>" + "`, you can also use the command through "
                        + "`" + statics.prefix + "common [-m <s/t/c/m for mode>] <osu name>` to make me compare your"
                        + " linked account with the specified name.\n**User names that contain spaces must be "
                        + "encapsulated with \"** e.g. \"nathan on osu\n";
            case 1:
                return "Either specify an second user name or link your discord to an osu profile via `" + statics.prefix
                        + "link <osu name>" + "` so that I compare you account with the specified name" + help;
            case 2:
                return "CtB is not yet supported" + help;
            case 3:
                return "After '-m' specify either 's' for standard, 't' for taiko, 'c' for CtB, or 'm' for mania" + help;
            case 4:
                return "You must specify at least one user name" + help;
            case 5:
                return "Could not parse message. Have you specified between one and two usernames?" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
