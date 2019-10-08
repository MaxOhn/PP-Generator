package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdWhatIfTaiko extends cmdWhatIf {

    @Override
    public GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    public String getName() {
        return "taiko";
    }
}
