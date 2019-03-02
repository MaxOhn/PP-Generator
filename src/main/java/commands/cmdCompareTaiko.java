package main.java.commands;

import de.maxikg.osuapi.model.*;
import main.java.core.Main;
import main.java.util.scoreEmbed;
import main.java.util.statics;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Collection;
import java.util.List;

public class cmdCompareTaiko implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return args.length < 2;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }

        String name = args.length > 0 ? args[0] : Main.discLink.getOsu(event.getAuthor().getId());
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
                if (fields.get(0).getValue().matches("(.?)+(\\{)?( ?\\d+ ?\\/){2} ?\\d+ ?(\\})?(.?)+")
                        || (fields.size() >= 5 &&
                        fields.get(5).getValue().matches("(.?)+(\\{)?( ?\\d+ ?\\/){2} ?\\d+ ?(\\})?(.?)+"))) {
                    mapID = msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/")+1);
                    break;
                }
            }
            if (--counter == 0) {
                event.getTextChannel().sendMessage("Could not find last `" + statics.prefix + "recenttaiko`, must " +
                        "be too old").queue();
                return;
            }
        }

        Collection<BeatmapScore> scores = Main.osu.getScores(Integer.parseInt(mapID)).mode(GameMode.TAIKO).username(name).query();
        if (scores.size() == 0) {
            event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                    mapID + "`").queue();
            return;
        }
        BeatmapScore score = scores.iterator().next();
        User user;
        try {
            user = Main.osu.getUserByUsername(name).mode(GameMode.TAIKO).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("Could not find osu user `" + name + "`").queue();
            return;
        }
        Beatmap map = Main.osu.getBeatmaps().beatmapId(Integer.parseInt(mapID)).mode(GameMode.TAIKO).query().iterator().next();
        Collection<UserScore> topPlays = Main.osu.getUserBestByUsername(name).mode(GameMode.TAIKO).limit(50).query();
        Collection<BeatmapScore> globalPlays = Main.osu.getScores(map.getBeatmapId()).mode(GameMode.TAIKO).query();
        scoreEmbed.embedScoreCompareTaiko(event, user, map, score, topPlays, globalPlays);
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "comparetaiko -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "comparetaiko` to make me show your best play on the map of "
                        + "the last `" + statics.prefix + "recenttaiko`. Enter `" + statics.prefix + "comparetaiko <osu name>` to" +
                        " compare with someone else";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }
}
