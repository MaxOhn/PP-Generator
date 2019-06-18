package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdRecentManiaGlobalLeaderboard extends cmdRecentGlobalLeaderboard {

    @Override
    protected GameMode getMode() {
        return GameMode.MANIA;
    }

    @Override
    protected String getName() {
        return "recentmania";
    }
}
