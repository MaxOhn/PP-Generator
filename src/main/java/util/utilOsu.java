package main.java.util;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.core.DBProvider;
import main.java.core.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class utilOsu {

    // Given a mod int, return the corresponding string
    public static String mods_intToStr(int mods) {
        if (mods == 0)
            return "NM";
        StringBuilder sb = new StringBuilder();
        if ((mods & 1) != 0)
            sb.append("NF");
        if ((mods & 2) != 0)
            sb.append("EZ");
        if ((mods & 4) != 0)
            sb.append("TD");
        if ((mods & 8) != 0)
            sb.append("HD");
        if ((mods & 16) != 0)
            sb.append("HR");
        if ((mods & 512) != 0)
            sb.append("NC");
        else if ((mods & 64) != 0)
            sb.append("DT");
        if ((mods & 256) != 0)
            sb.append("HT");
        if ((mods & 1024) != 0)
            sb.append("FL");
        if ((mods & 4096) != 0)
            sb.append("SO");
        if ((mods & 16_384) != 0)
            sb.append("PF");
        else if ((mods & 32) != 0)
            sb.append("SD");
        if ((mods & 1_048_576) != 0)
            sb.append("FI");
        if ((mods & 1_073_741_824) != 0)
            sb.append("MR");
        return sb.toString();
    }

    // Given a mod string, return the corresponding int
    public static int mods_strToInt(String mods) {
        int flag = 0;
        String[] modArr = mods.split("(?<=\\G..)");
        for (String s: modArr) {
            switch (s) {
                case "NF": flag += 1; break;
                case "EZ": flag += 2; break;
                case "TD": flag += 4; break;
                case "HD": flag += 8; break;
                case "HR": flag += 16; break;
                case "SD": flag += 32; break;
                case "DT": flag += 64; break;
                case "HT": flag += 256; break;
                case "NC": flag += 576; break;
                case "FL": flag += 1024; break;
                case "SO": flag += 4096; break;
                case "PF": flag += 16_416; break;
                case "FI": flag += 1_048_576; break;
                case "MR": flag += 1_073_741_824; break;

                case "1K": flag += 67_108_864; break;
                case "2K": flag += 268_435_456; break;
                case "3K": flag += 134_217_728; break;
                case "4K": flag += 32_768; break;
                case "5K": flag += 65_536; break;
                case "6K": flag += 131_072; break;
                case "7K": flag += 262_144; break;
                case "8K": flag += 524_288; break;
                case "9K": flag += 16_777_216; break;

                default: break;
            }
        }
        return flag;
    }

    // Given a mod int, return the corresponding string but only for key mods
    public static String keys_intToStr(int mods) {
        if ((mods & 67_108_864) != 0)
            return "1K";
        if ((mods & 268_435_456) != 0)
            return "2K";
        if ((mods & 134_217_728) != 0)
            return "3K";
        if ((mods & 32_768) != 0)
            return "4K";
        if ((mods & 65_536) != 0)
            return "5K";
        if ((mods & 131_072) != 0)
            return "6K";
        if ((mods & 262_144) != 0)
            return "7K";
        if ((mods & 524_288) != 0)
            return "8K";
        if ((mods & 16_777_216) != 0)
            return "9K";
        return "";
    }

    // Given a mod array, return the corresponding string
    public static String mods_arrToStr(GameMod[] mods) {
        return mods_intToStr(mods_arrToInt(mods));
    }

    // Given a mod array, return the corresponding string
    public static String mods_setToStr(Set<GameMod> mods) {
        return mods_intToStr(mods_arrToInt(mods.toArray(new GameMod[0])));
    }

    // Given a mod array, return the corresponding int
    public static int mods_arrToInt(GameMod[] mods) {
        int sum = 0;
        for(int i = 0; i < mods.length; sum |= mods[i++].getBit());
        return sum;
    }

    // Given a rank as a string, return the corresponding rank as enum
    public static rankEmote getRankEmote(String rank) {
        switch (rank) {
            case "XH": return rankEmote.XH;
            case "X": return rankEmote.X_;
            case "SH": return rankEmote.SH;
            case "S": return rankEmote.S_;
            case "A": return rankEmote.A_;
            case "B": return rankEmote.B_;
            case "C": return rankEmote.C_;
            case "D": return rankEmote.D_;
            default: return rankEmote.F_;
        }
    }

    // Saving ranks as discord emote ids
    public enum rankEmote {
        XH("515354675059621888"),
        X_("515354674929336320"),
        SH("515354675323600933"),
        S_("515354674791186433"),
        A_("515339175222837259"),
        B_("515354674866683904"),
        C_("515354674476351492"),
        D_("515354674963021824"),
        F_("515623098947600385");

        private String value;

        rankEmote(String s) {
            this.value = s;
        }

        public String getValue() {
            return value;
        }
    }

    // Calculate the index of a score in a score collection, return -1 if the score is not contained
    public static int indexInTopPlays(OsuScore score, Collection<OsuScore> topPlays) {
        int index = 0;
        for (OsuScore s: topPlays) {
            if (s.getScore() == score.getScore()
                    && s.getUserID() == score.getUserID()
                    && s.getMaxCombo() == score.getMaxCombo())
                return ++index;
            index++;
        }
        return -1;
    }

    // Given the url of osu beatmap, return the id of that map as a string
    public static String getIdFromString(String idString) {
        Pattern p = Pattern.compile("((.*)/([0-9]{1,8})($|([&?])m=[0-3]))|(^[0-9]{1,8}$)");
        String id = "-1";
        // Replace < > in case they were given for discord
        Matcher m = p.matcher(idString.replace("<", "").replace(">", ""));
        if (m.find())
            id = m.group(3);
        if (id == null)
            id = m.group(6);
        return id;
    }

    // First count the amount of total objects, then calculate the accuracy
    public static double getAcc(OsuScore score, GameMode mode) {
        int nTotal = score.getHit300() + score.getHit100() + score.getMisses();
        switch (mode) {
            case MANIA:
                nTotal += score.getGekis();
            case CATCH_THE_BEAT:
                nTotal += score.getKatus();
            case STANDARD:
                nTotal += score.getHit50();
            default: break;
        }
        return getAcc(score, mode, nTotal);
    }

    // Given a score, a mode and the amount of total hitobjects, return the accuracy of the score
    public static double getAcc(OsuScore score, GameMode mode, int nTotal) {
        double numerator = 0, denominator = nTotal;
        switch (mode) {
            case MANIA:
                numerator = (double)score.getKatus() * 200.0D + (double)score.getGekis() * 300.0D;
            case STANDARD:
                numerator += (double)score.getHit50() * 50.0D + (double)score.getHit100() * 100.0D + (double)score.getHit300() * 300.0D;
                denominator *= 300.0;
                break;
            case CATCH_THE_BEAT:
                numerator = (double)score.getHit300() + (double)score.getHit100() + (double)score.getHit50();
                //denominator = numerator + (double)score.getKatus() + (double)score.getMisses();
                break;
            case TAIKO:
                numerator = 0.5 * score.getHit100() + score.getHit300();
                break;
        }
        return Math.round(10_000.0 * numerator / denominator) / 100.0;
    }

    // Given the accuracy, approximate the hitresults of the score
    public static HashMap<String, Integer> getHitResults(GameMode mode, double acc, int nTotal, int n320, int n300, int n200, int n100, int n50, int nM) {
        if (acc > 1)
            acc /= 100;
        HashMap<String, Integer> hitresults = new HashMap<>();
        hitresults.put("nTotal", nTotal);
        switch (mode) {
            case STANDARD:
                if (n50 > 0 || n100 > 0)
                        n300 = nTotal - Math.max(n100, 0) - Math.max(n50, 0) - nM;
                else {
                    int targetTotal = (int) Math.round(acc * nTotal * 6);
                    int delta = targetTotal - (nTotal - nM);
                    n300 = delta / 5;
                    n100 = delta % 5;
                    n50 = nTotal - n300 - n100 - nM;
                }
                break;
            case TAIKO:
                if (n100 > 0)
                    n300 = nTotal - n100 - nM;
                else {
                    int targetTotal = (int) Math.round(acc * nTotal * 2);
                    n300 = targetTotal - (nTotal - nM);
                    n100 = nTotal - n300 - nM;
                }
                break;
            default: break;
        }
        hitresults.put("n320", n320);
        hitresults.put("n300", n300);
        hitresults.put("n200", n200);
        hitresults.put("n100", n100);
        hitresults.put("n50", n50);
        hitresults.put("nM", nM);
        return hitresults;
    }

    // Calculate the grade as a string of a given score
    public static String getGrade(GameMode mode, OsuScore score, int nObjects) {
        Set<GameMod> mods = new HashSet<>(Arrays.asList(score.getEnabledMods()));
        double acc;
        switch (mode) {
            case STANDARD:
                if (score.getHit300() == nObjects)
                    return mods.contains(GameMod.HIDDEN) ? "XH" : "X";
                double ratio300 = (double)score.getHit300()/nObjects;
                if (ratio300 > 0.9 && (double)score.getHit50()/nObjects < 0.01 && score.getMisses() == 0)
                    return mods.contains(GameMod.HIDDEN) ? "SH" : "S";
                else if ((ratio300 > 0.8 && score.getMisses() == 0) || ratio300 > 0.9)
                    return "A";
                else if ((ratio300 > 0.7 && score.getMisses() == 0) || ratio300 > 0.8)
                    return "B";
                else if (ratio300 > 0.6)
                    return "C";
            case MANIA:
                if (score.getGekis() == nObjects)
                    return mods.contains(GameMod.HIDDEN) ? "XH" : "X";
                acc = getAcc(score, mode);
                if (acc > 95)
                    return mods.contains(GameMod.HIDDEN) ? "SH" : "S";
                else if (acc > 90)
                    return "A";
                else if (acc > 80)
                    return "B";
                else if (acc > 70)
                    return "C";
            case TAIKO:
                if (score.getHit300() == nObjects)
                    return mods.contains(GameMod.HIDDEN) ? "XH" : "X";
                acc = getAcc(score, mode);
                if (acc > 95)
                    return mods.contains(GameMod.HIDDEN) ? "SH" : "S";
                else if (acc > 90)
                    return "A";
                else if (acc > 80)
                    return "B";
            case CATCH_THE_BEAT:
                acc = getAcc(score, GameMode.CATCH_THE_BEAT);
                if (acc == 100)
                    return mods.contains(GameMod.HIDDEN) ? "XH" : "X";
                else if (acc > 98)
                    return mods.contains(GameMod.HIDDEN) ? "SH" : "S";
                else if (acc > 94)
                    return "A";
                else if (acc > 90)
                    return "B";
                else if (acc > 85)
                    return "C";
        }
        return "D";
    }

    // Remove all misses of a score and modify the combo to a fullcombo
    public static void unchokeScore(OsuScore score, int maxCombo, GameMode mode, int nObjects, int transferTo) {
        if (mode == GameMode.MANIA) {
            score.setScore(1_000_000);
            score.setCountgeki(score.getGekis() + score.getMisses() + nObjects - (score.getGekis() + score.getHit300()
                    + score.getKatus() + score.getHit100() + score.getHit50() + score.getMisses()));
            score.setCountmiss(0);
            return;
        }
        if (score.getMaxCombo() == maxCombo) return;
        score.setMaxcombo(maxCombo);
        int missing = nObjects - (score.getHit300() + score.getHit100() + score.getHit50() + score.getMisses());
        double ratio = (double)score.getHit300() / (score.getHit300() + score.getHit100() + score.getHit50());
        // Distribute missing hitresults based on the current ratio
        if (missing > 0) {
            int new300s = (int)(ratio * missing);
            score.setCount300(score.getHit300() + new300s);
            score.setCount100(score.getHit100() + (missing - new300s));
        }
        switch (transferTo) {
            case 300: score.setCount300(score.getHit300() + score.getMisses()); break;
            case 100: score.setCount100(score.getHit100() + score.getMisses()); break;
            case 50: score.setCount50(score.getHit50() + score.getMisses()); break;
            default: throw new IllegalArgumentException("transferTo must be either 300, 100, or 50");
        }
        score.setCountmiss(0);
        score.setPp(0);
        score.setRank(utilOsu.getGrade(mode, score, nObjects));
    }

    public static OsuBeatmap getBeatmap(int mapID) throws OsuAPIException, IndexOutOfBoundsException {
        OsuBeatmap map;
        try {
            if (secrets.WITH_DB) map = DBProvider.getBeatmap(mapID);
            else throw new SQLException();
        } catch (ClassNotFoundException | SQLException e) {
            map = Main.osu.beatmaps.query(new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(mapID).build()).get(0);
            boolean validForDB = map.getApproved() == ApprovalState.RANKED || map.getApproved() == ApprovalState.LOVED;
            if (secrets.WITH_DB && validForDB) {
                Logger logger = LoggerFactory.getLogger(utilOsu.class);
                try {
                    DBProvider.addBeatmap(map);
                    logger.debug("Added map id " + map.getID() + " to map database");
                } catch (ClassNotFoundException | SQLException ex) {
                    logger.error("Error while adding new map id " + map.getID() + " to map database: ", e);
                } catch (IllegalArgumentException ex) {
                    StringBuilder msg = new StringBuilder("Map id ")
                            .append(map.getID())
                            .append(" not added to map database because ")
                            .append(ex.getMessage())
                            .append(" too long (");
                    switch (ex.getMessage()) {
                        case "version": msg.append(map.getVersion().length()); break;
                        case "title": msg.append(map.getTitle().length()); break;
                        case "artist": msg.append(map.getArtist().length()); break;
                        case "source": msg.append(map.getSource().length()); break;
                        default:
                            logger.error("Unexpected error in addBeatmap:", ex);
                            return map;
                    }
                    logger.debug(msg.append(")").toString());
                }
            }
        }
        return map;
    }

    public static List<OsuBeatmap> getBeatmaps(Collection<OsuScore> scores) throws SQLException, ClassNotFoundException, OsuAPIException {
        Logger logger = LoggerFactory.getLogger(utilOsu.class);
        List<OsuBeatmap> maps = new ArrayList<>();
        if (secrets.WITH_DB) {
            HashMap<Integer, OsuBeatmap> dbMaps = DBProvider.getBeatmaps(scores);
            logger.debug("Retrieved " + dbMaps.size() + "/" + scores.size() + " maps from beatmapInfo database");
            for (OsuScore s : scores) {
                OsuBeatmap map = dbMaps.get(s.getBeatmapID());
                if (map != null) maps.add(map);
                else {
                    map = s.getBeatmap().get();
                    maps.add(map);
                    boolean validForDB = map.getApproved() == ApprovalState.RANKED || map.getApproved() == ApprovalState.LOVED;
                    if (validForDB) {
                        try {
                            DBProvider.addBeatmap(map);
                        } catch (IllegalArgumentException ex) {
                            StringBuilder msg = new StringBuilder("Map id ")
                                    .append(map.getID())
                                    .append(" not added to map database because ")
                                    .append(ex.getMessage())
                                    .append(" too long (");
                            switch (ex.getMessage()) {
                                case "version":
                                    msg.append(map.getVersion().length());
                                    break;
                                case "title":
                                    msg.append(map.getTitle().length());
                                    break;
                                case "artist":
                                    msg.append(map.getArtist().length());
                                    break;
                                case "source":
                                    msg.append(map.getSource().length());
                                    break;
                                default:
                                    msg.append(-1);
                                    break;
                            }
                            logger.debug(msg.append(")").toString());
                        }
                        logger.debug("Added map id " + map.getID() + " to map database");
                    }
                }
            }
        } else {
            for (OsuScore s : scores)
                maps.add(s.getBeatmap().get());
        }
        return maps;
    }
}
