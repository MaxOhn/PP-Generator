package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecentBest;
import main.java.util.utilGeneral;

public class cmdRecentBestTaiko extends cmdRecentBest {
    @Override
    public String getName() {
        return "taiko";
    }

    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TAIKO;
    }
}
