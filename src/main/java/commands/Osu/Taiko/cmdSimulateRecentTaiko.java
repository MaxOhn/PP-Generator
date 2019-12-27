package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdSimulateRecent;
import main.java.util.utilGeneral;

public class cmdSimulateRecentTaiko extends cmdSimulateRecent {

    @Override
    protected GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    protected String getName() {
        return "taiko";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TAIKO;
    }
}
