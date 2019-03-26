package main.java.util;

import de.maxikg.osuapi.model.BeatmapScore;
import de.maxikg.osuapi.model.Mod;
import de.maxikg.osuapi.model.UserGame;
import de.maxikg.osuapi.model.UserScore;

import java.util.Collection;
import java.util.Set;

import static de.maxikg.osuapi.model.Mod.createSum;

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

    public static String abbrvModSet(Set<Mod> mods) {
        return mods_str(createSum(mods));
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

    public static int indexInTopPlays(UserGame score, Collection<UserScore> topPlays) {
        int index = 0;
        for (UserScore s: topPlays) {
            if (s.getScore() == score.getScore() && s.getDate() == s.getDate())
                return ++index;
            index++;
        }
        return -1;
    }

    public static int indexInTopPlays(BeatmapScore score, Collection<UserScore> topPlays) {
        int index = 0;
        for (UserScore s: topPlays) {
            if (s.getScore() == score.getScore() && s.getDate() == s.getDate())
                return ++index;
            index++;
        }
        return -1;
    }

    public static int indexInTopPlays(UserScore score, Collection<UserScore> topPlays) {
        int index = 0;
        for (UserScore s: topPlays) {
            if (s == score) return ++index;
            index++;
        }
        return -1;
    }

    public static int indexInGlobalPlays(UserGame score, Collection<BeatmapScore> globalPlays) {
        int index = 0;
        for (BeatmapScore s: globalPlays) {
            if (s.getScore() == score.getScore() && s.getDate() == s.getDate() && score.getUserId() == s.getUserId())
                return ++index;
            index++;
        }
        return -1;
    }

    public static int indexInGlobalPlays(BeatmapScore score, Collection<BeatmapScore> globalPlays) {
        int index = 0;
        for (BeatmapScore s: globalPlays) {
            if (score.equals(s))
                return ++index;
            index++;
        }
        return -1;
    }

    public static int indexInGlobalPlays(UserScore score, Collection<BeatmapScore> globalPlays) {
        int index = 0;
        for (BeatmapScore s: globalPlays) {
            if (s.getScore() == score.getScore() && s.getDate() == s.getDate() && score.getUserId() == s.getUserId())
                return ++index;
            index++;
        }
        return -1;
    }
}
