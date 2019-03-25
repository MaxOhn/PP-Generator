package main.java.core;

import de.maxikg.osuapi.model.*;
import main.java.util.secrets;
import main.java.util.utilOsu;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.List;

import static de.maxikg.osuapi.model.Mod.createSum;
import static main.java.util.utilGeneral.howLongAgo;
import static main.java.util.utilGeneral.secondsToTimeFormat;
import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.key_mods_str;

public class BotMessage {

    private MessageReceivedEvent event;
    private MessageType typeM;
    private EmbedBuilder eb;
    private MessageBuilder mb;
    private Performance p;

    private User u;
    private Beatmap m;
    private GameMode mode;
    private ScoreType typeS;
    private BeatmapScore bs;
    private UserScore us;
    private UserGame ug;
    private Collection<BeatmapScore> bsc;
    private Collection<UserScore> usc;
    private Collection<UserGame> ugc;
    private ArrayList<Beatmap> maps;

    private String topplays;
    private int retries;

    private boolean filesPrepared;

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final int shortFormatDelay = 45000;


    public BotMessage(MessageReceivedEvent event, MessageType typeM) {
        this.event = event;
        this.typeM = typeM;
        this.eb = new EmbedBuilder().setColor(Color.green);
        this.mb = new MessageBuilder();
        this.p = new Performance();
    }

    public void send(String msg) {
        if (typeM != MessageType.TEXT) throw new IllegalStateException(Error.TYPEM.getMsg());
        event.getTextChannel().sendMessage(msg).queue();
    }

