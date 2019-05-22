package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuScore;
import main.java.commands.ICommand;
import main.java.util.utilGeneral;

import java.util.Arrays;

public abstract class cmdModdedCommand implements ICommand {

    modStatus status = modStatus.WITHOUT;

    GameMod[] mods;

    enum modStatus {
        WITHOUT,
        CONTAINS,
        EXACT
    }

    boolean includesMods(OsuScore s) {
        return utilGeneral.isSubarray(mods, s.getEnabledMods());
    }
    boolean hasSameMods(OsuScore s) {
        return Arrays.equals(s.getEnabledMods(), mods);
    }
}
