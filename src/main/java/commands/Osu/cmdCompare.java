package main.java.commands.Osu;

import de.maxikg.osuapi.model.*;
import main.java.commands.Command;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.*;

import static de.maxikg.osuapi.model.Mod.parseFlagSum;
import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.mods_flag;

public class cmdCompare implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }

        boolean withMods = args.length > 0 && String.join(" ", args).matches("(.* )?-m(od(s)?)?( .*)?");
        Set<Mod> mods = null;
        List<String> argList = new LinkedList<>(Arrays.asList(args));
        if (withMods) {
            argList.remove("-m");
            argList.remove("-mod");
            argList.remove("-mods");
        }

        String name = argList.size() > 0 ? String.join(" ", argList) : Main.discLink.getOsu(event.getAuthor().getId());
        if (name == null) {
            event.getTextChannel().sendMessage(help(1)).queue();
            return;
        }
        String mapID = "";

        int counter = 100;
        for (Message msg: event.getTextChannel().getIterableHistory()) {
            if (msg.getAuthor().equals(event.getJDA().getSelfUser()) && msg.getEmbeds().size() > 0) {
                MessageEmbed msgEmbed = msg.getEmbeds().iterator().next();
                List<MessageEmbed.Field> fields = msgEmbed.getFields();
                if (fields.get(0).getValue().matches(".*\\{( ?\\d+ ?\\/){3} ?\\d+ ?\\}.*")
                        || (fields.size() >= 5 &&
                        fields.get(5).getValue().matches(".*\\{( ?\\d+ ?\\/){3} ?\\d+ ?\\}.*"))) {
                    mapID = msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/")+1);
                    if (withMods) {
                        if (fields.size() >= 5 && fields.get(0).getValue().contains("+")) {
                            mods = parseFlagSum(mods_flag(fields.get(0).getValue().split("\\+")[1]));
                        } else if (fields.get(0).getName().contains("+")) {
                            mods = parseFlagSum(mods_flag(fields.get(0).getName().split("\\+")[1].split(" ")[0]));
                        }
                    }
                    break;
                }
            }
            if (--counter == 0) {
                event.getTextChannel().sendMessage("Could not find last `" + statics.prefix + "recent`, must " +
                        "be too old").queue();
                return;
            }
        }

        Collection<BeatmapScore> scores =  Main.osu.getScores(Integer.parseInt(mapID)).username(name).query();
        if (scores.size() == 0) {
            event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                    mapID + "`").queue();
            return;
        }

        BeatmapScore score = scores.iterator().next();
        if (withMods) {
            Iterator<BeatmapScore> it = scores.iterator();
            while (it.hasNext() && !(score = it.next()).getEnabledMods().equals(mods));
            if (!score.getEnabledMods().equals(mods)) {
                event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                        mapID + "` with mods `" + abbrvModSet(mods) + "`").queue();
                score = scores.iterator().next();
            }
        }
        User user;
        try {
            user = Main.osu.getUserByUsername(name).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("Could not find osu user `" + name + "`").queue();
            return;
        }
        Beatmap map = Main.osu.getBeatmaps().beatmapId(Integer.parseInt(mapID)).query().iterator().next();
        Collection<UserScore> topPlays = Main.osu.getUserBestByUsername(name).limit(50).query();
        Collection<BeatmapScore> globalPlays = Main.osu.getScores(map.getBeatmapId()).query();
        new BotMessage(event, BotMessage.MessageType.COMPARE).user(user).map(map).beatmapscore(score)
                .topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "compare -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "compare [-m]` to make me show your best play on the map of "
                + "the last `" + statics.prefix + "recent`. Enter `" + statics.prefix + "compare <osu name>` to" +
                        " compare with someone else. If `-m` is added, I will also take the mod into account.";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }
}
