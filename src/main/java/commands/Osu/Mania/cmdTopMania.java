package main.java.commands.Osu.Mania;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.cmdTop;

public class cmdTopMania extends cmdTop {
    @Override
    public GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    public String getName() {
        return "mania";
    }
}
