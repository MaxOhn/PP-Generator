package main.java.commands.Osu;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_strToInt;

/*
    Compare the topscores of multiple players and show which maps appear in each top score list
 */
public class cmdCommonScores extends cmdModdedCommand implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length < 1 || args[0].equals("-h") || args[0].equals("-help")) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toList());
        List<String> names = new ArrayList<>();
        GameMode mode = GameMode.STANDARD;

        // Get the names inbetween quotes
        if (argList.stream().anyMatch(w -> w.contains("\""))) {
            String argString = String.join(" ", args);
            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(argString);
            while (m.find()) {
                names.add(m.group(1));
                argString = argString.replace("\"" + m.group(1) + "\"", "");
            }
            argList = Arrays.stream(argString.split(" "))
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toList());
        }

        // Get the mode by checking for -m or -mode
        for (int i = 0; i < argList.size(); i++) {
            if (argList.get(i).equals("-m") || args[i].equals("-mode")) {
                if (i+1 < args.length) {
                    switch (argList.get(i+1)) {
                        case "standard":
                        case "std":
                        case "s": mode = GameMode.STANDARD; break;
                        case "tko":
                        case "t": mode = GameMode.TAIKO; break;
                        case "ctb":
                        case "c":
                            event.getChannel().sendMessage(help(2)).queue();
                            return;
                        case "mania":
                        case "mna":
                        case "m": mode = GameMode.MANIA; break;
                        default:
                            event.getChannel().sendMessage(help(3)).queue();
                            return;
                    }
                } else {
                    event.getChannel().sendMessage(help(3)).queue();
                    return;
                }
            }
        }
        int delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }

        // Get the mods by checking for +...
        Pattern p = Pattern.compile("\\+[^!]*!?");
        setInitial();
        int mIdx = -1;
        for (String s : argList) {
            if (p.matcher(s).matches()) {
                mIdx = argList.indexOf(s);
                break;
            }
        }
        if (mIdx != -1) {
            String word = argList.get(mIdx);
            if (word.contains("!")) {
                status = cmdModdedCommand.modStatus.EXACT;
                word = word.substring(1, word.length() - 1);
            } else {
                status = word.equals("+nm") ? modStatus.EXACT : cmdModdedCommand.modStatus.CONTAINS;
                word = word.substring(1);
            }
            includedMods = GameMod.get(mods_strToInt(word.toUpperCase()));
            argList.remove(mIdx);
        }
        p = Pattern.compile("-[^!]*!");
        mIdx = -1;
        for (String s : argList) {
            if (p.matcher(s).matches()) {
                mIdx = argList.indexOf(s);
                break;
            }
        }
        if (mIdx != -1) {
            String word = argList.get(mIdx);
            word = word.substring(1, word.length()-1);
            excludedMods.addAll(Arrays.asList(GameMod.get(mods_strToInt(word.toUpperCase()))));
            if (word.contains("nm"))
                excludeNM = true;
            argList.remove(mIdx);
        }

        // If names not yet found, get them as single words now
        while (!argList.isEmpty()) {
            names.add(argList.get(0));
            argList.remove(0);
        }
        if (names.size() == 0) {
            event.getChannel().sendMessage(help(4)).queue();
            return;
        } else if (names.size() == 1) {
            String n = Main.discLink.getOsu(event.getAuthor().getId());
            if (n == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
            names.add(n);
        }
        int compareAmount = names.size();

        // Retrieve users
        List<OsuUser> users = new ArrayList<>();
        try {
            for (String name : names)
                users.add(Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(mode).build()));
        } catch (Exception e) {
            event.getChannel().sendMessage("Could not find at least one of the players (`"
                    + String.join("` / `", names) + "`)").queue();
            return;
        }

        // Retrieve top scores
        List<Collection<OsuScore>> scores = new ArrayList<>();
        try {
            for (OsuUser user : users)
                scores.add(user.getTopScores(100).get());
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores of at least "
                    + "one of the players (`" + String.join("` / `", names) + "`)").queue();
            return;
        }

        // Combine common scores
        List<OsuScore> common = scores.stream().flatMap(Collection::stream)
                .collect(Collectors.groupingBy(OsuScore::getBeatmapID))
                .values()
                .stream()
                .filter(list -> list.size() >= compareAmount && list.stream().allMatch(this::hasValidMods))
                .sorted((a, b) -> Math.round(a.get(0).getPp() - b.get(0).getPp()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Retrieve maps of common scores
        ArrayList<OsuBeatmap> maps = new ArrayList<>();
        for (int i = 0; i < Math.min(common.size(), 15*compareAmount); i += compareAmount) {
            OsuBeatmap map;
            int mapID = common.get(i).getBeatmapID();
            try {
                if (!secrets.WITH_DB)
                    throw new SQLException();
                map = DBProvider.getBeatmap(mapID);
            } catch (SQLException | ClassNotFoundException e) {
                try {
                    map = common.get(i).getBeatmap().get();
                } catch (OsuAPIException e1) {
                    event.getChannel().sendMessage("Could not retrieve beatmap with id `" + mapID + "`").queue();
                    return;
                }
                try {
                    if (secrets.WITH_DB)
                        DBProvider.addBeatmap(map);
                } catch (ClassNotFoundException | SQLException e1) {
                    e1.printStackTrace();
                }
            }
            maps.add(map);
        }

        new BotMessage(event.getChannel(), BotMessage.MessageType.COMMONSCORES).maps(maps).mode(mode).osuscores(common)
                .users(users).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "common -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "common <osu name 1> [osu name 2 [ osu name 3 ...]] [-m <s/t/c/m for mode>] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!]` to make "
                        + "me list the maps appearing in all given player's top 100 scores.\nIf you're not linked via `"
                        + statics.prefix + "link <osu name>" + "`, you must specify at least two names, otherwise I compare "
                        + "your linked account with the specified name.\n**User names that contain spaces must be "
                        + "encapsulated with \"** e.g. \"nathan on osu\""
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods.";
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
