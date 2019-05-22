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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_flag;

public class cmdTopScores implements ICommand {
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
        GameMode mode = GameMode.STANDARD;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-m") || args[i].equals("-mode")) {
                if (i+1 < args.length) {
                    switch (args[i+1]) {
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
        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        int delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }

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
            scores = user.getTopScores(withMods ? 100 : getAmount()).get();
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve top scores");
            return;
        }
        ArrayList<OsuBeatmap> maps = new ArrayList<>();
        for (OsuScore score : scores) {
            if (withMods && !Arrays.equals(score.getEnabledMods(), mods))
                continue;
            OsuBeatmap map;
            try {
                map = DBProvider.getBeatmap(score.getBeatmapID());
            } catch (SQLException | ClassNotFoundException e) {
                try {
                    map = score.getBeatmap().get();
                } catch (OsuAPIException e1) {
                    continue;
                }
                try {
                    DBProvider.addBeatmap(map);
                } catch (ClassNotFoundException | SQLException e1) {
                    e1.printStackTrace();
                }
            }
            if (getScoreCondition(score, mode) && getMapCondition(map)) {
                Main.fileInteractor.prepareFiles(map);
                maps.add(map);
                if (maps.size() >= 5) break;
            }
        }

        // Check if filtering is necessary
        if (!getScoreCondition(null, null) || !getMapCondition(null)) {
            scores = scores.stream()
                    .filter(s -> maps.stream().anyMatch(m -> m.getID() == s.getBeatmapID()))
                    .collect(Collectors.toList());
        } else if (withMods) {
            GameMod[] finalMods = mods;
            scores = scores.stream()
                    .filter(s -> Arrays.equals(s.getEnabledMods(), finalMods))
                    .collect(Collectors.toList());
        }
        if (scores.size() == 0) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(noScoreMessage(user.getUsername(), withMods));
            return;
        }
        new BotMessage(event, getMessageType()).user(user).osuscores(scores)
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
                return "Enter `" + statics.prefix + "topscores [-m <s/t/c/m for mode>] [osu name] [+<nm/hd/nfeznc/...>]` to make me list the user's top 5 scores."
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
