package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecent;
import main.java.util.utilGeneral;

public class cmdRecentTaiko extends cmdRecent {

    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    public String getName() {
        return "recenttaiko";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TAIKO;
    }
}