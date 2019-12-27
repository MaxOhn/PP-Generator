package main.java.commands.Osu.Mania;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdSimulateRecent;
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
}
