package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

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
