package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdCompareMania extends cmdCompare {

    @Override
    GameMode getMode() {
        return GameMode.MANIA;
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
