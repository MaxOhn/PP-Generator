package main.java.commands.Osu.Fruits;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdProfile;
import main.java.util.utilGeneral;

public class cmdProfileFruits extends cmdProfile {
    @Override
    public GameMode getMode() {
        return GameMode.CATCH_THE_BEAT;
    }

    @Override
    public String getName() {
        return "ctb";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.CTB;
    }
}
