package main.java.commands.Osu;

import de.maxikg.osuapi.model.GameMode;

public class cmdCompareMania extends cmdCompare {

    @Override
    GameMode getMode() {
        return GameMode.OSU_MANIA;
    }

    @Override
    String  getRegex() {
        return ".*\\{( ?\\d+ ?\\/){5} ?\\d+ ?\\}.*";
    }

    @Override
    String getName() {
        return "mania";
    }
}
