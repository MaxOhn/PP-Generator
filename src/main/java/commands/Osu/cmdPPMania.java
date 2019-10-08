package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdPPMania extends cmdPP {

    @Override
    public String getName() {
        return "mania";
    }

    @Override
    public GameMode getMode() {
        return GameMode.MANIA;
    }
}
