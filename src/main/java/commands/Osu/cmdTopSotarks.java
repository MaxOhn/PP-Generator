package main.java.commands.Osu;

import de.maxikg.osuapi.model.Beatmap;
import main.java.core.BotMessage;
import main.java.util.statics;

public class cmdTopSotarks extends cmdTopScores {

    @Override
    int getAmount() {
        return 100;
    }

    @Override
    boolean getCondition(Beatmap m) {
        if (m == null) return false;
        return (m.getCreator().equals("Sotarks")
                && !m.getVersion().contains("'s Hard")
                && !m.getVersion().contains("'s Insane")
                && !m.getVersion().contains("'s Expert")
                && !m.getVersion().contains("'s Extra")
                && !m.getVersion().contains("'s Extreme"))
                    || m.getVersion().contains("Sotarks");
    }

    @Override
    BotMessage.MessageType getMessageType() {
        return BotMessage.MessageType.TOPSOTARKS;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "topsotarks -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "topsotarks [-m <s/t/c/m for mode>] [osu name]` to make me list the user's top 5 scores"
                        + " on any Sotarks maps.\nIf no player name specified, your discord must be linked to an osu profile via `"
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
