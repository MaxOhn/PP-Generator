package main.java.commands.Osu.Mania;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdSimulateRecent;
import main.java.util.statics;
import main.java.util.utilGeneral;

public class cmdSimulateRecentMania extends cmdSimulateRecent {

    @Override
    protected GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    protected String getName() {
        return "mania";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.MANIA;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "simulate" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "simulate" + getName() + "[number] [osu name] [+<nm/hd/nfeznc/...>]"
                        + "[-s <score>]`"
                        + " to make me simulate a score on the user's most recently played mania map."
                        + "\nIf a score value is given I will simulate that value, otherwise I simulate an SS"
                        + "\nIf a number is specified and no beatmap, e.g. `" + statics.prefix + "simulate" + getName() + "8`, I will skip the most recent 7 scores "
                        + "and choose the 8-th recent score, defaults to 1."
                        + "\nWith `+` you can choose mods, e.g. `+ezhddt`."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            default:
                return help(0);
        }
    }
}
