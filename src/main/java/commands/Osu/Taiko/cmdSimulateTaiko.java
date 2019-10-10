package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.cmdSimulateRecent;

public class cmdSimulateTaiko extends cmdSimulateRecent {

    @Override
    protected GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    protected String getName() {
        return "taiko";
    }
}