    public void buildAndSend() {
        if (u == null) throw new IllegalStateException(Error.USER.getMsg());
        eb.setThumbnail("attachment://thumb.jpg");
        eb.setAuthor(u.getUsername() + ": "
                + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRaw()) + "pp (#"
                + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRank()) + " "
                + u.getCountry().toString().toUpperCase()
                + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRankCountry()) + ")",
        "https://osu.ppy.sh/u/" + u.getUserId(), "https://a.ppy.sh/" + u.getUserId());
        File thumbFile;
        int idx;
        String ppString = "**", hitString = "{ ", extendedTitle = "";
        TemporalAccessor timestamp;
        Date date = new Date();
        switch (typeM) {
            case RECENT:
                if (retries == 0) throw new IllegalStateException(Error.HISTORY.getMsg());
                mb.append("Try #").append(String.valueOf(retries));
            case COMPARE:
            case RECENTBEST:
            case SINGLETOP:
                if (m == null) throw new IllegalStateException(Error.MAP.getMsg());
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + m.getBeatmapSetId() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                switch (typeS) {
                    case BEATMAPSCORE:
                        date = bs.getDate();
                        timestamp = date.toInstant();
                        break;
                    case USERSCORE:
                        date = us.getDate();
                        timestamp = date.toInstant();
                        break;
                    case USERGAME:
                        date = ug.getDate();
                        timestamp = date.toInstant();
                        break;
                    default: throw new IllegalStateException(Error.TYPES.getMsg());
                }
                switch (mode) {
                    case STANDARD:
                        hitString += p.getN300() + " / " + p.getN100() + " / " + p.getN50();
                        extendedTitle = m.getArtist() + " - " + m.getTitle() + " [" +
                                m.getVersion() + "]" + " [" + p.getStarRating() + "★]";
                        break;
                    case TAIKO:
                        hitString += p.getN300() + " / " + p.getN100();
                        extendedTitle = m.getArtist() + " - " + m.getTitle() + " [" +
                                m.getVersion() + "]" + " [" + p.getStarRating() + "★]";
                    case OSU_MANIA:
                        hitString += hitString.equals("{ ") ? p.getNGeki() + " / " + p.getN300() + " / "
                                + p.getNKatu() + " / " + p.getN100() + " / " + p.getN50() : "";
                        extendedTitle += extendedTitle.equals("") ? getKeyString() + " " + m.getArtist() + " - "
                                + m.getTitle() + " [" + m.getVersion() + "]" + " [" + p.getStarRating() + "★]" : "";
                        break;
                    default: throw new IllegalStateException("GameMode not supported");
                }
                ppString += p.getPp();
                hitString += " / " + p.getNMisses() + " }";
                String mapInfo = "Length: `" + secondsToTimeFormat(m.getTotalLength()) + "` (`"
                        + secondsToTimeFormat(m.getHitLength()) + "`) BPM: `" + m.getBpm() + "` Objects: `"
                        + p.getNObjects() + "`\nCS: `" + m.getDifficultySize() + "` AR: `"
                        + m.getDifficultyApproach() + "` OD: `" + m.getDifficultyOverall() + "` HP: `"
                        + m.getDifficultyDrain() + "` Stars: `" + df.format(m.getDifficultyRating()) + "`";
                eb.setTimestamp(timestamp)
                    .setDescription(topplays)
                    .setTitle(getKeyString() + " " + m.getArtist() + " - " + m.getTitle() + " [" + m.getVersion()
                                    + "]","https://osu.ppy.sh/b/" + m.getBeatmapId())
                    .addField("Rank", getRank() + getModString(),true)
                    .addField("Score", NumberFormat.getNumberInstance(Locale.US).format(p.getScore()),true)
                    .addField("Acc", p.getAcc() + "%",true)
                    .addField("PP", ppString + "**/" + p.getPpMax() + "PP",true)
                    .addField("Combo", p.getCombo() + "x/" + p.getMaxCombo() + "x",true)
                    .addField("Hits", hitString,true)
                    .addField("Map Info", mapInfo,true);
                break;
            case SCORES:
                if (m == null) throw new IllegalStateException(Error.MAP.getMsg());
                if (bsc == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                thumbFile = filesPrepared
                        ? new File(secrets.thumbPath + m.getBeatmapSetId() + "l.jpg")
                        : new File(secrets.thumbPath + "bgNotFound.png");
                eb.setTitle(getKeyString() + " " + m.getArtist() + " - " + m.getTitle() + " [" + m.getVersion()
                                + "]","https://osu.ppy.sh/b/" + m.getBeatmapId());
                List<BeatmapScore> orderedScores = new ArrayList<>(bsc);
                orderedScores.sort(Comparator.comparing(BeatmapScore::getPp).reversed());
                idx = 1;
                for (BeatmapScore s : orderedScores) {
                    beatmapscore(s);
                    String fieldName = "**" + idx++ + ".** " + getRank() + getModString() + "\t[" + p.getStarRating() + "★]\t" +
                            NumberFormat.getNumberInstance(Locale.US).format(s.getScore()) + "\t(" + p.getAcc() + "%)";
                    String fieldValue = "**" + df.format(s.getPp()) + "**/" + p.getPpMax() + "PP\t[ "
                            + s.getMaxCombo() + "x/" + p.getMaxCombo() + "x ]\t {";
                    switch (mode) {
                        case STANDARD: fieldValue += s.getCount300() + "/" + s.getCount100() + "/" + s.getCount50(); break;
                        case TAIKO: fieldValue +=  s.getCount300() + "/" + s.getCount100(); break;
                        case OSU_MANIA:
                            fieldValue += s.getCountGeki() + "/" + s.getCount300() + "/" + s.getCountKatu()
                                    + "/" + s.getCount100() + "/" + s.getCount50();
                            break;
                        default: throw new IllegalStateException(Error.MODE.getMsg());
                    }
                    fieldValue += "/" + s.getCountMiss() + "}\t" + howLongAgo(s.getDate());
                    eb.addField(fieldName, fieldValue, false);
                }
                break;
            case TOPSCORES:
                if (usc == null || maps == null) throw new IllegalStateException(Error.COLLECTION.getMsg());
                eb.setThumbnail("https://a.ppy.sh/" + u.getUserId());
                eb.setAuthor(u.getUsername() + ": "
                            + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRaw()) + "pp (#"
                            + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRank()) + " "
                            + u.getCountry().toString().toUpperCase()
                            + NumberFormat.getNumberInstance(Locale.US).format(u.getPpRankCountry()) + ")",
                    "https://osu.ppy.sh/u/" + u.getUserId(), "attachment://thumb.jpg");
                thumbFile = new File(secrets.flagPath + u.getCountry().toString().toUpperCase() + ".png");
                String mods;
                StringBuilder description = new StringBuilder();
                idx = 1;
                for (UserScore s : usc) {
                    map(maps.get(idx - 1));
                    userscore(s);
                    mods = getModString();
                    if (!description.toString().equals("")) description.append("\n");
                    p.userscore(s).mode(mode);
                    description.append("**").append(idx++).append(".** [**")
                            .append(m.getTitle()).append("[").append(m.getVersion()).append("]**](https://osu.ppy.sh/b/")
                            .append(m.getBeatmapId()).append(")").append(mods.equals("") ? "" : "**" + mods + "**").append(" [")
                            .append(p.getStarRating()).append("★]\n ")
                            .append(getRank()).append(" **").append(p.getPp()).append("**/").append(p.getPpMax())
                            .append("PP ~ (").append(p.getAcc()).append("%) ~ ")
                            .append(NumberFormat.getNumberInstance(Locale.US).format(s.getScore())).append("\n  [ ")
                            .append(p.getCombo()).append("x/").append(p.getMaxCombo()).append("x ] ~ { ");
                    switch (mode) {
                        case STANDARD:
                            description.append(s.getCount300()).append(" / ").append(s.getCount100()).append(" / ")
                                    .append(s.getCount50());
                            break;
                        case OSU_MANIA:
                            description.append(s.getCountGeki()).append(" / ").append(s.getCount300()).append(" / ")
                                    .append(s.getCountKatu()).append(" / ").append(s.getCount100()).append(" / ")
                                    .append(s.getCount50());
                            break;
                        case TAIKO:
                            description.append(s.getCount300()).append(" / ").append(s.getCount100());
                            break;
                        default: break;
                    }
                    description.append(" / ").append(s.getCountMiss()).append(" } ~ ").append(howLongAgo(s.getDate()));
                }
                eb.setDescription(description);
                break;
            default: throw new IllegalStateException(Error.TYPEM.getMsg());
        }
        mb.setEmbed(eb.build());
        final String hString = hitString;
        final String timeAgo = howLongAgo(date);
        final String eTitle = extendedTitle;
        MessageAction ma = this.event.getTextChannel().sendFile(thumbFile, "thumb.jpg", mb.build());
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
                                        "**/" + p.getPpMax() + "PP\t[ " + p.getCombo() + "x/" +
                                        p.getMaxCombo() + "x ]\t " + hString, false));
                        eb.setTitle(eTitle, "https://osu.ppy.sh/b/" + m.getBeatmapId());
                        message.editMessage(eb.build()).queue();
                    } catch (InterruptedException ignored) { }
                });
                break;
            case SCORES: ma.queue(); break;
            case TOPSCORES: ma.queue(); break;
            default: throw new IllegalStateException(Error.TYPEM.getMsg());
        }
    }

    public BotMessage user(User user) {
        this.u = user;
        return this;
    }

    public BotMessage map(Beatmap map) {
        this.m = map;
        this.p.map(map);
        this.filesPrepared = Main.fileInteractor.prepareFiles(map);
        this.mode = map.getMode();
        return this;
    }

    public BotMessage mode(GameMode mode) {
        if (mode == GameMode.CTB) throw new IllegalStateException(Error.MODE.getMsg());
        this.mode = mode;
        this.p.mode(mode);
        return this;
    }

    public BotMessage beatmapscore(BeatmapScore score) {
        this.bs = score;
        this.typeS = ScoreType.BEATMAPSCORE;
        this.p.beatmapscore(score);
        return this;
    }

    public BotMessage beatmapscore(Collection<BeatmapScore> scores) {
        this.bsc = scores;
        this.typeS = ScoreType.BEATMAPSCORE;
        return this;
    }

    public BotMessage userscore(UserScore score) {
        this.us = score;
        this.typeS = ScoreType.USERSCORE;
        this.p.userscore(score);
        return this;
    }

    public BotMessage userscore(Collection<UserScore> scores) {
        this.usc = scores;
        this.typeS = ScoreType.USERSCORE;
        return this;
    }

    public BotMessage usergame(UserGame score) {
        this.ug = score;
        this.typeS = ScoreType.USERGAME;
        this.p.usergame(score);
        return this;
    }

    public BotMessage usergame(Collection<UserGame> scores) {
        this.ugc = scores;
        this.typeS = ScoreType.USERGAME;
        return this;
    }

    public BotMessage maps(ArrayList<Beatmap> maps) {
        this.maps = maps;
        return this;
    }

    public BotMessage topplays(Collection<UserScore> playsT, Collection<BeatmapScore> playsG) {
        int topPlayIdx = 0;
        int globalPlayIdx = 0;
        switch (typeS) {
            case BEATMAPSCORE:
                if (playsT != null) topPlayIdx = utilOsu.indexInTopPlays(bs, playsT);
                if (playsG != null) globalPlayIdx = utilOsu.indexInGlobalPlays(bs, playsG);
                break;
            case USERSCORE:
                if (playsT != null) topPlayIdx = utilOsu.indexInTopPlays(us, playsT);
                if (playsG != null) globalPlayIdx = utilOsu.indexInGlobalPlays(us, playsG);
                break;
            case USERGAME:
                if (playsT != null) topPlayIdx = utilOsu.indexInTopPlays(ug, playsT);
                if (playsG != null) globalPlayIdx = utilOsu.indexInGlobalPlays(ug, playsG);
                break;
            default: throw new IllegalStateException(Error.TYPES.getMsg());
        }
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

    public BotMessage history(Collection<UserGame> history) {
        int mapID;
        switch (typeS) {
            case USERSCORE: mapID = us.getBeatmapId(); break;
            case USERGAME: mapID = ug.getBeatmapId(); break;
            default: throw new IllegalStateException(Error.TYPES.getMsg());
        }
        for (UserGame game : history) {
            if (game.getBeatmapId() == mapID)
                if (game.getScore() > 10000)
                    retries++;
                else
                    break;
        }
        return this;
    }

    private String getRank() {
        String scoreRank;
        switch (typeS) {
            case BEATMAPSCORE: scoreRank = bs.getRank(); break;
            case USERSCORE: scoreRank = us.getRank(); break;
            case USERGAME: scoreRank = ug.getRank(); break;
            default: throw new IllegalStateException(Error.TYPES.getMsg());
        }
        return event.getJDA().getGuildById(secrets.devGuildID)
                .getEmoteById(utilOsu.getRankEmote(scoreRank).getValue()).getAsMention()
            + (scoreRank.equals("F") ? " (" + p.getCompletion() + "%)" : "");
    }

    private String getModString() {
        String out;
        switch (typeS) {
            case BEATMAPSCORE: out = abbrvModSet(bs.getEnabledMods()); break;
            case USERSCORE: out = abbrvModSet(us.getEnabledMods()); break;
            case USERGAME: out = abbrvModSet(ug.getEnabledMods()); break;
            default: throw new IllegalStateException(Error.TYPES.getMsg());
        }
        if (!out.equals(""))
            out = " +" + out;
        return out;
    }

    private String getKeyString() {
        if (mode != GameMode.OSU_MANIA) return "";
        String keys;
        switch (typeS) {
            case BEATMAPSCORE: keys = key_mods_str(createSum(bs.getEnabledMods())); break;
            case USERSCORE: keys = key_mods_str(createSum(us.getEnabledMods())); break;
            case USERGAME: keys = key_mods_str(createSum(ug.getEnabledMods())); break;
            default: throw new IllegalStateException(Error.TYPES.getMsg());
        }
        return "[" + (keys.equals("") ? ((int)m.getDifficultySize() + "K") : keys) + "]";
    }

    private enum ScoreType {
        BEATMAPSCORE, USERSCORE, USERGAME
    }

    private enum Error {
        USER("Unspecified user"),
        MAP("Unspecified map"),
        HISTORY("Unspecified history"),
        TYPES("Invalid score type"),
        TYPEM("Invalid message type"),
        MODE("Unsupported game mode"),
        COLLECTION("Collection is undefined");
        String msg;
        Error(String msg) {
            this.msg = msg;
        }
        String getMsg() {
            return msg;
        }
    }

    public enum MessageType {
        RECENT, COMPARE, RECENTBEST, SCORES, SINGLETOP, TOPSCORES, TEXT
    }
}
