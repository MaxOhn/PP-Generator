package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;

public class cmdRecentTaikoLeaderboard extends cmdRecentLeaderboard {

    @Override
    protected GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    protected String getName() {
        return "recenttaiko";
    }
}
