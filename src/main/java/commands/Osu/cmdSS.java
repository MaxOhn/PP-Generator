package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import main.java.core.BotMessage;
import main.java.util.statics;

public class cmdSS extends cmdTopScores {

    @Override
    int getAmount() {
        return 100;
    }

    @Override
    boolean getScoreCondition(OsuScore s, GameMode mode) {
        if (s == null) return false;
        boolean result = s.getMisses() == 0 && s.getHit100() == 0;
        if (mode == GameMode.STANDARD) result &= s.getHit50() == 0;
        else if (mode == GameMode.MANIA) result &= s.getGekis() == 0 && s.getKatus() == 0 && s.getHit50() == 0;
        return result && s.isPerfect();
    }

    @Override
    BotMessage.MessageType getMessageType() {
        return BotMessage.MessageType.SS;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "ss -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "ss [-m <s/t/c/m for mode>] [osu name]` to make me list the user's top 5 SS scores."
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
