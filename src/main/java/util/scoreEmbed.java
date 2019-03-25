package main.java.util;

import de.maxikg.osuapi.model.*;
import main.java.core.Main;
import main.java.core.Performance;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import static de.maxikg.osuapi.model.Mod.createSum;
import static main.java.util.utilGeneral.howLongAgo;
import static main.java.util.utilGeneral.secondsToTimeFormat;
import static main.java.util.utilOsu.*;

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

    private static boolean prepareFiles(int mapID) {
        boolean success = true;
        if (!new File(secrets.thumbPath + mapID + "l.jpg").isFile())
            success = Main.fileInteractor.downloadMapThumb(mapID);
        if (!new File(secrets.mapPath + mapID + ".osu").isFile())
            success &= Main.fileInteractor.downloadMap(mapID);
        return success;
    }

    private static ArrayList<MessageEmbed.Field> createFields(Performance p, Beatmap m, String r, String mods) {
        String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                + p.getnObjects() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
        fields.add(new MessageEmbed.Field("Rank", r + mods, true));
        fields.add(new MessageEmbed.Field("Score",
                "" + NumberFormat.getNumberInstance(Locale.US).format(p.getScore()), true));
        fields.add(new MessageEmbed.Field("Acc", p.getAccString() + "% ", true));
        fields.add(new MessageEmbed.Field("PP",
                "**" + df.format(p.getPp()) + "**/" + df.format(p.getPpMax()), true));
        fields.add(new MessageEmbed.Field("Combo", p.getCombo() + "/" + m.getMaxCombo(), true));
        fields.add(new MessageEmbed.Field("Hits", "{ " + p.getN300() + " / " + p.getN100() + " / "
                + p.getN50() + " / " + p.getNMisses() + " }", true));
        fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
        return fields;
    }

    private static ArrayList<MessageEmbed.Field> createFieldsMania(Performance p, Beatmap m, String rank, String mods) {
        String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                + p.getnObjects() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
        fields.add(new MessageEmbed.Field("Rank", rank + mods,true));
        fields.add(new MessageEmbed.Field("Score",
                "" + NumberFormat.getNumberInstance(Locale.US).format(p.getScore()), true));
        fields.add(new MessageEmbed.Field("Acc", p.getAccString() + "% ", true));
        fields.add(new MessageEmbed.Field("PP",
                "**" + (p.getRank().equals ("F")? "-" : p.getPp()) + "**/" +
                        df.format(p.getPpMax()), true));
        fields.add(new MessageEmbed.Field("Combo", p.getCombo() +"/" + m.getMaxCombo(), true));
        fields.add(new MessageEmbed.Field("Hits", p.getNGeki() + "/" + p.getN300() + "/" + p.getNKatu() + "/" +
                p.getN100() + "/" + p.getN50() + "/" + p.getNMisses(), true));
        fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
        return fields;
    }

    private static ArrayList<MessageEmbed.Field> createFieldsTaiko(Performance p, Beatmap m, String rank, String mods) {
        String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                + p.getnObjects() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
        ArrayList<MessageEmbed.Field> fields = new ArrayList<>();
        fields.add(new MessageEmbed.Field("Rank", rank + mods,true));
        fields.add(new MessageEmbed.Field("Score",
                "" + NumberFormat.getNumberInstance(Locale.US).format(p.getScore()), true));
        fields.add(new MessageEmbed.Field("Acc", p.getAccString() + "% ", true));
        fields.add(new MessageEmbed.Field("PP",
                "**" + (p.getRank().equals ("F")? "-" : p.getPp()) + "**/" +
                        df.format(p.getPpMax()), true));
        fields.add(new MessageEmbed.Field("Combo", p.getCombo() +"/" + m.getMaxCombo(), true));
        fields.add(new MessageEmbed.Field("Hits", p.getN300() + "/" + p.getN100() + "/" + p.getNMisses(), true));
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

    private static String updateBuilderTitle(Beatmap m, Set<Mod> mods, double starRating) {
        return (mods == null ? "" : keyString(mods, m)) + m.getArtist() + " - " + m.getTitle() + " [" + m.getVersion() + "]"
                + " [" + df.format(starRating) + "★]";
    }

    public void embedScoreCompare(MessageReceivedEvent event, User user, Beatmap map, BeatmapScore score,
                                         Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays) {
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        Performance performance = new Performance().map(map).beatmapscore(score);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String rank = event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());

        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFields(performance, map, rank, mods));

        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");

        event.getTextChannel().sendFile(bgThumb,
                "thumb.jpg").embed(eb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(score.getPp()) +
                                "pp**/" + df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCount300() + " / " + score.getCount100() + " / " +
                                score.getCount50() + " / " + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map, null, performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                message.editMessage(eb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void embedScoreRecentBest(MessageReceivedEvent event, User user, Beatmap map, UserScore score,
                                        Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays, GameMode mode) {
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String mods = modString(score.getEnabledMods());
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        String rank;
        EmbedBuilder eb;
        MessageBuilder mb;
        Performance performance = new Performance().map(map).userscore(score).mode(mode);
        switch (mode) {
            case STANDARD:
                rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                        + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
                eb = createBuilder(map, user)
                        .setTimestamp(score.getDate().toInstant())
                        .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
                eb.getFields().addAll(createFields(performance, map, rank, mods));
                mb = new MessageBuilder(eb.build());
                event.getTextChannel().sendFile(bgThumb,
                        "thumb.jpg", mb.build()).queue(message -> {
                    try {
                        Thread.sleep(shortFormatDelay);
                        eb.clearFields().setTimestamp(null)
                                .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                        performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(score.getPp()) +
                                        "pp**/" + df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                        map.getMaxCombo() + "x ]\t { " + score.getCount300() + " / " + score.getCount100() + " / " +
                                        score.getCount50() + " / " + score.getCountMiss() + " }", false));
                        eb.setTitle(updateBuilderTitle(map, null, performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                        mb.setEmbed(eb.build());
                        message.editMessage(mb.build()).queue();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                return;
            case OSU_MANIA:
                rank = event.getJDA().getGuildById(secrets.devGuildID)
                        .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();
                eb = createBuilder(map, user)
                        .setTimestamp(score.getDate().toInstant())
                        .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
                eb.getFields().addAll(createFieldsMania(performance, map, rank, mods));
                eb.setTitle(keyString(score.getEnabledMods(), map) + eb.build().getTitle(), eb.build().getUrl());
                event.getTextChannel().sendFile(bgThumb,
                        "thumb.jpg").embed(eb.build()).queue(message -> {
                    try {
                        Thread.sleep(shortFormatDelay);
                        eb.clearFields().setTimestamp(null)
                                .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                        performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**"
                                        + (score.getRank().equals("F") ? "-" : (df.format(score.getPp()) + "pp")) + "**/" +
                                        df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                        map.getMaxCombo() + "x ]\t { " + score.getCountGeki() + "/" + score.getCount300() +
                                        "/" + score.getCountKatu() + "/" + score.getCount100() + "/" + score.getCount50()
                                        + "/" + score.getCountMiss() + " }", false));
                        eb.setTitle(updateBuilderTitle(map, score.getEnabledMods(), performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                        message.editMessage(eb.build()).queue();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                break;
            case TAIKO:
                rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                        + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
                eb = createBuilder(map, user)
                        .setTimestamp(score.getDate().toInstant())
                        .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
                eb.getFields().addAll(createFieldsTaiko(performance, map, rank, mods));
                mb = new MessageBuilder(eb.build());
                event.getTextChannel().sendFile(bgThumb,
                        "thumb.jpg", mb.build()).queue(message -> {
                    try {
                        Thread.sleep(shortFormatDelay);
                        eb.clearFields().setTimestamp(null)
                                .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                        NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                        performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**"
                                        + (score.getRank().equals("F") ? "-" : (df.format(score.getPp()) + "pp")) + "**/" +
                                        df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                        map.getMaxCombo() + "x ]\t { " + score.getCount300() +
                                        "/" + score.getCount100() + "/" + score.getCountMiss() + " }", false));
                        eb.setTitle(updateBuilderTitle(map, null, performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                        mb.setEmbed(eb.build());
                        message.editMessage(mb.build()).queue();
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
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        Performance performance = new Performance().map(map).usergame(score);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        int amountTries = countRetries(user.getUsername(), score, history);
        String rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());
        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFields(performance, map, rank, mods));
        MessageBuilder mb = new MessageBuilder("Try #" + amountTries).setEmbed(eb.build());
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        event.getTextChannel().sendFile(bgThumb,
                "thumb.jpg", mb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**" + df.format(performance.getPp()) +
                                "pp**/" + df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCount300() + " / " + score.getCount100() + " / " +
                                score.getCount50() + " / " + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map, null, performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                mb.setEmbed(eb.build());
                message.editMessage(mb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreCompareMania(MessageReceivedEvent event, User user, Beatmap map, BeatmapScore score,
                                              Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays) {
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        Performance performance = new Performance().map(map).beatmapscore(score).mode(GameMode.OSU_MANIA);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String rank = event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();
        String mods = modString(score.getEnabledMods());
        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFieldsMania(performance, map, rank, mods));
        eb.setTitle(keyString(score.getEnabledMods(), map) + eb.build().getTitle(), eb.build().getUrl());
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        event.getTextChannel().sendFile(bgThumb,"thumb.jpg").embed(eb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**"
                                + (score.getRank().equals("F") ? "-" : (df.format(score.getPp()) + "pp")) + "**/" +
                                df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCountGeki() + "/" + score.getCount300() +
                                "/" + score.getCountKatu() + "/" + score.getCount100() + "/" + score.getCount50()
                                + "/" + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map, score.getEnabledMods(), performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                message.editMessage(eb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreRecentMania(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                                             Collection<UserGame> history, Collection<UserScore> topPlays,
                                             Collection<BeatmapScore> globalPlays) {
        // countgeki = 320, count300 = 300, countkatu = 200, count100 = 100, count50 = 50, countmiss = miss
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        Performance performance = new Performance().map(map).usergame(score).mode(GameMode.OSU_MANIA);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        int amountTries = countRetries(user.getUsername(), score, history);
        String rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());
        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFieldsMania(performance, map, rank, mods));
        eb.setTitle(keyString(score.getEnabledMods(), map) + eb.build().getTitle(), eb.build().getUrl());
        MessageBuilder mb = new MessageBuilder("Try #" + amountTries).setEmbed(eb.build());
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        event.getTextChannel().sendFile(bgThumb,"thumb.jpg", mb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**"
                                + (score.getRank().equals("F") ? "-" : performance.getPp() + "pp") + "**/" +
                                df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCountGeki() + "/" + score.getCount300() +
                                "/" + score.getCountKatu() + "/" + score.getCount100() + "/" + score.getCount50()
                                + "/" + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map, score.getEnabledMods(), performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                mb.setEmbed(eb.build());
                message.editMessage(mb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreRecentTaiko(MessageReceivedEvent event, User user, Beatmap map, UserGame score,
                                             Collection<UserGame> history, Collection<UserScore> topPlays,
                                             Collection<BeatmapScore> globalPlays) {
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        Performance performance = new Performance().map(map).usergame(score).mode(GameMode.TAIKO);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        int amountTries = countRetries(user.getUsername(), score, history);
        String rank = event.getJDA().getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention()
                + (score.getRank().equals("F") ? " (" + performance.getCompletion() + "%)" : "");
        String mods = modString(score.getEnabledMods());
        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFieldsTaiko(performance, map, rank, mods));
        MessageBuilder mb = new MessageBuilder("Try #" + amountTries).setEmbed(eb.build());
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        event.getTextChannel().sendFile(bgThumb,"thumb.jpg", mb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**" + (score.getRank().equals("F") ?
                                "-" : performance.getPp() + "pp") + "**/" +
                                df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCount300() +
                                "/" + score.getCount100() + "/" + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map, null, performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                mb.setEmbed(eb.build());
                message.editMessage(mb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScoreCompareTaiko(MessageReceivedEvent event, User user, Beatmap map, BeatmapScore score,
                                              Collection<UserScore> topPlays, Collection<BeatmapScore> globalPlays) {
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        Performance performance = new Performance().map(map).beatmapscore(score).mode(GameMode.TAIKO);
        int topPlayIndex = utilOsu.indexInTopPlays(score, topPlays);
        int globalPlayIndex = utilOsu.indexInGlobalPlays(score, globalPlays);
        String rank = event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();
        String mods = modString(score.getEnabledMods());
        EmbedBuilder eb = createBuilder(map, user)
                .setTimestamp(score.getDate().toInstant())
                .setDescription(topPlayDescription(topPlayIndex, globalPlayIndex));
        eb.getFields().addAll(createFieldsTaiko(performance, map, rank, mods));
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        event.getTextChannel().sendFile(bgThumb,
                "thumb.jpg").embed(eb.build()).queue(message -> {
            try {
                Thread.sleep(shortFormatDelay);
                eb.clearFields().setTimestamp(null)
                        .addField(new MessageEmbed.Field(rank + mods + "\t" +
                                NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" +
                                performance.getAccString() + "%)\t" + howLongAgo(score.getDate()), "**" + (score.getRank().equals("F") ?
                                "-" : (df.format(score.getPp()) + "pp")) + "**/" +
                                df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                                map.getMaxCombo() + "x ]\t { " + score.getCount300() +
                                "/" + score.getCount100() + "/" + score.getCountMiss() + " }", false));
                eb.setTitle(updateBuilderTitle(map, null, performance.getStarRating()), "https://osu.ppy.sh/b/" + map.getBeatmapId());
                message.editMessage(eb.build()).queue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void embedScores(MessageReceivedEvent event, User user, Beatmap map, Collection<BeatmapScore> scores) {
        boolean fileSuccess = prepareFiles(map.getBeatmapId());
        List<BeatmapScore> orderedScores = new ArrayList<>(scores);
        orderedScores.sort(Comparator.comparing(BeatmapScore::getPp).reversed());
        EmbedBuilder eb = createBuilder(map, user);
        File bgThumb = fileSuccess
                ? new File(secrets.thumbPath + map.getBeatmapSetId() + "l.jpg")
                : new File(secrets.thumbPath + "bgNotFound.png");
        Performance performance = new Performance().map(map);
        String rank, mods, name, value;
        int idx = 1;
        for (BeatmapScore score : scores) {
            performance.beatmapscore(score);
            rank = event.getJDA().getGuildById(secrets.devGuildID)
                    .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();
            mods = modString(score.getEnabledMods());
            name = "**" + idx + ".** " + rank + mods + "\t[" + df.format(performance.getStarRating()) + "★]\t" +
                    NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" + performance.getAccString() + "%)";
            switch (map.getMode()) {
                case STANDARD:
                    value = "**" + df.format(score.getPp()) + "pp**/" +
                            df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                            map.getMaxCombo() + "x ]\t {" + score.getCount300() + "/" + score.getCount100() + "/" +
                            score.getCount50() + "/" + score.getCountMiss() + "}\t" + howLongAgo(score.getDate());
                break;
                case OSU_MANIA:
                    value = "**" + df.format(score.getPp()) + "pp**/" +
                            df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                            map.getMaxCombo() + "x ]\t {" + score.getCountGeki() + "/" + score.getCount300() + "/"
                            + score.getCountKatu() + "/" + score.getCount100() + "/" +
                            score.getCount50() + "/" + score.getCountMiss() + "}\t" + howLongAgo(score.getDate());
                    break;
                case TAIKO:
                    value = "**" + df.format(score.getPp()) + "pp**/" +
                            df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                            map.getMaxCombo() + "x ]\t {" + score.getCount300() + "/" + score.getCount100() + "/"
                            + score.getCountMiss() + "}\t" + howLongAgo(score.getDate());
                    break;
                default: value = ""; break;
            }
            eb.addField(name, value, false);
            idx++;
        }
        event.getTextChannel().sendFile(bgThumb,"thumb.jpg").embed(eb.build()).queue();
    }

    public static void embedTopScores(MessageReceivedEvent event, User user, Collection<UserScore> scores, GameMode mode) {

        for (UserScore score : scores) {
            prepareFiles(score.getBeatmapId());
        }

        EmbedBuilder eb =  new EmbedBuilder()
                .setColor(Color.green)
                .setThumbnail("https://a.ppy.sh/" + user.getUserId())
                .setAuthor(user.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(user.getPpRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(user.getPpRank()) + " "
                                + user.getCountry().toString().toUpperCase()
                                + NumberFormat.getNumberInstance(Locale.US).format(user.getPpRankCountry()) + ")",
                        "https://osu.ppy.sh/u/" + user.getUserId(), "attachment://flag.png");
        File flag = new File(secrets.flagPath + user.getCountry().toString().toUpperCase() + ".png");
        //*
        Performance performance;
        String rank, mods, acc, name, value;
        int idx = 1;
        for (UserScore score : scores) {
            performance = new Performance(map, score, map.getMode().getValue());
            acc = df.format(100 * Math.max(0.0D, Math.min(((double) score.getCount50() *
                    50.0D + (double) score.getCount100() * 100.0D + (double) score.getCount300() * 300.0D) / ((double)
                    (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()) * 300.0D), 1.0D)));
            rank = event.getJDA().getGuildById(secrets.devGuildID)
                    .getEmoteById(utilOsu.getRankEmote(score.getRank()).getValue()).getAsMention();
            mods = modString(score.getEnabledMods());
            name = "**" + idx + ".** " + rank + mods + "\t[" + df.format(performance.getStarRating()) + "★]\t" +
                    NumberFormat.getNumberInstance(Locale.US).format(score.getScore()) + "\t(" + acc + "%)";
            value = "**" + df.format(score.getPp()) + "pp**/" +
                    df.format(performance.getPpMax()) + "PP\t[ " + score.getMaxCombo() + "x/" +
                    map.getMaxCombo() + "x ]\t {" + score.getCount300() + "/" + score.getCount100() + "/" +
                    score.getCount50() + "/" + score.getCountMiss() + "}\t" + howLongAgo(score.getDate());
            eb.addField(name, value, false);
            idx++;
        }
        event.getTextChannel().sendFile(flag, "flag.png").embed(eb.build()).queue();
        //*/
    }
}
