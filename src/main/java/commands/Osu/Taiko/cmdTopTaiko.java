package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdTop;
import main.java.util.utilGeneral;

public class cmdTopTaiko extends cmdTop {
    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    public String getName() {
        return "taiko";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TAIKO;
    }
}
