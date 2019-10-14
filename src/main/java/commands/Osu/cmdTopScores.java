package main.java.commands.Osu;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.FileInteractor;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_strToInt;

/*
    Display top scores of a user that satisfy conditions
 */
public class cmdTopScores extends cmdModdedCommand implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        setInitial();
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        GameMode mode = GameMode.STANDARD;
        // Check for a mode in arguments
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
        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        int delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }
        // Check for mods in arguments
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
        // Get the name either from arguments or from database link
        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
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
        List<OsuScore> scores;
        try {
            scores = user.getTopScores(status != modStatus.WITHOUT || excludeNM || excludedMods.size() > 0 ? 100 : getAmount()).get();
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        ArrayList<OsuBeatmap> maps = new ArrayList<>();
        for (OsuScore score : scores) {
            // Score must have the appropriate mods
            if (!isValidScore(score))
                continue;
            // Retrieve the score's map
            OsuBeatmap map;
            try {
                if (!secrets.WITH_DB)
                    throw new SQLException();
                map = DBProvider.getBeatmap(score.getBeatmapID());
            } catch (SQLException | ClassNotFoundException e) {
                try {
                    map = score.getBeatmap().get();
                } catch (OsuAPIException e1) {
                    continue;
                }
                try {
                    if (secrets.WITH_DB)
                        DBProvider.addBeatmap(map);
                } catch (ClassNotFoundException | SQLException e1) {
                    e1.printStackTrace();
                }
            }
            // If both score and map condition are satisfied, add them
            if (getScoreCondition(score, mode) && getMapCondition(map)) {
                FileInteractor.prepareFiles(map);
                maps.add(map);
            }
        }
        // Check if filtering is necessary
        if (!getScoreCondition(null, null) || !getMapCondition(null)) {
            scores = scores.stream()
                    .filter(s -> maps.stream().anyMatch(m -> m.getID() == s.getBeatmapID()))
                    .collect(Collectors.toList());
        } else if (status != modStatus.WITHOUT || excludeNM || excludedMods.size() > 0) {
            scores = scores.stream()
                    .filter(this::isValidScore)
                    .collect(Collectors.toList());
        }
        if (scores.size() == 0) {
            event.getChannel().sendMessage(noScoreMessage(user.getUsername(), status != modStatus.WITHOUT || excludedMods.size() > 0 || excludeNM)).queue();
            return;
        }
        // Build message
        new BotMessage(event.getChannel(), getMessageType()).user(user).osuscores(scores)
                .maps(maps.stream().limit(5).collect(Collectors.toCollection(ArrayList::new)))
                .mode(mode)
                .buildAndSend();
    }

    String noScoreMessage(String username, boolean withMods) {
        return "Could not find any top scores from user `" + username + "`" + (withMods ? " with the specified mods" : "");
    }

    int getAmount() {
        return 5;
    }

    boolean getMapCondition(OsuBeatmap m) {
        return true;
    }

    boolean getScoreCondition(OsuScore s, GameMode m) {
        return true;
    }

    BotMessage.MessageType getMessageType() {
        return BotMessage.MessageType.TOPSCORES;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "topscores -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "topscores [-m <s/t/c/m for mode>] [osu name] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!]` to make me list the user's top 5 scores."
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            case 2:
                return "CtB is not yet supported" + help;
            case 3:
                return "After '-m' specify either 's' for standard, 't' for taiko, 'c' for CtB, or 'm' for mania" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
