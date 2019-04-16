package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdCompareTaiko extends cmdCompare {

    @Override
    GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    String  getRegex() {
        return ".*\\{( ?\\d+ ?\\/){2} ?\\d+ ?\\}.*";
    }

    @Override
    String getName() {
        return "taiko";
    }
}
