package main.java.commands.Osu;

import de.maxikg.osuapi.model.GameMode;

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