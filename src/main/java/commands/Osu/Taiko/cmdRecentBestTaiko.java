package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.cmdRecentBest;

public class cmdRecentBestTaiko extends cmdRecentBest {
    @Override
    public String getName() {
        return "taiko";
    }

    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }
}
