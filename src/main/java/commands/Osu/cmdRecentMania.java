package main.java.commands.Osu;

import de.maxikg.osuapi.model.GameMode;

public class cmdRecentMania extends cmdRecent {

    @Override
    GameMode getMode() {
        return GameMode.OSU_MANIA;
    }

    @Override
    String getName() {
        return "recentmania";
    }
}
