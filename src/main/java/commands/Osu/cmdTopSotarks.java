package main.java.commands.Osu;

import com.oopsjpeg.osu4j.OsuBeatmap;
import main.java.core.BotMessage;
import main.java.util.statics;

import java.util.regex.Pattern;

public class cmdTopSotarks extends cmdTopScores {

    @Override
    int getAmount() {
        return 100;
    }

    @Override
    boolean getMapCondition(OsuBeatmap m) {
        if (m == null) return false;
        Pattern p = Pattern.compile("^(?!.*Sotarks).*'s? (Easy|Normal|Hard|Insane|Expert|Extra|Extreme)");
        return (m.getCreatorName().equals("Sotarks") && !p.matcher(m.getVersion()).matches())
                    || m.getVersion().contains("Sotarks");
    }

    @Override
    BotMessage.MessageType getMessageType() {
        return BotMessage.MessageType.TOPSOTARKS;
    }

    @Override
    String noScoreMessage(String username, boolean withMods) {
        return "`" + username + "` appears to not have any Sotarks scores " + (withMods ? "with the specified mods " : "")
                + "in the personal top 100 and I could not be any prouder \\:')";
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
