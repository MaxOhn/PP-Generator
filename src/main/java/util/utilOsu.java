package main.java.util;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class utilOsu {

    public static String mods_str(int mods) {
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
        if ((mods & 16384) != 0)
            sb.append("PF");
        else if ((mods & 32) != 0)
            sb.append("SD");
        if ((mods & 1048576) != 0)
            sb.append("FI");
        return sb.toString();
    }

    public static int mods_flag(String mods) {
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
                case "PF": flag += 16416; break;
                case "FI": flag += 1048576; break;
                default: break;
            }
        }
        return flag;
    }

    public static String key_mods_str(int mods) {
        if ((mods & 67108864) != 0)
            return "1K";
        if ((mods & 268435456) != 0)
            return "2K";
        if ((mods & 134217728) != 0)
            return "3K";
        if ((mods & 32768) != 0)
            return "4K";
        if ((mods & 65536) != 0)
            return "5K";
        if ((mods & 131072) != 0)
            return "6K";
        if ((mods & 262144) != 0)
            return "7K";
        if ((mods & 524288) != 0)
            return "8K";
        if ((mods & 16777216) != 0)
            return "9K";
        return "";
    }

    public static String abbrvModSet(GameMod[] mods) {
        return mods_str(createSum(mods));
    }

    public static String abbrvModSet(Set<GameMod> mods) {
        return mods_str(createSum(mods.toArray(new GameMod[0])));
    }

    public static int createSum(GameMod[] mods) {
        int sum = 0;
        for(int i = 0; i < mods.length; sum |= mods[i++].getBit());
        return sum;
    }

    public static rankEmote getRankEmote(String rank) {
        switch (rank) {
            case "XH":
                return rankEmote.XH;
            case "X":
                return rankEmote.X_;
            case "SH":
                return rankEmote.SH;
            case "S":
                return rankEmote.S_;
            case "A":
                return rankEmote.A_;
            case "B":
                return rankEmote.B_;
            case "C":
                return rankEmote.C_;
            case "D":
                return rankEmote.D_;
        }
        return rankEmote.F_;
    }

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

    public static String getIdFromString(String idString) {
        Pattern p = Pattern.compile("((.*)\\/([0-9]{1,8})($|(&|\\?)m=[0-3]))|([0-9]{1,8})");

        String id = "-1";
        Matcher m = p.matcher(idString);
        if (m.find())
            id = m.group(3);
        if (id == null)
            id = m.group(6);
        return id;
    }

    public static double getAcc(OsuScore score, GameMode mode) {
        int nTotal = score.getHit300() + score.getHit100() + score.getMisses();
        switch (mode) {
            case STANDARD:
                nTotal += score.getHit50();
                break;
            case MANIA:
                nTotal += score.getGekis() + score.getKatus() + score.getHit50();
                break;
            default: break;
        }
        return getAcc(score, mode, nTotal);
    }

    public static double getAcc(OsuScore score, GameMode mode, int nTotal) {
        double numerator = (double)score.getHit50() * 50.0D + (double)score.getHit100() * 100.0D + (double)score.getHit300() * 300.0D;
        if (mode == GameMode.MANIA)
            numerator += (double)score.getKatus() * 200.0D + (double)score.getGekis() * 300.0D;
        else if (mode == GameMode.TAIKO)
            numerator = 0.5 * score.getHit100() + score.getHit300();
        double denominator;
        if (mode == GameMode.STANDARD)
            denominator = (double)(nTotal) * 300.0D;
        else // taiko, mania
            denominator = nTotal;
        if (mode == GameMode.MANIA) denominator *= 300;
        double res = numerator / denominator;
        return 100*Math.max(0.0D, Math.min(res, 1.0D));
    }

    public static HashMap<String, Integer> getHitResults(GameMode mode, double acc, OsuScore s) {
        int nTotal = 0;
        switch (mode) {
            case STANDARD:
                nTotal += s.getHit300() + s.getHit100() + s.getHit50() + s.getMisses();
                break;
            case MANIA:
                nTotal += s.getGekis() + s.getHit300() + s.getKatus() + s.getHit100() + s.getHit50() + s.getMisses();
                break;
            case TAIKO:
                nTotal += s.getHit300() + s.getHit100() + s.getMisses();
                break;
        }
        return getHitResults(mode, acc, nTotal, s.getGekis(), s.getHit300(), s.getKatus(), s.getHit100(), s.getHit50(), s.getMisses());
    }

    public static HashMap<String, Integer> getHitResults(GameMode mode, double acc, int nTotal, int n320, int n300, int n200, int n100, int n50, int nM) {

        if (acc > 1)
            acc /= 100;

        HashMap<String, Integer> hitresults = new HashMap<>();
        hitresults.put("nTotal", nTotal);

        switch (mode) {
            case STANDARD:
                if (n50 > 0 || n100 > 0)
                        n300 = nTotal - (n100 > 0 ? n100 : 0) - (n50 > 0 ? n50 : 0) - nM;
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

    public static String getRank(GameMode mode, OsuScore score, int nObjects, Set<GameMod> mods) {
        double ratio300;
        double acc;
        switch (mode) {
            case STANDARD:
                if (score.getHit300() == nObjects)
                    return mods.contains(GameMod.HIDDEN) ? "XH" : "X";
                ratio300 = (double)score.getHit300()/nObjects;
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
        }
        return "D";
    }
}
