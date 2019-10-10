package main.java.core;

import com.oopsjpeg.osu4j.*;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import main.java.util.utilOsu;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static main.java.util.utilGeneral.howLongAgo;
import static main.java.util.utilGeneral.secondsToTimeFormat;
import static main.java.util.utilOsu.abbrvModSet;

public class BotMessage {

    private MessageChannel channel;
    private User author;
    private MessageType typeM;
    private EmbedBuilder eb;
    private MessageBuilder mb;
    private Performance p;

    private OsuUser u;
    private List<OsuUser> users;
    private OsuScore score;
    private List<OsuScore> scores;
    private ArrayList<OsuBeatmap> maps;

    private String topplays;
    private int retries;

    private boolean filesPrepared;

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final int shortFormatDelay = 45000;

    public BotMessage(MessageChannel channel, MessageType typeM) {
        this.channel  = channel;
        this.typeM = typeM;
        this.eb = new EmbedBuilder().setColor(Color.green);
        this.mb = new MessageBuilder();
        this.p = new Performance();
    }

    public void buildAndSend() {
        buildAndSend(null);
    }

    public void buildAndSend(Runnable runnable) {
        File thumbFile = null;
        int idx;
        String ppString = "**", hitString = "{ ", extendedTitle = "";
        TemporalAccessor timestamp;
        ZonedDateTime date = ZonedDateTime.now();
        switch (typeM) {
            case RECENT:
                if (retries == 0) throw new IllegalStateException(Error.HISTORY.getMsg());
                mb.append("Try #").append(String.valueOf(retries));
            case COMPARE:
            case RECENTBEST:
            case SINGLETOP: {
                eb.setThumbnail("attachment://thumb.jpg");
                eb.setAuthor(u.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPPRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getRank()) + " "
                                + u.getCountry()
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "https://a.ppy.sh/" + u.getID());
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                date = score.getDate();
                timestamp = date.toInstant();
                switch (p.getMode()) {
                    case STANDARD:
                        hitString += p.getN300() + " / " + p.getN100() + " / " + p.getN50();
                        extendedTitle = p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" +
                                p.getMap().getVersion() + "]" + " [" + p.getStarRating() + "★]";
                        break;
                    case TAIKO:
                        hitString += p.getN300() + " / " + p.getN100();
                        extendedTitle = p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" +
                                p.getMap().getVersion() + "]" + " [" + p.getStarRating() + "★]";
                    case MANIA:
                        hitString += hitString.equals("{ ") ? p.getNGeki() + " / " + p.getN300() + " / "
                                + p.getNKatu() + " / " + p.getN100() + " / " + p.getN50() : "";
                        extendedTitle += extendedTitle.equals("") ? getKeyString() + " " + p.getMap().getArtist() + " - "
                                + p.getMap().getTitle() + " [" + p.getMap().getVersion() + "]" + " [" + p.getStarRating() + "★]" : "";
                        break;
                    default: throw new IllegalStateException("GameMode not supported");
                }
                ppString += p.getPp();
                hitString += " / " + p.getNMisses() + " }";
                String mapInfo = "Length: `" + secondsToTimeFormat(p.getMap().getTotalLength()) + "` (`"
                        + secondsToTimeFormat(p.getMap().getHitLength()) + "`) BPM: `" + p.getMap().getBPM() + "` Objects: `"
                        + p.getNObjects() + "`\nCS: `" + p.getMap().getSize() + "` AR: `"
                        + p.getMap().getApproach() + "` OD: `" + p.getMap().getOverall() + "` HP: `"
                        + p.getMap().getDrain() + "` Stars: `" + df.format(p.getMap().getDifficulty()) + "`";
                eb.setTimestamp(timestamp)
                    .setDescription(topplays)
                    .setTitle(getKeyString() + " " + p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" + p.getMap().getVersion()
                                    + "]","https://osu.ppy.sh/b/" + p.getMap().getID())
                    .addField("Rank", getRank() + getModString(),true)
                    .addField("Score", NumberFormat.getNumberInstance(Locale.US).format(p.getScore()),true)
                    .addField("Acc", p.getAcc() + "%",true)
                    .addField("PP", ppString + "**/" + p.getPpMax() + "PP",true)
                    .addField("Combo", "**" + p.getCombo() + "x**/" + p.getMaxCombo() + "x",true)
                    .addField("Hits", hitString,true)
                    .addField("Map Info", mapInfo,true);
                break;
            }
            case SCORES: {
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                eb.setThumbnail("attachment://thumb.jpg");
                eb.setAuthor(u.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPPRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getRank()) + " "
                                + u.getCountry()
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "https://a.ppy.sh/" + u.getID());
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                eb.setTitle(p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" + p.getMap().getVersion()
                        + "]", "https://osu.ppy.sh/b/" + p.getMap().getID());
                List<OsuScore> orderedScores = new ArrayList<>(scores);
                orderedScores.sort(Comparator.comparing(OsuScore::getPp).reversed());
                idx = 1;
                for (OsuScore s : orderedScores) {
                    osuscore(s);
                    String fieldName = "**" + idx++ + ".** " + getRank() + getModString() + "\t[" + p.getStarRating() + "★]\t" +
                            NumberFormat.getNumberInstance(Locale.US).format(s.getScore()) + "\t(" + p.getAcc() + "%)";
                    if (p.getMode() == GameMode.MANIA) fieldName += "\t" + getKeyString();
                    String fieldValue = "**" + p.getPp() + "**/" + p.getPpMax() + "PP\t[ **"
                            + s.getMaxCombo() + "x**/" + p.getMaxCombo() + "x ]\t {";
                    switch (p.getMode()) {
                        case STANDARD:
                            fieldValue += s.getHit300() + "/" + s.getHit100() + "/" + s.getHit50();
                            break;
                        case TAIKO:
                            fieldValue += s.getHit300() + "/" + s.getHit100();
                            break;
                        case MANIA:
                            fieldValue += s.getGekis() + "/" + s.getHit300() + "/" + s.getKatus()
                                    + "/" + s.getHit100() + "/" + s.getHit50();
                            break;
                        default:
                            throw new IllegalStateException(Error.MODE.getMsg());
                    }
                    fieldValue += "/" + s.getMisses() + "}\t" + howLongAgo(s.getDate());
                    eb.addField(fieldName, fieldValue, false);
                }
                break;
            }
            case NOCHOKESCORES:
                if (author == null) throw new IllegalStateException(Error.AUTHOR.getMsg());
                mb.append(author.getAsMention()).append(" No-choke top scores for `").append(u.getUsername()).append("`:");
            case TOPSOTARKS:
                if (mb.isEmpty()) {
                    if (scores.size() < 10) {
                        mb.append("I found ").append(String.valueOf(scores.size())).append(" Sotarks scores in `")
                                .append(u.getUsername()).append("`'s top 100 and this is kinda sad \\:(");
                    } else {
                        mb.append("There are ").append(String.valueOf(scores.size())).append(" Sotarks scores in `")
                                .append(u.getUsername()).append("`'s top 100 and this is very sad \\:(");
                    }
                    scores = scores.stream().limit(5).collect(Collectors.toList());
                }
            case SS:
                if (mb.isEmpty()) {
                    mb.append("I found ").append(String.valueOf(scores.size())).append(" SS scores in `")
                            .append(u.getUsername()).append("`'s top 100");
                    if (scores.size() > 5) mb.append(", here's the top 5 of them:");
                    else mb.append(":");
                    scores = scores.stream().limit(5).collect(Collectors.toList());
                }
            case TOPSCORES: {
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (maps == null) throw new IllegalStateException(Error.MAP.getMsg());
                if (mb.isEmpty() && scores.size() > 5) {
                    mb.append("I found ").append(String.valueOf(scores.size())).append(" scores with the specified mods in `")
                            .append(u.getUsername()).append("`'s top 100, here's the top 5 of them:");
                    scores = scores.stream().limit(5).collect(Collectors.toList());
                }
                eb.setThumbnail("https://a.ppy.sh/" + u.getID());
                eb.setAuthor(u.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPPRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getRank()) + " "
                                + u.getCountry()
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "attachment://thumb.jpg");
                thumbFile = new File(statics.flagPath + u.getCountry() + ".png");
                String mods;
                StringBuilder description = new StringBuilder();
                idx = 1;
                for (OsuScore s : scores) {
                    map(maps.get(idx - 1));
                    osuscore(s);
                    mods = getModString();
                    if (!description.toString().equals("")) description.append("\n");
                    if (typeM == MessageType.NOCHOKESCORES) p.noChoke();
                    description.append("**").append(idx++).append(".** [**")
                            .append(p.getMap().getTitle()).append(" [").append(p.getMap().getVersion()).append("]**](https://osu.ppy.sh/b/")
                            .append(p.getMap().getID()).append(")").append(mods.equals("") ? "" : "**" + mods + "**").append(" [")
                            .append(p.getStarRating()).append("★]\n ")
                            .append(getRank()).append(" **").append(p.getPp()).append("**/").append(p.getPpMax())
                            .append("PP ~ (").append(p.getAcc()).append("%) ~ ")
                            .append(NumberFormat.getNumberInstance(Locale.US).format(s.getScore())).append("\n  [ ")
                            .append(p.getCombo()).append("x/").append(p.getMaxCombo()).append("x ] ~ { ");
                    switch (p.getMode()) {
                        case STANDARD:
                            description.append(s.getHit300()).append(" / ").append(s.getHit100()).append(" / ")
                                    .append(s.getHit50());
                            break;
                        case MANIA:
                            description.append(s.getGekis()).append(" / ").append(s.getHit300()).append(" / ")
                                    .append(s.getKatus()).append(" / ").append(s.getHit100()).append(" / ")
                                    .append(s.getHit50());
                            break;
                        case TAIKO:
                            description.append(s.getHit300()).append(" / ").append(s.getHit100());
                            break;
                        default:
                            break;
                    }
                    description.append(" / ").append(s.getMisses()).append(" } ~ ").append(howLongAgo(s.getDate()));
                }
                eb.setDescription(description);
                break;
            }
            case LEADERBOARD: {
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                String iconURL = "";
                if (scores.size() > 10) {
                    iconURL = "https://a.ppy.sh/" + scores.get(0).getUserID();
                    mb.append("I found ").append(String.valueOf(scores.size())).append(" scores with the " +
                            "specified mods on the specified map's leaderboard, here's the top 10 of them:");
                    scores = scores.stream().limit(10).collect(Collectors.toList());
                } else if (scores.size() == 0) {
                    mb.append("There appear to be no scores on the specified map");
                } else
                    iconURL = "https://a.ppy.sh/" + scores.get(0).getUserID();
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                eb.setThumbnail("attachment://thumb.jpg");
                if (iconURL.isEmpty()) {
                    eb.setAuthor(getKeyString() + " " + p.getMap().getArtist() + " - " + p.getMap().getTitle()
                                    + " [" + p.getMap().getVersion() + "] [" + p.getStarRating() + "★]",
                            "https://osu.ppy.sh/b/" + p.getMap().getID());
                } else {
                    eb.setAuthor(getKeyString() + " " + p.getMap().getArtist() + " - " + p.getMap().getTitle()
                                    + " [" + p.getMap().getVersion() + "] [" + p.getStarRating() + "★]",
                            "https://osu.ppy.sh/b/" + p.getMap().getID(), iconURL);
                }
                String comboDisplay;
                StringBuilder description = new StringBuilder();
                idx = 1;
                for (OsuScore s : scores) {
                    osuscore(s);
                    comboDisplay = " [ " + p.getCombo() + "x/";
                    if (p.getMode() == GameMode.MANIA) {
                        comboDisplay += " " + p.getNMisses() + " miss" + (p.getNMisses() != 1 ? "es" : "") + " ]";
                    } else {
                        comboDisplay += p.getMaxCombo() + "x ]";
                    }
                    if (!description.toString().equals("")) description.append("\n");
                    String modstr = getModString().isEmpty() ? "" : "**" + getModString() + "**";
                    description.append("**").append(idx++).append(".** ").append(getRank()).append(" **[").append(s.getUsername())
                            .append("](https://osu.ppy.sh/u/").append(s.getUsername().replaceAll(" ", "%20")).append(")**: ")
                            .append(NumberFormat.getNumberInstance(Locale.US).format(s.getScore()))
                            .append(comboDisplay).append(modstr).append("\n~  **")
                            .append(p.getPp()).append("**/").append(p.getPpMax()).append("PP")
                            .append(" ~ ").append(p.getAcc()).append("% ~ ").append(howLongAgo(s.getDate()));
                }
                eb.setDescription(description);
                break;
            }
            case COMMONSCORES: {
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (users == null) throw new IllegalStateException(Error.USER.getMsg());
                if (maps == null) throw new IllegalStateException(Error.MAP.getMsg());
                List<String> names = users.stream().map(OsuUser::getUsername).collect(Collectors.toList());
                if (scores.size() > 0) {
                    mb.append("`").append(String.join("`, `", names.subList(0, names.size() - 1))).append("` and `")
                            .append(names.get(names.size() - 1)).append("` have ").append(String.valueOf(scores.size() / names.size()))
                            .append(" common beatmaps in their top 100 scores");
                    if (scores.size() > 15 * names.size())
                        mb.append(", here's the top 15 of them:");
                    else
                        mb.append(":");
                    scores = scores.stream().limit(15 * names.size()).collect(Collectors.toList());
                } else {
                    mb.append("`").append(String.join("`, `", names.subList(0, names.size() - 1))).append("` and `")
                            .append(names.get(names.size() - 1)).append("` have no common scores in their top 100");
                }
                StringBuilder description = new StringBuilder();
                idx = 1;
                for (int i = 0; i < Math.min(scores.size(), 15 * names.size()); i += names.size()) {
                    p.osuscore(scores.get(i));
                    p.map(maps.get(i / names.size()));
                    if (!description.toString().equals("")) description.append("\n");
                    description.append("**").append(idx++).append(".** [").append(p.getMap().getArtist()).append(" - ")
                            .append(p.getMap().getTitle()).append(" [").append(p.getMap().getVersion())
                            .append("]](https://osu.ppy.sh/b/").append(p.getMap().getID()).append(")");
                }

                List<String> urls = users.stream().map(u -> "https://a.ppy.sh/" + u.getID()).collect(Collectors.toList());
                BufferedImage img = utilGeneral.combineImages(urls);
                if (img == null || (thumbFile = FileInteractor.saveImage(img, "avatar" + users.hashCode() + ".png")) == null) {
                    eb.setThumbnail("https://a.ppy.sh/" + users.get(0).getID());
                } else {
                    eb.setThumbnail("attachment://thumb.jpg");
                }
                eb.setDescription(description);
                break;
            }
            case RATIO: {
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (u == null) throw new IllegalStateException(Error.USER.getMsg());
                mb.append("Average ratios of `").append(u.getUsername()).append("`'s top ")
                        .append(String.valueOf(scores.size())).append(" in ").append(p.getMode().getName()).append(":");
                int[] accs = new int[]{0, 90, 95, 97, 99, 100};
                int[] nScores = new int[accs.length];
                int[] nMisses = new int[accs.length];
                int[] nTotal = new int[accs.length];
                int[] nGekis = new int[accs.length];
                int[] n300 = new int[accs.length];
                for (OsuScore s : scores) {
                    double acc = utilOsu.getAcc(s, p.getMode());
                    for (int i = 0; i < accs.length; i++) {
                        if (acc > accs[i] || (acc == 100 && i == accs.length - 1)) {
                            nGekis[i] += s.getGekis();
                            n300[i] += s.getHit300();
                            nTotal[i] += s.getGekis() + s.getHit300() + s.getKatus() + s.getHit100() + s.getHit50() + s.getMisses();
                            nMisses[i] += s.getMisses();
                            nScores[i]++;
                        }
                    }
                }
                eb.setThumbnail("https://a.ppy.sh/" + u.getID());
                eb.setAuthor(u.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getPPRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getRank()) + " "
                                + u.getCountry()
                                + NumberFormat.getNumberInstance(Locale.US).format(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "attachment://thumb.jpg");
                thumbFile = new File(statics.flagPath + u.getCountry() + ".png");
                StringBuilder description = new StringBuilder("__**Acc: #Scores | Ratio | % misses:**__");
                for (int i = 0, iLimit = nScores[nScores.length - 1] == 0 ? accs.length - 1 : accs.length; i < iLimit; i++) {
                    description.append("\n**").append(accs[i] < 100 ? ">" : "").append(accs[i]).append("% :** ").append(nScores[i]).append(" | ")
                            .append(n300[i] == 0 ? nGekis[i] > 0 ? 1 : 0 : (double) (Math.round(100 * (double) nGekis[i] / n300[i])) / 100).append(" | ")
                            .append((double) (Math.round(100 * 100 * (double) nMisses[i] / nTotal[i])) / 100).append("%");
                }
                eb.setDescription(description.toString());
                break;
            }
            case SIMULATE: {
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                mb.append("Simulated score:");
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                eb.setThumbnail("attachment://thumb.jpg");
                osuscore(scores.get(0));
                switch (p.getMode()) {
                    case STANDARD:
                        hitString += p.getN300() + " / " + p.getN100() + " / " + p.getN50();
                        break;
                    case TAIKO:
                        hitString += p.getN300() + " / " + p.getN100();
                    case MANIA:
                        hitString += hitString.equals("{ ") ? p.getNGeki() + " / " + p.getN300() + " / "
                                + p.getNKatu() + " / " + p.getN100() + " / " + p.getN50() : "";
                        break;
                    default:
                        throw new IllegalStateException("GameMode not supported");
                }
                ppString += p.getPp();
                hitString += " / " + p.getNMisses() + " }";
                String mapInfo = "Length: `" + secondsToTimeFormat(p.getMap().getTotalLength()) + "` (`"
                        + secondsToTimeFormat(p.getMap().getHitLength()) + "`) BPM: `" + p.getMap().getBPM() + "` Objects: `"
                        + p.getNObjects() + "`\nCS: `" + p.getMap().getSize() + "` AR: `"
                        + p.getMap().getApproach() + "` OD: `" + p.getMap().getOverall() + "` HP: `"
                        + p.getMap().getDrain() + "` Stars: `" + df.format(p.getMap().getDifficulty()) + "`";
                List<MessageEmbed.Field> fields = new ArrayList<>();
                if (p.getScore() > 0)
                    fields.add(new MessageEmbed.Field("Score", NumberFormat.getNumberInstance(Locale.US).format(p.getScore()), true));
                fields.add(new MessageEmbed.Field("Acc", p.getAcc() + "%", true));
                if (p.getMode() != GameMode.MANIA)
                    fields.add(new MessageEmbed.Field("Combo", "**" + p.getCombo() + "x**/" + p.getMaxCombo() + "x", true));
                fields.add(new MessageEmbed.Field("Hits", hitString, true));
                if (scores.size() == 1) {
                    eb.setTitle(getKeyString() + " " + p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" + p.getMap().getVersion()
                            + "] [" + p.getStarRating() + "★]", "https://osu.ppy.sh/b/" + p.getMap().getID());
                    fields.add(0, new MessageEmbed.Field("Rank", getRank() + getModString(), true));
                    fields.add(3, new MessageEmbed.Field("PP", ppString + "**/" + p.getPpMax() + "PP", true));
                    fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
                } else {
                    eb.setTitle(getKeyString() + " " + p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" + p.getMap().getVersion()
                            + "]", "https://osu.ppy.sh/b/" + p.getMap().getID());
                    fields.add(0, new MessageEmbed.Field("Rank", getRank(), true));
                    StringBuilder description = new StringBuilder();
                    for (OsuScore s : scores) {
                        if (p.getMode() == GameMode.MANIA) {
                            GameMod[] tempMods = s.getEnabledMods();
                            boolean foundMod = false;
                            for (int i = 0; i < s.getEnabledMods().length; i++) {
                                if (tempMods[i] == GameMod.HARD_ROCK || tempMods[i] == GameMod.HIDDEN) {
                                    foundMod = true;
                                    break;
                                }
                            }
                            if (foundMod) continue;
                        }
                        osuscore(s);
                        String ppStr;
                        if (s.getPp() == 0) {
                            try {
                                ppStr = new DecimalFormat("0.00").format(DBProvider.getPpRating(p.getMap().getID(), utilOsu.abbrvModSet(s.getEnabledMods())));
                            } catch (Exception e) {
                                ppStr = p.getPp();
                            }
                        } else {
                            ppStr = p.getPp();
                        }
                        fields.add(new MessageEmbed.Field((getModString().equals("") ? "NM" : getModString().substring(2)) + " (" + p.getStarRating() + "★):",
                                "**" + ppStr + "pp** / " + p.getPpMax() + "PP", true));
                    }
                    fields.add(new MessageEmbed.Field("Map Info", mapInfo, true));
                    eb.setDescription(description.append("\n"));
                }
                for (MessageEmbed.Field f : fields)
                    eb.addField(f);
                break;
            }
            default: throw new IllegalStateException(Error.TYPEM.getMsg());
        }
        mb.setEmbed(eb.build());
        final String hString = hitString;
        final String timeAgo = howLongAgo(date);
        final String eTitle = extendedTitle;
        MessageAction ma = thumbFile != null
                ? channel.sendFile(thumbFile, "thumb.jpg", mb.build())
                : channel.sendMessage(mb.build());
        try {
            switch (typeM) {
                case RECENT:
                case COMPARE:
                case RECENTBEST:
                case SINGLETOP:
                    ma.queue(message -> {
                        try {
                            Thread.sleep(shortFormatDelay);
                            eb.clearFields().setTimestamp(null)
                                    .addField(new MessageEmbed.Field(getRank() + getModString() + "\t" +
                                            NumberFormat.getNumberInstance(Locale.US).format(p.getScore()) + "\t(" +
                                            p.getAcc() + "%)\t" + timeAgo, "**" + p.getPp() +
                                            "**/" + p.getPpMax() + "PP\t[ **" + p.getCombo() + "x**/" +
                                            p.getMaxCombo() + "x ]\t " + hString, false));
                            eb.setTitle(eTitle, "https://osu.ppy.sh/b/" + p.getMap().getID());
                            message.editMessage(eb.build()).queue();
                        } catch (InterruptedException ignored) {
                        }
                    });
                    break;
                case LEADERBOARD:
                case SCORES:
                case TOPSCORES:
                case TOPSOTARKS:
                case SS:
                case RATIO:
                case NOCHOKESCORES:
                case SIMULATE:
                    ma.queue();
                    break;
                case COMMONSCORES:
                    ma.queue(msg -> FileInteractor.deleteImage("avatar" + users.hashCode() + ".png"));
                    break;
                default:
                    throw new IllegalStateException(Error.TYPEM.getMsg());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("Caught error while sending message:");
            e.printStackTrace();
        }
        if (runnable != null) runnable.run();
    }

    public BotMessage user(OsuUser user) {
        this.u = user;
        return this;
    }

    public BotMessage users(List<OsuUser> users) {
        this.users = users;
        return this;
    }

    public BotMessage map(OsuBeatmap map) {
        this.p.map(map);
        this.filesPrepared = FileInteractor.prepareFiles(map);
        return this;
    }

    public BotMessage mode(GameMode mode) {
        if (mode == GameMode.CATCH_THE_BEAT) throw new IllegalStateException(Error.MODE.getMsg());
        this.p.mode(mode);
        return this;
    }

    public BotMessage osuscore(OsuScore score) {
        this.score = score;
        this.p.osuscore(score);
        return this;
    }

    public BotMessage osuscores(List<OsuScore> scores) {
        this.scores = scores;
        return this;
    }

    public BotMessage maps(ArrayList<OsuBeatmap> maps) {
        this.maps = maps;
        return this;
    }

    public BotMessage topplays(Collection<OsuScore> playsT, Collection<OsuScore> playsG) {
        int topPlayIdx = 0;
        int globalPlayIdx = 0;
        if (playsT != null) topPlayIdx = utilOsu.indexInTopPlays(score, playsT);
        if (playsG != null) globalPlayIdx = utilOsu.indexInTopPlays(score, playsG);
        String descriptionStr = "__**";
        if (topPlayIdx > 0) {
            descriptionStr += "Personal Best #" + topPlayIdx;
            if (globalPlayIdx > 0)
                descriptionStr += " and ";
        }
        if (globalPlayIdx > 0)
            descriptionStr += "Global Top #" + globalPlayIdx;
        this.topplays = descriptionStr.equals("__**") ? "" : descriptionStr + "!**__";
        return this;
    }

    public BotMessage history(Collection<OsuScore> history) {
        int mapID = score.getBeatmapID();
        for (OsuScore game : history) {
            if (game.getBeatmapID() == mapID) {
                if (game.getScore() > 10000)
                    retries++;
            } else
                break;
        }
        return this;
    }

    public BotMessage author(User author) {
        this.author = author;
        return this;
    }

    private String getRank() {
        String scoreRank = p.getRank();
        return Main.jda.getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(scoreRank).getValue()).getAsMention()
            + (scoreRank.equals("F") ? " (" + p.getCompletion() + "%)" : "");
    }

    private String getModString() {
        String out = abbrvModSet(score.getEnabledMods());
        if (!out.equals(""))
            out = " +" + out;
        return out;
    }

    private String getKeyString() {
        if (p.getMode() != GameMode.MANIA) return "";
        return "[" + (int)p.getMap().getSize() + "K" + "]";
    }

    private enum Error {
        MAP("Unspecified map"),
        HISTORY("Unspecified history"),
        TYPEM("Invalid message type"),
        MODE("Unsupported game mode"),
        COLLECTION("Collection is undefined"),
        USER("User is undefined"),
        AUTHOR("Author is undefined");
        String msg;
        Error(String msg) {
            this.msg = msg;
        }
        String getMsg() {
            return msg;
        }
    }

    public enum MessageType {
        RECENT,
        COMPARE,
        RECENTBEST,
        SCORES,
        SINGLETOP,
        TOPSCORES,
        NOCHOKESCORES,
        TOPSOTARKS,
        SS,
        LEADERBOARD,
        COMMONSCORES,
        RATIO,
        SIMULATE
    }
}
