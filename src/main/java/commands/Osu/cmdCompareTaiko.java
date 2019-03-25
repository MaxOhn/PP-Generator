package main.java.commands.Osu;

import de.maxikg.osuapi.model.GameMode;

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
