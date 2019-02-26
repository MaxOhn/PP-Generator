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

import static de.maxikg.osuapi.model.Mod.createSum;
import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.countRetries;
import static main.java.util.utilGeneral.howLongAgo;
import static main.java.util.utilGeneral.secondsToTimeFormat;
import static main.java.util.utilOsu.key_mods_str;

public class scoreEmbed {

    private static DecimalFormat df = new DecimalFormat("0.00");
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

    private static String keyString(Set<Mod> mods, Beatmap map) {
        String keys = key_mods_str(createSum(mods));
        return "[" + (keys.equals("") ? ((int)map.getDifficultySize() + "K") : keys) + "] ";
    }

    private static ArrayList<MessageEmbed.Field> createFields(Performance p, Beatmap m, Object o, String r, String acc,
                                                              String mods) {
        int nmiss; int n50; int n100; int n300; int mcombo; int score; double pp;
        if (o instanceof BeatmapScore) {
            nmiss = ((BeatmapScore) o).getCountMiss(); n50 = ((BeatmapScore) o).getCount50();
            n100 = ((BeatmapScore) o).getCount100(); n300 = ((BeatmapScore) o).getCount300();
            mcombo = ((BeatmapScore) o).getMaxCombo(); score = ((BeatmapScore) o).getScore();
            pp = ((BeatmapScore) o).getPp();
        } else if (o instanceof UserGame){
            nmiss = ((UserGame) o).getCountMiss(); n50 = ((UserGame) o).getCount50();
            n100 = ((UserGame) o).getCount100(); n300 = ((UserGame) o).getCount300();
            mcombo = ((UserGame) o).getMaxCombo(); score = ((UserGame) o).getScore(); pp = p.getTotalPlayPP();
        } else { // UserScore
            nmiss = ((UserScore) o).getCountMiss(); n50 = ((UserScore) o).getCount50();
            n100 = ((UserScore) o).getCount100(); n300 = ((UserScore) o).getCount300();
            mcombo = ((UserScore) o).getMaxCombo(); score = ((UserScore) o).getScore(); pp = ((UserScore) o).getPp();
        }
        String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                + p.getObjectAmount() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
        fields.add(new MessageEmbed.Field("Rank", r + mods, true));
        fields.add(new MessageEmbed.Field("Score",
                "" + NumberFormat.getNumberInstance(Locale.US).format(score), true));
        fields.add(new MessageEmbed.Field("Acc", acc + "% ", true));
        fields.add(new MessageEmbed.Field("PP",
                "**" + df.format(pp) + "**/" + df.format(p.getTotalMapPP()), true));
        fields.add(new MessageEmbed.Field("Combo", mcombo + "/" + m.getMaxCombo(), true));
        fields.add(new MessageEmbed.Field("Hits", "{ " + n300 + " / " + n100 + " / " + n50 +
                " / " + nmiss + " }", true));
        fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
        return fields;
    }

    private static ArrayList<MessageEmbed.Field> createFieldsMania(Performance p, Beatmap m, Object o, String acc,
                                                                   String rank, String mods) {
        return createFieldsMania(p, m, o, acc, rank, mods, "");
    }

    private static ArrayList<MessageEmbed.Field> createFieldsMania(Performance p, Beatmap m, Object o, String acc,
                String rank, String mods, String currPP) {
        int nmiss;  int n50;  int n100; int nkatu; int n300; int ngeki; int mcombo; int score; double pp; String r;
        if (o instanceof BeatmapScore) {
            nmiss = ((BeatmapScore) o).getCountMiss(); n50 = ((BeatmapScore) o).getCount50();
            n100 = ((BeatmapScore) o).getCount100(); n300 =((BeatmapScore) o).getCount300();
            mcombo = ((BeatmapScore) o).getMaxCombo(); score = ((BeatmapScore) o).getScore();
            pp = ((BeatmapScore) o).getPp(); nkatu = ((BeatmapScore)o).getCountKatu();
            ngeki = ((BeatmapScore)o).getCountGeki(); r = ((BeatmapScore)o).getRank();
        } else {
            nmiss = ((UserGame) o).getCountMiss(); n50 = ((UserGame) o).getCount50();
            n100 = ((UserGame) o).getCount100(); n300 =((UserGame) o).getCount300();
            mcombo = ((UserGame) o).getMaxCombo(); score = ((UserGame) o).getScore(); pp = p.getTotalPlayPP();
            nkatu = ((UserGame)o).getCountKatu(); r = ((UserGame)o).getRank(); ngeki = ((UserGame)o).getCountGeki();
        }
        String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                + p.getObjectAmount() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
        fields.add(new MessageEmbed.Field("Rank", rank + mods,true));
        fields.add(new MessageEmbed.Field("Score",
                "" + NumberFormat.getNumberInstance(Locale.US).format(score), true));
        fields.add(new MessageEmbed.Field("Acc", acc + "% ", true));
        fields.add(new MessageEmbed.Field("PP",
                "**" + (r.equals ("F")? "-" : currPP.equals("") ? df.format(pp) : currPP) + "**/" +
                        df.format(p.getTotalMapPP()), true));
        fields.add(new MessageEmbed.Field("Combo", mcombo +"/" + m.getMaxCombo(), true));
        fields.add(new MessageEmbed.Field("Hits", ngeki + "/" + n300 + "/" + nkatu + "/" +
                n100 + "/" + n50 + "/" + nmiss, true));
        fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
        return fields;
    }

