package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdProfile;
import main.java.util.utilGeneral;

public class cmdProfileTaiko extends cmdProfile {
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
