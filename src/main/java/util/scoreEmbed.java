package main.java.util;

import de.maxikg.osuapi.model.*;
import main.java.core.Performance;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.countRetries;
import static main.java.util.utilGeneral.howLongAgo;
import static main.java.util.utilGeneral.secondsToTimeFormat;

public class scoreEmbed {

    private static DecimalFormat  df = new DecimalFormat("0.00");
    private static int shortFormatDelay = 45000;

    private static String topPlayDescription(int topPlayIndex, int globalPlayIndex) {
        String descriptionStr = "__**";
        if (topPlayIndex != -1) {
            descriptionStr += "Personal Best #" + topPlayIndex;
            if (globalPlayIndex != -1)
                descriptionStr += " and ";
        }
        if (globalPlayIndex != -1)
            descriptionStr += "Global Top #" + globalPlayIndex;
        return descriptionStr.equals("__**") ? "" : descriptionStr + "!**__";
    }

    private static String modString(Set<Mod> mods) {
        String out = abbrvModSet(mods);
        if (!out.equals(""))
            out = " +" + out;
        return out;
    }

    private static ArrayList<MessageEmbed.Field> createFields(Performance p, Beatmap m, Object o, String r, String acc) {
        int nmiss;  int n50;  int n100; int n300; int mcombo; int score; double pp;
        if (o instanceof BeatmapScore) {
            nmiss = ((BeatmapScore) o).getCountMiss(); n50 = ((BeatmapScore) o).getCount50();
            n100 = ((BeatmapScore) o).getCount100(); n300 =((BeatmapScore) o).getCount300();
            mcombo = ((BeatmapScore) o).getMaxCombo(); score = ((BeatmapScore) o).getScore();
            pp = ((BeatmapScore) o).getPp();
        } else {
            nmiss = ((UserGame) o).getCountMiss(); n50 = ((UserGame) o).getCount50();
            n100 = ((UserGame) o).getCount100(); n300 =((UserGame) o).getCount300();
            mcombo = ((UserGame) o).getMaxCombo(); score = ((UserGame) o).getScore(); pp = p.getTotalPlayPP();
        }
        String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                + p.getObjectAmount() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
        fields.add(new MessageEmbed.Field("Rank", r,true));
        fields.add(new MessageEmbed.Field("Score",
                "" + NumberFormat.getNumberInstance(Locale.US).format(score), true));
        fields.add(new MessageEmbed.Field("Acc", acc + "% ", true));
        fields.add(new MessageEmbed.Field("PP",
                "**" + df.format(pp) + "**/" + df.format(p.getTotalMapPP()), true));
        fields.add(new MessageEmbed.Field("Combo", mcombo +"/" + m.getMaxCombo(), true));
        fields.add(new MessageEmbed.Field("Hits", "{ " + n300 + " / " + n100 + " / " + n50 +
                " / " + nmiss + " }", true));
        fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
        return fields;
    }

    private static EmbedBuilder createBuilder(Beatmap m, User u, Object o) {
        String mods = o instanceof BeatmapScore ? modString(((BeatmapScore) o).getEnabledMods()) :
                modString(((UserGame) o).getEnabledMods());
        return new EmbedBuilder()
                .setColor(Color.green)
                .setThumbnail("attachment://thumb.jpg")
                .setTitle("" + m.getTitle() + " [" + m.getVersion() + "]" + mods,
                        "https://osu.ppy.sh/b/" + m.getBeatmapId())
                .setAuthor(u.getUsername() + ": "
                            + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRaw()) + "pp (#"
                            + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRank()) + " "
                            + u.getCountry().toString().toUpperCase()
                            + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRankCountry()) + ")",
                    "https://osu.ppy.sh/u/" + u.getUserId(), "https://a.ppy.sh/" + u.getUserId());
    }

    protected static void embedScore(MessageReceivedEvent event, User user, Beatmap map, BeatmapScore score,
                              Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays) {
        Performance performance = new Performance(map);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String acc = df.format( 100*Math.max(0.0D, Math.min(((double)score.getCount50() *
                50.0D + (double)score.getCount100() * 100.0D + (double)score.getCount300() * 300.0D) / ((double)
                (score.getCount50()+score.getCount100()+score.getCount300()+score.getCountMiss()) * 300.0D), 1.0D)));
        String rank = event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");

        EmbedBuilder eb = createBuilder(map, user, score)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFields(performance, map, score, rank, acc));

        event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                "thumb.jpg").embed(eb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                    .addField(new MessageEmbed.Field(rank + "\t" +
                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                        acc + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(score.getPp()) +
                        "pp**/" + df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() +"x/" +
                        map.getMaxCombo() + "x ]\t { "+ score.getCount300() + " / " + score.getCount100() + " / " +
                        score.getCount50() + " / " + score.getCountMiss() + " }", false));
                message.editMessage(eb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    protected static void embedScore(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                              Collection<UserGame> history, Collection<UserScore> topPlays,
                              Collection<BeatmapScore> globalPlays) {
        Performance performance = new Performance(map, score);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        int amountTries = countRetries(user.getUsername(), score, history);
        String acc = df.format( score.getRank().equals("F") ? 100*Math.max(0.0D, Math.min(((double)score.getCount50() *
                50.0D + (double)score.getCount100() * 100.0D + (double)score.getCount300() * 300.0D) / ((double)
                (score.getCount50()+score.getCount100()+score.getCount300()+score.getCountMiss()) * 300.0D), 1.0D)) :
                performance.getAcc());
        String rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");

        EmbedBuilder eb = createBuilder(map, user, score)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFields(performance, map, score, rank, acc));

        MessageBuilder mb = new MessageBuilder("Try #" + amountTries).setEmbed(eb.build());
        event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                "thumb.jpg", mb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                    .addField(new MessageEmbed.Field(rank + "\t" +
                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                        acc + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(performance.getTotalPlayPP()) +
                        "pp**/" + df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() +"x/" +
                        map.getMaxCombo() + "x ]\t { "+ score.getCount300() + " / " + score.getCount100() + " / " +
                        score.getCount50() + " / " + score.getCountMiss() + " }", false));
                mb.setEmbed(eb.build());
                message.editMessage(mb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreMania(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                                     Collection<UserGame> history, Collection<UserScore> topPlays,
                                     Collection<BeatmapScore> globalPlays) {
        // TODO
    }
}
