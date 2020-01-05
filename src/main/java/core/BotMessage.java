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
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static main.java.util.utilGeneral.howLongAgo;
import static main.java.util.utilGeneral.secondsToTimeFormat;

/*
    Creates more complex messages like embeds
 */
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
    private LinkedList<Integer> indices;
    private List<OsuBeatmap> maps;

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

    public void buildAndSend(@NotNull Runnable runnable) {
        buildAndSend();
        runnable.run();
    }

    // All data is set, time to create the message (absolutely disgusting function and I'm sorry for that)
    public void buildAndSend() {
        File thumbFile = null;  // File for either author image or thumbnail image
        String hitString = "{ ", extendedTitle = "";
        TemporalAccessor timestamp;
        ZonedDateTime date = ZonedDateTime.now();
        // ------ BUILDING THE MESSAGE ------
        switch (typeM) {
            // Naturally depends on message type
            // Absence of "break;" enables nice statement reusability between message types
            case RECENT:
                if (retries == 0) throw new IllegalStateException(Error.HISTORY.getMsg());
                mb.append("Try #").append(String.valueOf(retries));
            case COMPARE:
            case RECENTBEST:
            case SINGLETOP: {
                // Map needs to be set beforehand
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                eb.setThumbnail("attachment://thumb.jpg");
                eb.setAuthor(u.getUsername() + ": "
                                + formatNumber(u.getPPRaw()) + "pp (#"
                                + formatNumber(u.getRank()) + " "
                                + u.getCountry()
                                + formatNumber(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "https://a.ppy.sh/" + u.getID());
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                date = score.getDate();
                timestamp = date.toInstant();
                switch (p.getMode()) {
                    case CATCH_THE_BEAT:
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
                }
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
                    .addField("Rank", getGradeFull() + getModString(),true)
                    .addField("Score", formatNumber(p.getScore()),true)
                    .addField("Acc", p.getAcc() + "%",true)
                    .addField("PP", "**" + p.getPp() + "**/" + p.getPpMax() + "PP",true)
                    .addField("Combo", "**" + p.getCombo() + "x**/" + p.getMaxCombo() + "x",true)
                    .addField("Hits", hitString,true)
                    .addField("Map Info", mapInfo,true);
                break;
            }
            case SCORES: {
                // Map and scores need to be set beforehand
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                eb.setThumbnail("attachment://thumb.jpg");
                eb.setAuthor(u.getUsername() + ": "
                                + formatNumber(u.getPPRaw()) + "pp (#"
                                + formatNumber(u.getRank()) + " "
                                + u.getCountry()
                                + formatNumber(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "https://a.ppy.sh/" + u.getID());
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                eb.setTitle(p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" + p.getMap().getVersion()
                        + "]", "https://osu.ppy.sh/b/" + p.getMap().getID());
                List<OsuScore> orderedScores = new ArrayList<>(scores);
                orderedScores.sort(Comparator.comparing(OsuScore::getPp).reversed());
                int idx = 1;
                for (OsuScore s : orderedScores) {
                    osuscore(s);
                    String fieldName = "**" + idx++ + ".** " + getGradeFull() + getModString() + "\t[" + p.getStarRating() + "★]\t" +
                            formatNumber(s.getScore()) + "\t(" + p.getAcc() + "%)";
                    if (p.getMode() == GameMode.MANIA) fieldName += "\t" + getKeyString();
                    String fieldValue = "**" + p.getPp() + "**/" + p.getPpMax() + "PP\t[ **"
                            + s.getMaxCombo() + "x**/" + p.getMaxCombo() + "x ]\t {";
                    switch (p.getMode()) {
                        case CATCH_THE_BEAT:
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
                    }
                    fieldValue += "/" + s.getMisses() + "}\t" + howLongAgo(s.getDate());
                    eb.addField(fieldName, fieldValue, false);
                }
                break;
            }
            case NOCHOKESCORES:
                // Author needs to be set beforehand
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
            case RECENTBESTS:
                if (mb.isEmpty()) {
                    mb.append("Here are the ").append(String.valueOf(scores.size())).append(" most recent scores in `")
                            .append(u.getUsername()).append("`'s top 100");
                }
            case TOPSCORES: {
                // Maps and scores need to be set beforehand
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (maps == null) throw new IllegalStateException(Error.MAP.getMsg());
                if (mb.isEmpty() && scores.size() > 5) {
                    mb.append("I found ").append(String.valueOf(scores.size())).append(" scores with the specified properties in `")
                            .append(u.getUsername()).append("`'s top 100, here's the top 5 of them:");
                    scores = scores.stream().limit(5).collect(Collectors.toList());
                }
                eb.setThumbnail("https://a.ppy.sh/" + u.getID());
                eb.setAuthor(u.getUsername() + ": "
                                + formatNumber(u.getPPRaw()) + "pp (#"
                                + formatNumber(u.getRank()) + " "
                                + u.getCountry()
                                + formatNumber(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "attachment://thumb.jpg");
                thumbFile = new File(statics.flagPath + u.getCountry() + ".png");
                String mods;
                StringBuilder description = new StringBuilder();
                int mapIdx = 0;
                for (OsuScore s : scores) {
                    int scoreIdx = nextIndex();
                    map(maps.get(mapIdx++));
                    osuscore(s);
                    mods = getModString();
                    String ppMax = u.getMode() == GameMode.CATCH_THE_BEAT ? "0" : p.getPpMax();
                    if (!description.toString().equals("")) description.append("\n");
                    if (typeM == MessageType.NOCHOKESCORES) p.noChoke(50);
                    description.append("**").append(scoreIdx).append(".** [**")
                            .append(p.getMap().getTitle()).append(" [").append(p.getMap().getVersion()).append("]**](https://osu.ppy.sh/b/")
                            .append(p.getMap().getID()).append(")").append(mods.equals("") ? "" : "**" + mods + "**").append(" [")
                            .append(p.getStarRating()).append("★]\n ")
                            .append(getGradeFull()).append(" **").append(p.getPp()).append("**/").append(ppMax)
                            .append("PP ~ (").append(p.getAcc()).append("%) ~ ")
                            .append(formatNumber(s.getScore())).append("\n  [ ")
                            .append(p.getCombo()).append("x/").append(p.getMaxCombo()).append("x ] ~ { ");
                    switch (p.getMode()) {
                        case STANDARD:
                        case CATCH_THE_BEAT:
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
                    }
                    description.append(" / ").append(s.getMisses()).append(" } ~ ").append(howLongAgo(s.getDate()));
                }
                eb.setDescription(description);
                break;
            }
            case LEADERBOARD: {
                // Map, author and scores need to be set beforehand
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                if (author == null) throw new IllegalStateException(Error.AUTHOR.getMsg());
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
                String authorName = Main.discLink.getOsu(author.getId());
                if (authorName != null)
                    authorName = authorName.toLowerCase();
                String comboDisplay;
                StringBuilder description = new StringBuilder();
                int idx = 1;
                for (OsuScore s : scores) {
                    osuscore(s);
                    comboDisplay = " [ " + p.getCombo() + "x/";
                    comboDisplay += p.getMode() == GameMode.MANIA
                            ? (" " + p.getNMisses() + " miss" + (p.getNMisses() != 1 ? "es" : "") + " ]")
                            : (p.getMaxCombo() + "x ]");
                    if (!description.toString().equals("")) description.append("\n");
                    String modstr = getModString().isEmpty() ? "" : "**" + getModString() + "**";
                    description.append("**").append(idx++).append(".** ").append(getGradeFull()).append(" **");
                    if (authorName != null && s.getUsername().toLowerCase().equals(authorName))
                        description.append("__");
                    description.append("[").append(s.getUsername()).append("](https://osu.ppy.sh/u/")
                            .append(s.getUsername().replaceAll(" ", "%20")).append(")");
                    if (authorName != null && s.getUsername().toLowerCase().equals(authorName))
                        description.append("__");
                    description.append("**: ")
                            .append(formatNumber(s.getScore()))
                            .append(comboDisplay).append(modstr).append("\n~  **")
                            .append(p.getPp()).append("**/").append(p.getPpMax()).append("PP")
                            .append(" ~ ").append(p.getAcc()).append("% ~ ").append(howLongAgo(s.getDate()));
                }
                eb.setDescription(description);
                break;
            }
            case COMMONSCORES: {
                // Maps, users, and scores need to be set beforehand
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
                int idx = 1;
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
                // User and scores need to be set beforehand
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                if (u == null) throw new IllegalStateException(Error.USER.getMsg());
                mb.append("Average ratios of `").append(u.getUsername()).append("`'s top ")
                        .append(String.valueOf(scores.size())).append(" in ").append(p.getMode().getName()).append(":");
                int[] accs = new int[] {0, 90, 95, 97, 99, 100};
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
                                + formatNumber(u.getPPRaw()) + "pp (#"
                                + formatNumber(u.getRank()) + " "
                                + u.getCountry()
                                + formatNumber(u.getCountryRank()) + ")",
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
                // Map needs to be set beforehand
                if (p.getMap() == null) throw new IllegalStateException(Error.MAP.getMsg());
                mb.append("Simulated score:");
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + p.getMap().getBeatmapSetID() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                eb.setThumbnail("attachment://thumb.jpg");
                switch (p.getMode()) {
                    case CATCH_THE_BEAT:
                    case STANDARD:
                        hitString += p.getN300() + " / " + p.getN100() + " / " + p.getN50();
                        break;
                    case TAIKO:
                        hitString += p.getN300() + " / " + p.getN100();
                    case MANIA:
                        hitString += hitString.equals("{ ") ? p.getNGeki() + " / " + p.getN300() + " / "
                                + p.getNKatu() + " / " + p.getN100() + " / " + p.getN50() : "";
                        break;
                }
                hitString += " / " + p.getNMisses() + " }";
                String mapInfo = "Length: `" + secondsToTimeFormat(p.getMap().getTotalLength()) + "` (`"
                        + secondsToTimeFormat(p.getMap().getHitLength()) + "`) BPM: `" + p.getMap().getBPM() + "` Objects: `"
                        + p.getNObjects() + "`\nCS: `" + p.getMap().getSize() + "` AR: `"
                        + p.getMap().getApproach() + "` OD: `" + p.getMap().getOverall() + "` HP: `"
                        + p.getMap().getDrain() + "` Stars: `" + df.format(p.getMap().getDifficulty()) + "`";
                List<MessageEmbed.Field> fields = new ArrayList<>();
                if (p.getScore() > 0)
                    fields.add(new MessageEmbed.Field("Score", formatNumber(p.getScore()), true));
                fields.add(new MessageEmbed.Field("Acc", p.getAcc() + "%", true));
                if (p.getMode() != GameMode.MANIA)
                    fields.add(new MessageEmbed.Field("Combo", "**" + p.getCombo() + "x**/" + p.getMaxCombo() + "x", true));
                fields.add(new MessageEmbed.Field("Hits", hitString, true));
                eb.setTitle(getKeyString() + " " + p.getMap().getArtist() + " - " + p.getMap().getTitle() + " [" + p.getMap().getVersion()
                        + "] [" + p.getStarRating() + "★]", "https://osu.ppy.sh/b/" + p.getMap().getID());
                fields.add(0, new MessageEmbed.Field("Rank", getGradeFull() + getModString(), true));
                fields.add(3, new MessageEmbed.Field("PP", "**" + p.getPp() + "**/" + p.getPpMax() + "PP", true));
                fields.add(new MessageEmbed.Field("Map Info", mapInfo, false));
                for (MessageEmbed.Field f : fields)
                    eb.addField(f);
                break;
            }
            case PROFILE:
                // User and scores need to be set beforehand
                if (u == null) throw new IllegalStateException(Error.USER.getMsg());
                if (scores == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                eb.setThumbnail("https://a.ppy.sh/" + u.getID())
                        .setAuthor(u.getUsername() + ": "
                                + formatNumber(u.getPPRaw()) + "pp (#"
                                + formatNumber(u.getRank()) + " "
                                + u.getCountry()
                                + formatNumber(u.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + u.getID(), "attachment://thumb.jpg")
                        .setFooter("Joined osu! " + DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' hh:mm a").format(u.getJoinedDate()) + " (" + howLongAgo(u.getJoinedDate()) + ")", null);
                thumbFile = new File(statics.flagPath + u.getCountry() + ".png");
                // Calculate all interesting values
                double totalAcc = 0, minAcc = 100, maxAcc = 0;
                double totalPp = 0, minPp = scores.get(scores.size() - 1).getPp(), maxPp = scores.get(0).getPp();
                double totalCombo = 0;
                int minCombo = Integer.MAX_VALUE, maxCombo = 0;
                double factor = 1;
                HashMap<Integer, Integer> amountModsIncluded = new HashMap<>();
                HashMap<Integer, Double> ppModsIncluded = new HashMap<>();
                HashMap<Integer, Integer> amountModsExact = new HashMap<>();
                HashMap<Integer, Double> ppModsExact = new HashMap<>();
                boolean multiMods = false;
                for (OsuScore s : scores) {
                    double acc = utilOsu.getAcc(s, u.getMode());
                    totalAcc += acc;
                    if (acc < minAcc) minAcc = acc;
                    if (acc > maxAcc) maxAcc = acc;
                    totalCombo += s.getMaxCombo();
                    if (s.getMaxCombo() < minCombo)
                        minCombo = s.getMaxCombo();
                    if (s.getMaxCombo() > maxCombo)
                        maxCombo = s.getMaxCombo();
                    totalPp += s.getPp();
                    double weightedScorePp = factor * s.getPp();
                    factor *= 0.95;
                    int modBits = utilOsu.mods_arrToInt(s.getEnabledMods());
                    amountModsExact.compute(modBits, (k, v) -> v == null ? 1 : v + 1);
                    ppModsExact.compute(modBits, (k, v) -> v == null ? weightedScorePp : v + weightedScorePp);
                    if (modBits == 0) {
                        amountModsIncluded.compute(0, (k, v) -> v == null ? 1 : v + 1);
                        ppModsIncluded.compute(0, (k, v) -> v == null ? weightedScorePp : v + weightedScorePp);
                    } else {
                        boolean skipDT = false, skipSD = false;
                        int modAmount = 0;
                        for (int i = 0; i < s.getEnabledMods().length; i++) {
                            GameMod mod = s.getEnabledMods()[i];
                            if (mod == GameMod.NIGHTCORE) skipDT = true;
                            else if (mod == GameMod.PERFECT) skipSD = true;
                            else if (mod == GameMod.DOUBLE_TIME && skipDT) continue;
                            else if (mod == GameMod.SUDDEN_DEATH && skipSD) continue;
                            amountModsIncluded.compute((int)mod.getBit(), (k, v) -> v == null ? 1 : v + 1);
                            ppModsIncluded.compute((int)mod.getBit(), (k, v) -> v == null ? weightedScorePp : v + weightedScorePp);
                            modAmount++;
                        }
                        if (modAmount > 1) multiMods = true;
                    }
                }
                double bonusPp = 416.6667 * (1 - Math.pow(0.9994, (u.getCountRankSSH() + u.getCountRankSS() + u.getCountRankSH() + u.getCountRankS() + u.getCountRankA())));
                String amountModsIncludedString = amountModsIncluded.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .map(e -> "`" + utilOsu.mods_intToStr(e.getKey()) + " " + e.getValue() + "%`")
                        .collect(Collectors.joining(" > "));
                String ppModsIncludedString = ppModsIncluded.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .map(e -> "`" + utilOsu.mods_intToStr(e.getKey()) + " " + df.format(e.getValue()) + "pp`")
                        .collect(Collectors.joining(" > "));
                String amountModsExactString = "", ppModsExactString = "";
                if (multiMods) {
                    amountModsExactString = amountModsExact.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .map(e -> "`" + utilOsu.mods_intToStr(e.getKey()) + " " + e.getValue() + "%`")
                            .collect(Collectors.joining(" > "));
                    ppModsExactString = ppModsExact.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .map(e -> "`" + utilOsu.mods_intToStr(e.getKey()) + " " + df.format(e.getValue()) + "pp`")
                            .collect(Collectors.joining(" > "));
                }
                // Prepare all fields
                eb.addField("Ranked score:", formatNumber(u.getRankedScore()), true)
                        .addField("Total score:", formatNumber(u.getTotalScore()), true)
                        .addField("Total hits:", formatNumber(u.getTotalHits()), true)
                        .addField("Play count / time:", formatNumber(u.getPlayCount()) + " / " + (u.getPlayTimeSeconds() / 3600) + " hrs", true)
                        .addField("Level:", df.format(u.getLevel()), true)
                        .addField("Bonus PP:", "~" + df.format(bonusPp) + "pp", true)
                        .addField("Accuracy:", df.format(u.getAccuracy()) + "%", true)
                        .addField("Unweighted accuracy:", df.format(totalAcc / 100) + "% [" + minAcc + "% - " + maxAcc + "%]", true)
                        .addField("Grades:",
                                getGradeEmote("XH") + u.getCountRankSSH()
                                + " " + getGradeEmote("X") + u.getCountRankSS()
                                + " " + getGradeEmote("SH") + u.getCountRankSH()
                                + " " + getGradeEmote("S") + u.getCountRankS()
                                + " " + getGradeEmote("A") + u.getCountRankA()
                                , false)
                        .addField("Average PP:", df.format(totalPp / 100) + "pp [" + df.format(minPp) + " - " + df.format(maxPp) + "]", true)
                        .addField("Average Combo:", df.format(totalCombo / 100) + " [" + minCombo + " - " + maxCombo  + "]", true);
                if (multiMods)
                    eb.addField("Favourite mod combinations:", amountModsExactString, false);
                eb.addField("Favourite mods:", amountModsIncludedString, false);
                if (multiMods)
                    eb.addField("PP earned with mod combination:", ppModsExactString, false);
                eb.addField("PP earned with mod:", ppModsIncludedString, false);
                break;
            default: throw new IllegalStateException(Error.TYPEM.getMsg());
        }
        mb.setEmbed(eb.build());
        final String hString = hitString;
        final String timeAgo = howLongAgo(date);
        final String eTitle = extendedTitle;
        MessageAction ma = thumbFile != null
                ? channel.sendFile(thumbFile, "thumb.jpg", mb.build())
                : channel.sendMessage(mb.build());
        // ------ SENDING THE MESSAGE ------
        try {
            switch (typeM) {
                case RECENT:
                case COMPARE:
                case RECENTBEST:
                case SINGLETOP:
                    // Send and later minimize the message
                    ma.queue(message -> {
                        try {
                            Thread.sleep(shortFormatDelay);
                            eb.clearFields().setTimestamp(null)
                                    .addField(new MessageEmbed.Field(getGradeFull() + getModString() + "\t" +
                                            formatNumber(p.getScore()) + "\t(" +
                                            p.getAcc() + "%)\t" + timeAgo, "**" + p.getPp() +
                                            "**/" + p.getPpMax() + "PP\t[ **" + p.getCombo() + "x**/" +
                                            p.getMaxCombo() + "x ]\t " + hString, false));
                            eb.setTitle(eTitle, "https://osu.ppy.sh/b/" + p.getMap().getID());
                            message.editMessage(eb.build()).queue();
                        } catch (InterruptedException ignored) { }
                    });
                    break;
                case SIMULATE:
                    // Send and later minimize the message a little differently than before
                    ma.queue(message -> {
                        try {
                            Thread.sleep(shortFormatDelay);
                            eb.clearFields().setTimestamp(null)
                                    .addField(new MessageEmbed.Field(getGradeFull() + getModString() + (p.getScore() > 0 ? "\t" +
                                            formatNumber(p.getScore()) : "") + "\t(" +
                                            p.getAcc() + "%)", "**" + p.getPp() +
                                            "**/" + p.getPpMax() + "PP\t[ **" + p.getCombo() + "x**/" +
                                            p.getMaxCombo() + "x ]\t " + hString, false));
                            message.editMessage(eb.build()).queue();
                        } catch (InterruptedException ignored) { }
                    });
                    break;
                case LEADERBOARD:
                case SCORES:
                case RECENTBESTS:
                case TOPSCORES:
                case TOPSOTARKS:
                case SS:
                case RATIO:
                case NOCHOKESCORES:
                case PROFILE:
                    // Just send
                    ma.queue();
                    break;
                case COMMONSCORES:
                    // Remove the temporarily created osu pfp combination image again
                    ma.queue(msg -> FileInteractor.deleteImage("avatar" + users.hashCode() + ".png"));
                    break;
                default:
                    throw new IllegalStateException(Error.TYPEM.getMsg());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error("Caught error while sending message:", e);
        }
    }

    // Set a single user for the message
    public BotMessage user(OsuUser user) {
        this.u = user;
        return this;
    }

    // Set multiple users for the message
    public BotMessage users(List<OsuUser> users) {
        this.users = users;
        return this;
    }

    // Set a single map for the message
    public BotMessage map(OsuBeatmap map) {
        this.p.map(map);
        this.filesPrepared = FileInteractor.prepareFiles(map);
        return this;
    }

    // Set mulitple maps for the message
    public BotMessage maps(List<OsuBeatmap> maps) {
        this.maps = maps;
        return this;
    }

    // Set the game mode
    public BotMessage mode(GameMode mode) {
        this.p.mode(mode);
        return this;
    }

    // Set a single score for the message
    public BotMessage osuscore(OsuScore score) {
        this.score = score;
        this.p.osuscore(score);
        return this;
    }

    // Set multiple scores for the message
    public BotMessage osuscores(List<OsuScore> scores) {
        this.scores = scores;
        return this;
    }

    // Check whether the score is in the map's global leaderboard or in the user's personal best
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

    // Calculate the retry count via recent-history of user
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

    // Set the author of the event
    public BotMessage author(User author) {
        this.author = author;
        return this;
    }

    // Set indices of scores
    public BotMessage indices(LinkedList<Integer> indices) {
        this.indices = indices;
        return this;
    }

    // In case of an enumeration, this function returns the index of the next element
    private Integer nextIndex() {
        if (this.indices == null || this.indices.isEmpty()) {
            this.indices = IntStream.range(1, 16).boxed().collect(Collectors.toCollection(LinkedList::new));
        }
        return this.indices.pollFirst();
    }

    // Return the grade of the score as emote and the completion % if the score is failed
    private String getGradeFull() {
        String scoreGrade = p.getGrade();
        return getGradeEmote(scoreGrade) + (scoreGrade.equals("F") ? " (" + p.getCompletion() + "%)" : "");
    }

    // Return the grade of the score as emote
    private String getGradeEmote(String grade) {
        return Main.jda.getGuildById(secrets.devGuildID).getEmoteById(utilOsu.getRankEmote(grade).getValue()).getAsMention();
    }

    // Return a formated string for the mod combination
    private String getModString() {
        String out = utilOsu.mods_arrToStr(score.getEnabledMods());
        if (!out.equals("NM"))
            out = " +" + out;
        return out;
    }

    // Return a string containing key info for mania maps
    private String getKeyString() {
        if (p.getMode() != GameMode.MANIA) return "";
        return "[" + (int)p.getMap().getSize() + "K" + "]";
    }

    // Auxiliary function to return a string of a number in a representable fashion
    private String formatNumber(double number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    private enum Error {
        MAP("Unspecified map"),
        HISTORY("Unspecified history"),
        TYPEM("Invalid message type"),
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
        RECENTBESTS,
        SCORES,
        SINGLETOP,
        TOPSCORES,
        NOCHOKESCORES,
        TOPSOTARKS,
        SS,
        LEADERBOARD,
        COMMONSCORES,
        RATIO,
        SIMULATE,
        PROFILE,
    }
}
