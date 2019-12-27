package main.java.commands.Osu.Taiko;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecentGlobalLeaderboard;
import main.java.util.utilGeneral;

public class cmdRecentTaikoGlobalLeaderboard extends cmdRecentGlobalLeaderboard {

    @Override
    protected GameMode getMode() {
        return GameMode.TAIKO;
    }

    @Override
    protected String getName() {
        return "recenttaiko";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TAIKO;
    }
}
