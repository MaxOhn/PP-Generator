package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuScore;
import main.java.commands.ICommand;
import main.java.util.utilGeneral;

import java.util.Arrays;
import java.util.HashSet;

public abstract class cmdModdedCommand implements ICommand {

    modStatus status = modStatus.WITHOUT;
    boolean excludeNM = false;

    GameMod[] includedMods;
    HashSet<GameMod> excludedMods;

    enum modStatus {
        WITHOUT,
        CONTAINS,
        EXACT,
    }

    void setInitial() {
        status = modStatus.WITHOUT;
        includedMods = new GameMod[0];
        excludedMods = new HashSet<>();
        excludeNM = false;
    }

    boolean isValidScore(OsuScore score) {
        boolean response = excludedMods.size() == 0 || excludesMods(score);
        switch (status) {
            case CONTAINS:
                response &= utilGeneral.isSubarray(includedMods, score.getEnabledMods());
                break;
            case EXACT:
                response &= Arrays.equals(score.getEnabledMods(), includedMods);
                break;
        }
        return response & (!excludeNM || score.getEnabledMods().length > 0);
    }

    private boolean excludesMods(OsuScore s) {
        return excludedMods.stream().allMatch(m -> {
            for (GameMod mod : s.getEnabledMods())
                if (m == mod)
                    return false;
            return true;
        });
    }
}
