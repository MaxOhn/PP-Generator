package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdRecentMania extends cmdRecent {

    @Override
    GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    String getName() {
        return "recentmania";
    }
}
