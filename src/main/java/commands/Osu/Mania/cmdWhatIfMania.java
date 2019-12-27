package main.java.commands.Osu.Mania;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdWhatIf;
import main.java.util.utilGeneral;

public class cmdWhatIfMania extends cmdWhatIf {

    @Override
    public GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    public String getName() {
        return "mania";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.MANIA;
    }
}
