package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import main.java.commands.Osu.Standard.cmdTop;
import main.java.core.BotMessage;
import main.java.util.statics;

/*
    Count amount of SS in users top scores
 */
public class cmdSS extends cmdTop {

    // The score must be an SS
    @Override
    boolean getScoreCondition(OsuScore s, GameMode mode) {
        if (s == null) return false;
        boolean result = s.getMisses() == 0 && s.getHit100() == 0 && s.isPerfect();
        if (mode == GameMode.STANDARD) result &= s.getHit50() == 0;
        else if (mode == GameMode.MANIA) result &= s.getKatus() == 0 && s.getHit50() == 0;
        return result && hasValidMods(s);
    }

    @Override
    BotMessage.MessageType getMessageType() {
        return BotMessage.MessageType.SS;
    }

    @Override
    String noScoreMessage(String username, boolean withMods) {
        return "`" + username + "` appears to not have any SS scores in the"
                + " personal top 100" + (withMods ? " with the specified mods" : "") + " :/";
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "ss -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "ss [-m <s/t/c/m for mode>] [osu name] [-acc <number>] [-grade <SS/A/D/...>] [-combo <number>] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!]` to make me list the user's top 5 SS scores."
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods."
                        + "\nWith `-acc` you can specify a bottom limit for counted accuracies. Must be a positive decimal number."
                        + "\nWith `-combo` you can specify a bottom limit for counted combos. Must be a positive integer."
                        + "\nWith `-grade` you can specify what grade counted scores will have. Must be either SS, S, A, B, C, or D"
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `"
                        + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            case 2:
                return "CtB is not yet supported" + help;
            case 3:
                return "After '-m' specify either 's' for standard, 't' for taiko, 'c' for CtB, or 'm' for mania" + help;
            default:
                return help(0);
        }
    }
}
