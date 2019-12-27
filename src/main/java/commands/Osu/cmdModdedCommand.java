package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuScore;
import main.java.commands.ICommand;
import main.java.util.utilGeneral;

import java.util.Arrays;
import java.util.HashSet;

/*
    Handle mods from arguments for a command
 */
public abstract class cmdModdedCommand implements ICommand {

    protected modStatus status = modStatus.WITHOUT;
    protected boolean excludeNM = false;

    protected GameMod[] includedMods;
    protected HashSet<GameMod> excludedMods;

    // If mods aren't given, choose WITHOUT, otherwise they must be either exact or a subset
    protected enum modStatus {
        WITHOUT,
        CONTAINS,
        EXACT,
    }

    protected void setInitial() {
        status = modStatus.WITHOUT;
        includedMods = new GameMod[0];
        excludedMods = new HashSet<>();
        excludeNM = false;
    }

    // Check if given score satisfies mod condition
    protected boolean hasValidMods(OsuScore score) {
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
