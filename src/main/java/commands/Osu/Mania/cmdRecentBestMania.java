package main.java.commands.Osu.Mania;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecentBest;
import main.java.util.utilGeneral;

public class cmdRecentBestMania extends cmdRecentBest {
    @Override
    public String getName() {
        return "mania";
    }

    @Override
    public GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.MANIA;
    }
}
