package main.java.commands;

import de.maxikg.osuapi.model.Beatmap;
import de.maxikg.osuapi.model.BeatmapScore;
import de.maxikg.osuapi.model.User;
import main.java.core.Main;
import main.java.util.scoreEmbed;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cmdScores implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length < 1 || args[0].equals("-h") || args[0].equals("-help")) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        Pattern p = Pattern.compile("(https://osu.ppy.sh/b/)([0-9]{1,8})(.*)");
        String mapID = "";
        try {
            Matcher m = p.matcher(args[0]);
            mapID = m.group(1);
        } catch (Exception e) {
            event.getTextChannel().sendMessage(help(2)).queue();
            return;
        }
        List<String> argList = new LinkedList<>(Arrays.asList(args));
        String name = argList.size() > 1
                ? String.join(" ", argList.subList(1, argList.size()))
                : Main.discLink.getOsu(event.getAuthor().getId());
        if (name == null) {
            event.getTextChannel().sendMessage(help(1)).queue();
            return;
        }

        User user;
        try {
            user = Main.osu.getUserByUsername(name).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("Could not find osu user `" + name + "`").queue();
            return;
        }
        Collection<BeatmapScore> scores =  Main.osu.getScores(Integer.parseInt(mapID)).username(name).query();
        if (scores.size() == 0) {
            event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                    mapID + "`").queue();
            return;
        }
        Beatmap map = Main.osu.getBeatmaps().beatmapId(Integer.parseInt(mapID)).query().iterator().next();
        scoreEmbed.embedScores(event, user, map, scores);
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "scores -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "scores TODO` to make me show ...";
            case 1:
                return "Either specify an osu name as second argument or link your discord to an osu profile via `" +
                        statics.prefix + "link <osu name>" + "`" + help;
            case 2:
                return "The first argument must be the link to a beatmap e.g. `https://osu.ppy.sh/b/1613091&m=0`" + help;
            default:
                return help(0);
        }
    }
}
