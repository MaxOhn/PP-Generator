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
        includedMods =new GameMod[0];
        excludedMods = new HashSet<>();
        excludeNM = false;
    }

    boolean isValidScore(OsuScore score) {
        boolean response = excludedMods.size() == 0 || excludesMods(score);
        switch (status) {
            case CONTAINS:
                response &= includesMods(score);
                break;
            case EXACT:
                response &= hasSameMods(score);
                break;
        }
        return response & (!excludeNM || score.getEnabledMods().length > 0);
    }

    boolean includesMods(OsuScore s) {
        return utilGeneral.isSubarray(includedMods, s.getEnabledMods());
    }
    boolean hasSameMods(OsuScore s) {
        return Arrays.equals(s.getEnabledMods(), includedMods);
    }
    boolean excludesMods(OsuScore s) {
        return !excludedMods.stream().allMatch(m -> {
            for (GameMod mod : s.getEnabledMods())
                if (m == mod)
                    return true;
            return false;
        });
    }
}
