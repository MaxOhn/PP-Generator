package main.java.commands.Osu.Mania;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecent;
import main.java.util.utilGeneral;

public class cmdRecentMania extends cmdRecent {

    @Override
    public GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    public String getName() {
        return "recentmania";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.MANIA;
    }
}
