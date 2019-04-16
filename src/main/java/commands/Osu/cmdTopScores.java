package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
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
import java.util.stream.Collectors;

public class cmdTopScores implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
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
                            event.getTextChannel().sendMessage(help(2)).queue();
                            return;
                        case "m": mode = GameMode.MANIA; break;
                        default:
                            event.getTextChannel().sendMessage(help(3)).queue();
                            return;
                    }
                } else {
                    event.getTextChannel().sendMessage(help(3)).queue();
                    return;
                }
            }
        }
        ArrayList<String> argList = new ArrayList<>(Arrays.asList(args));
        int delIndex = Math.max(argList.indexOf("-m"), argList.indexOf("-mode"));
        if (delIndex > -1) {
            argList.remove(delIndex + 1);
            argList.remove(delIndex);
        }

        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return;
            }
        } else
            name = String.join(" ", argList);
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(mode).build());
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        Collection<OsuScore> scores = null;
        try {
            scores = user.getTopScores(getAmount()).get();
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        ArrayList<OsuBeatmap> maps = new ArrayList<>();
        for (OsuScore score : scores) {
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
            if (getCondition(map)) {
                Main.fileInteractor.prepareFiles(map);
                maps.add(map);
            }
        }
        if (!getCondition(null)) {
            scores = scores.stream()
                    .filter(s -> maps.stream().anyMatch(m -> m.getID() == s.getBeatmapID()))
                    .collect(Collectors.toList());
        }
        if (scores.size() == 0) {
            if (getMessageType() == BotMessage.MessageType.TOPSCORES) {
                event.getTextChannel().sendMessage("Could not find any scores from user `" + user.getUsername() + "`").queue();
            } else if (getMessageType() == BotMessage.MessageType.TOPSOTARKS) {
                event.getTextChannel().sendMessage("`" + user.getUsername() + "` appears to not have any Sotarks scores in the"
                        + " personal top 100 and I could not be any prouder \\:')").queue();
            }
            return;
        }
        new BotMessage(event, getMessageType()).user(user).osuscores(scores)
                .maps(maps.stream().limit(5).collect(Collectors.toCollection(ArrayList::new)))
                .mode(mode)
                .buildAndSend();
    }

    int getAmount() {
        return 5;
    }

    boolean getCondition(OsuBeatmap m) {
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
                return "Enter `" + statics.prefix + "topscores [-m <s/t/c/m for mode>] [osu name]` to make me list the user's top 5 scores."
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
