package main.java.commands.Osu;

import de.maxikg.osuapi.model.Beatmap;
import de.maxikg.osuapi.model.GameMode;
import de.maxikg.osuapi.model.User;
import de.maxikg.osuapi.model.UserScore;
import main.java.commands.Command;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

public class cmdTopScores implements Command {
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
                        case "m": mode = GameMode.OSU_MANIA; break;
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
        User user;
        try {
            user = Main.osu.getUserByUsername(name).mode(mode).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        Collection<UserScore> scores = Main.osu.getUserBestByUsername(name).mode(mode).limit(5).query();
        ArrayList<Beatmap> maps = new ArrayList<>();
        for (UserScore score : scores) {
            try {
            maps.add(Main.osu.getBeatmaps().beatmapId(score.getBeatmapId()).limit(1).mode(mode).query().iterator().next());
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            } catch (NoSuchElementException e) {
                event.getTextChannel().sendMessage("Something went wrong, go ping bade or smth :p").queue();
                e.printStackTrace();
                return;
            }

        }
        new BotMessage(event, BotMessage.MessageType.TOPSCORES).user(user).userscore(scores).maps(maps).mode(mode)
                .buildAndSend();
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