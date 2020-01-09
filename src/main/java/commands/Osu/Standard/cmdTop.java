package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.commands.Osu.cmdModdedCommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import main.java.util.utilOsu;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_strToInt;

/*
    Display top scores of a user that satisfy conditions
 */
public class cmdTop extends cmdModdedCommand implements INumberedCommand {

    private int number = 1;
    private double acc;
    private int combo;
    private String grade;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        setInitial();
        number = 1;
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (number > 100) {
            event.getChannel().sendMessage("The number must be between 1 and 100").queue();
            return;
        }
        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        // Check for accuracy in arguments
        int delIndex = argList.indexOf("-acc");
        if (delIndex > -1) {
            try {
                acc = Double.parseDouble(argList.get(delIndex + 1));
                if (acc < 0) throw new IllegalArgumentException("Accuracy must be positive");
            } catch (Exception e) {
                event.getChannel().sendMessage(help(4)).queue();
                return;
            }
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        } else acc = 0;
        // Check for combo in arguments
        delIndex = argList.indexOf("-combo");
        if (delIndex > -1) {
            try {
                combo = Integer.parseInt(argList.get(delIndex + 1));
                if (combo < 0) throw new IllegalArgumentException("Combo must be positive");
            } catch (Exception e) {
                event.getChannel().sendMessage(help(5)).queue();
                return;
            }
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        } else combo = 0;
        // Check for grade in arguments
        delIndex = argList.indexOf("-grade");
        if (delIndex > -1) {
            if (argList.size() < delIndex + 2) {
                event.getChannel().sendMessage(help(6)).queue();
                return;
            }
            switch (argList.get(delIndex + 1).toLowerCase()) {
                case "ss":
                case "s":
                case "a":
                case "b":
                case "c":
                case "d":
                    grade = argList.get(delIndex + 1).toLowerCase();
                    break;
                default:
                    event.getChannel().sendMessage(help(6)).queue();
                    return;
            }
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        } else grade = "";
        // Check for "last" option in arguments
        boolean last = false;
        delIndex = argList.indexOf("-last");
        if (delIndex > -1) {
            last = true;
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
            word = word.substring(1, word.length() - 1);
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
        if (event.isFromType(ChannelType.TEXT) && event.getMessage().getMentionedMembers().size() > 0) {
            name = Main.discLink.getOsu(event.getMessage().getMentionedMembers().get(0).getUser().getId());
            if (name == null) {
                event.getChannel().sendMessage("The mentioned user is not linked, I don't know who you mean").queue();
                return;
            }
        }
        // Retrieve osu user data
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("User `" + name + "` was not found").queue();
            return;
        }
        // Retrieve user's top scores
        List<OsuScore> scores;
        try {
            scores = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        List<OsuScore> actual = new ArrayList<>(scores);
        if (last) {
            Collections.reverse(scores);
        }
        if (number < 6) {
            // Consider only scores with correct mods that fullfill the score condition
            scores = scores.stream()
                    .filter(s -> getScoreCondition(s, user.getMode()))
                    .collect(Collectors.toList());
            List<OsuBeatmap> maps = new ArrayList<>();
            // If there is a condition on maps, take all scores for which the map satisfies the condition
            if (!getMapCondition(null)) {
                List<OsuBeatmap> allMaps;
                try {
                    allMaps = utilOsu.getBeatmaps(scores);
                } catch (SQLException | OsuAPIException | ClassNotFoundException e) {
                    event.getChannel().sendMessage("Something went wrong, blame bade").queue();
                    logger.error("Error while retrieving maps in bulk: ", e);
                    return;
                }
                for (OsuBeatmap map : allMaps)
                    if (getMapCondition(map))
                        maps.add(map);
                scores = scores.stream()
                        .filter(s -> maps.stream().anyMatch(m -> m.getID() == s.getBeatmapID()))
                        .collect(Collectors.toList());
            // No condition on maps -> just take the maps of the first 5 scores
            } else {
                try {
                    maps.addAll(utilOsu.getBeatmaps(scores.stream().limit(5).collect(Collectors.toList())));
                } catch (SQLException | OsuAPIException | ClassNotFoundException e) {
                    event.getChannel().sendMessage("Something went wrong, blame bade").queue();
                    logger.error("Error while retrieving maps in bulk: ", e);
                    return;
                }
            }
            if (scores.size() == 0) {
                event.getChannel().sendMessage(noScoreMessage(user.getUsername(), status != modStatus.WITHOUT || excludedMods.size() > 0 || excludeNM)).queue();
                return;
            }
            LinkedList<Integer> indices = new LinkedList<>();
            for (OsuScore s : scores) {
                indices.addLast(actual.indexOf(s) + 1);
            }
            // Build message
            new BotMessage(event.getChannel(), getMessageType()).user(user).osuscores(scores)
                    .maps(maps.stream().limit(5).collect(Collectors.toList())).indices(indices)
                    .mode(getMode()).buildAndSend();
        } else {
            // Get the appropriate score
            OsuScore topScore = null;
            OsuBeatmap map = null;
            for (OsuScore s : scores) {
                if (getScoreCondition(s, user.getMode())) {
                    if (!getMapCondition(null)) {
                        try {
                            map = utilOsu.getBeatmap(s.getBeatmapID());
                        } catch (OsuAPIException e) {
                            event.getChannel().sendMessage("Some osu! API issue, blame bade").queue();
                            return;
                        }
                        if (getMapCondition(map) && --number == 0) {
                            topScore = s;
                            break;
                        }
                    } else if (--number == 0) {
                        topScore = s;
                        break;
                    }
                }
            }
            if (topScore == null) {
                event.getChannel().sendMessage("No top score found with the specified poperties.").queue();
                return;
            }
            // Retrieve the score's map if it didn't happen already
            if (map == null) {
                try {
                    map = utilOsu.getBeatmap(topScore.getBeatmapID());
                } catch (OsuAPIException e) {
                    event.getChannel().sendMessage("Some osu! API issue, blame bade").queue();
                    return;
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
            // Create the message
            new BotMessage(event.getChannel(), BotMessage.MessageType.SINGLETOP).user(user).map(map).osuscore(topScore)
                    .mode(getMode()).topplays(actual, globalPlays).buildAndSend();
        }
    }

    String noScoreMessage(String username, boolean withMods) {
        return "Could not find any top scores from user `" + username + "`" + (withMods || acc > 0 || !grade.isEmpty() || combo > 0 ? " with the given specifications" : "");
    }

    boolean getMapCondition(OsuBeatmap m) {
        return true;
    }

    boolean getScoreCondition(OsuScore s, GameMode m) {
        if (s == null) return true;
        return utilOsu.getAcc(s, m) >= acc
                && s.getMaxCombo() >= combo
                && (grade.isEmpty()
                    || s.getRank().replace("H", "").replace("X", "ss").toLowerCase().equals(grade))
                && hasValidMods(s);
    }

    BotMessage.MessageType getMessageType() {
        return BotMessage.MessageType.TOPSCORES;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "top" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "top" + getName() + "[number] [osu name] [-acc <number>] [-grade <SS/A/D/...>] [-combo <number>] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!] [-last]` to make me list the user's top scores with the given properties."
                        + "\nIf no number is specified or it's up to 5, I will show the top 5 scores. Otherwise I will show only the number-th top score."
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods."
                        + "\nWith `-acc` you can specify a bottom limit for counted accuracies. Must be a positive decimal number."
                        + "\nWith `-combo` you can specify a bottom limit for counted combos. Must be a positive integer."
                        + "\nWith `-grade` you can specify what grade counted scores will have. Must be either SS, S, A, B, C, or D."
                        + "\nWith `-last`, I will start enumerating the scores from last to first instead of first to last."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            case 4:
                return "After '-acc' you must specify a positive number i.e. `-acc 96.73`" + help;
            case 5:
                return "After '-combo' you must specify a positive integer i.e. `-combo 567`" + help;
            case 6:
                return "After '-grade' you must specify a either SS, S, A, B, C, or D i.e. `-grade B`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }

    @Override
    public INumberedCommand setNumber(int number) {
        this.number = number;
        return this;
    }

    public String getName() {
        return "";
    }

    public GameMode getMode() {
        return GameMode.STANDARD;
    }
}
