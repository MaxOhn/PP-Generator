package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.cmdTop;

public class cmdTopTaiko extends cmdTop {
    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    public String getName() {
        return "taiko";
    }
}