    private static EmbedBuilder createBuilder(Beatmap m, User u) {
        return new EmbedBuilder()
                .setColor(Color.green)
                .setThumbnail("attachment://thumb.jpg")
                .setTitle(m.getArtist() + " - " + m.getTitle() + " [" + m.getVersion() + "]",
                        "https://osu.ppy.sh/b/" + m.getBeatmapId())
                .setAuthor(u.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRank()) + " "
                                + u.getCountry().toString().toUpperCase()
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRankCountry()) + ")",
                        "https://osu.ppy.sh/u/" + u.getUserId(), "https://a.ppy.sh/" + u.getUserId());
    }

    private static String updateBuilderTitle(Beatmap m) {
        return m.getArtist() + " - " + m.getTitle() + " [" + m.getVersion() + "]"
                + " [" + df.format(m.getDifficultyRating()) + "★]";
    }

    public static void embedScoreCompare(MessageReceivedEvent event, User user, Beatmap map, BeatmapScore score,
                                         Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays) {
        Performance performance = new Performance(map, score, 0);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String acc = df.format(100 * Math.max(0.0D, Math.min(((double) score.getCount50() *
                50.0D + (double) score.getCount100() * 100.0D + (double) score.getCount300() * 300.0D) / ((double)
                (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()) * 300.0D), 1.0D)));
        String rank = event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());

        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFields(performance, map, score, rank, acc, mods));

        event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                "thumb.jpg").embed(eb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                acc + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(score.getPp()) +
                                "pp**/" + df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCount300() + " / " + score.getCount100() + " / " +
                                score.getCount50() + " / " + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                message.editMessage(eb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreRecentBest(MessageReceivedEvent event, User user, Beatmap map, UserScore score,
                                        Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays, GameMode mode) {
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String mods = modString(score.getEnabledMods());
        Performance performance;
        String acc;
        String rank;
        EmbedBuilder eb;

        switch (mode) {
            case STANDARD:
                performance = new Performance(map, score, mode.getValue());
                acc = df.format(100 * Math.max(0.0D, Math.min(((double) score.getCount50() *
                        50.0D + (double) score.getCount100() * 100.0D + (double) score.getCount300() * 300.0D) / ((double)
                        (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()) * 300.0D), 1.0D)));
                rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                        + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");

                eb = createBuilder(map, user)
                        .setTimestamp(score.getDate().toInstant())
                        .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
                eb.getFields().addAll(createFields(performance, map, score, rank, acc, mods));

                MessageBuilder mb = new MessageBuilder(eb.build());
                event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                        "thumb.jpg", mb.build()).queue(message -> {
                    try {
                        Thread.sleep(shortFormatDelay);
                        eb.clearFields().setTimestamp(null)
                                .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                        acc + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(score.getPp()) +
                                        "pp**/" + df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                        map.getMaxCombo() + "x ]\t { " + score.getCount300() + " / " + score.getCount100() + " / " +
                                        score.getCount50() + " / " + score.getCountMiss() + " }", false));
                        eb.setTitle(updateBuilderTitle(map), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                        mb.setEmbed(eb.build());
                        message.editMessage(mb.build()).queue();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                return;
            case OSU_MANIA:
                performance = new Performance(map, score, 3);
                int passedObjects = score.getCountGeki() + score.getCount300() + score.getCountKatu() + score.getCount100() +
                        score.getCount50() + score.getCountMiss();
                acc = df.format(100 * Math.max(0.0D, Math.min(((double) score.getCount50() * 50.0D +
                        (double) score.getCount100() * 100.0D + (double) score.getCountKatu() * 200.0D + (double) score.getCount300() *
                        300.0D + (double) score.getCountGeki() * 300.0D) / ((double) passedObjects * 300.0D), 1.0D)));
                rank = event.getJDA().getGuildById(secrets.devGuildID)
                        .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();

                eb = createBuilder(map, user)
                        .setTimestamp(score.getDate().toInstant())
                        .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
                eb.getFields().addAll(createFieldsMania(performance, map, score, acc, rank, mods));
                eb.setTitle(keyString(score.getEnabledMods(), map) + eb.build().getTitle(), eb.build().getUrl());

                event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                        "thumb.jpg").embed(eb.build()).queue(message -> {
                    try {
                        Thread.sleep(shortFormatDelay);
                        eb.clearFields().setTimestamp(null)
                                .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                        acc + "%)\t" + howLongAgo(score.getDate()), "**" + (score.getRank().equals("F") ?
                                        "-" : (df.format(score.getPp()) + "pp")) + "**/" +
                                        df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                        map.getMaxCombo() + "x ]\t { " + score.getCountGeki() + "/" + score.getCount300() +
                                        "/" + score.getCountKatu() + "/" + score.getCount100() + "/" + score.getCount50()
                                        + "/" + score.getCountMiss() + " }", false));
                        eb.setTitle(updateBuilderTitle(map), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                        message.editMessage(eb.build()).queue();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                break;
            default: break;
        }
    }

    public static void embedScoreRecent(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                                        Collection<UserGame> history, Collection<UserScore> topPlays,
                                        Collection<BeatmapScore> globalPlays) {
        Performance performance = new Performance(map, score, 0);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        int amountTries = countRetries(user.getUsername(), score, history);
        String acc = df.format(score.getRank().equals("F") ? 100 * Math.max(0.0D, Math.min(((double) score.getCount50() *
                50.0D + (double) score.getCount100() * 100.0D + (double) score.getCount300() * 300.0D) / ((double)
                (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()) * 300.0D), 1.0D)) :
                performance.getAcc());
        String rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());

        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFields(performance, map, score, rank, acc, mods));

        MessageBuilder mb = new MessageBuilder("Try #" + amountTries).setEmbed(eb.build());
        event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                "thumb.jpg", mb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                acc + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(performance.getTotalPlayPP()) +
                                "pp**/" + df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCount300() + " / " + score.getCount100() + " / " +
                                score.getCount50() + " / " + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                mb.setEmbed(eb.build());
                message.editMessage(mb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreCompareMania(MessageReceivedEvent event, User user, Beatmap map, BeatmapScore score,
                                              Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays) {
        Performance performance = new Performance(map, score, 3);
        int passedObjects = score.getCountGeki() + score.getCount300() + score.getCountKatu() + score.getCount100() +
                score.getCount50() + score.getCountMiss();
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String acc = df.format(100 * Math.max(0.0D, Math.min(((double) score.getCount50() * 50.0D +
                (double) score.getCount100() * 100.0D + (double) score.getCountKatu() * 200.0D + (double) score.getCount300() *
                300.0D + (double) score.getCountGeki() * 300.0D) / ((double) passedObjects * 300.0D), 1.0D)));
        String rank = event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();
        String mods = modString(score.getEnabledMods());

        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFieldsMania(performance, map, score, acc, rank, mods));
        eb.setTitle(keyString(score.getEnabledMods(), map) + eb.build().getTitle(), eb.build().getUrl());

        event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                "thumb.jpg").embed(eb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                acc + "%)\t" + howLongAgo(score.getDate()), "**" + (score.getRank().equals("F") ?
                                "-" : (df.format(score.getPp()) + "pp")) + "**/" +
                                df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCountGeki() + "/" + score.getCount300() +
                                "/" + score.getCountKatu() + "/" + score.getCount100() + "/" + score.getCount50()
                                + "/" + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                message.editMessage(eb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreRecentMania(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                                             Collection<UserGame> history, Collection<UserScore> topPlays,
                                             Collection<BeatmapScore> globalPlays) {
        embedScoreRecentMania(event, user, map, score, history, topPlays, globalPlays, null);
    }

    public static void embedScoreRecentMania(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                                             Collection<UserGame> history, Collection<UserScore> topPlays,
                                             Collection<BeatmapScore> globalPlays, BeatmapScore bestScore) {
        // countgeki = 320, count300 = 300, countkatu = 200, count100 = 100, count50 = 50, countmiss = miss
        Performance performance = new Performance(map, score, 3);
        int passedObjects = score.getCountGeki() + score.getCount300() + score.getCountKatu() + score.getCount100() +
                score.getCount50() + score.getCountMiss();
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        int amountTries = countRetries(user.getUsername(), score, history);
        String currPP = df.format(bestScore == null ? performance.getTotalPlayPP() : bestScore.getPp());
        String acc = df.format(100 * Math.max(0.0D, Math.min(((double) score.getCount50() * 50.0D +
                (double) score.getCount100() * 100.0D + (double) score.getCountKatu() * 200.0D + (double) score.getCount300() *
                300.0D + (double) score.getCountGeki() * 300.0D) / ((double) passedObjects * 300.0D), 1.0D)));
        String rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());

        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFieldsMania(performance, map, score, acc, rank, mods, currPP));
        eb.setTitle(keyString(score.getEnabledMods(), map) + eb.build().getTitle(), eb.build().getUrl());

        MessageBuilder mb = new MessageBuilder("Try #" + amountTries).setEmbed(eb.build());
        event.getTextChannel().sendFile(new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg"),
                "thumb.jpg", mb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                acc + "%)\t" + howLongAgo(score.getDate()), "**" + (score.getRank().equals("F") ?
                                "-" : currPP) + "**/" +
                                df.format(performance.getTotalMapPP()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCountGeki() + "/" + score.getCount300() +
                                "/" + score.getCountKatu() + "/" + score.getCount100() + "/" + score.getCount50()
                                + "/" + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                mb.setEmbed(eb.build());
                message.editMessage(mb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
