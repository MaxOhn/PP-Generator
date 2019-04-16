package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdRecentTaiko extends cmdRecent {

    @Override
    GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    String getName() {
        return "recenttaiko";
    }
}