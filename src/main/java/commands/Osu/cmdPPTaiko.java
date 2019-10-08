package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdPPTaiko extends cmdPP {

    @Override
    public String getName() {
        return "taiko";
    }

    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }
}
