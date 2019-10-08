package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdWhatIfMania extends cmdWhatIf {

    @Override
    public GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    public String getName() {
        return "mania";
    }
}
