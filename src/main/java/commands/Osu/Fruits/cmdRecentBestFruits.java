package main.java.commands.Osu.Fruits;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecentBest;
import main.java.util.utilGeneral;

public class cmdRecentBestFruits extends cmdRecentBest {
    @Override
    public String getName() {
        return "ctb";
    }

    @Override
    public GameMode getMode() {
        return GameMode.CATCH_THE_BEAT;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.CTB;
    }
}
