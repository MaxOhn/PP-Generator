package main.java.commands.Osu.Fruits;

import com.oopsjpeg.osu4j.GameMode;
import main.java.commands.Osu.Standard.cmdRecentGlobalLeaderboard;
import main.java.util.utilGeneral;

public class cmdRecentFruitsGlobalLeaderboard extends cmdRecentGlobalLeaderboard {
    @Override
    public String getName() {
        return "recentctb";
    }

    @Override
    public GameMode getMode() {
        return GameMode.CATCH_THE_BEAT;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.CTB;
    }
}
