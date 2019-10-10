package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.cmdRecentLeaderboard;

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
