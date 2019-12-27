package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.OsuBeatmap;
import main.java.core.BotMessage;
import main.java.util.statics;

import java.util.regex.Pattern;

/*
    Count amount of maps in users top scores that are from sotarks
 */
public class cmdSotarks extends cmdTop {

    // The map of the score must either have sotarks as creator or be a guest diff from sotarks
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
        String help = " (`" + statics.prefix + "sotarks -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "sotarks[number] [-m <s/t/c/m for mode>] [osu name] [-acc <number>] [-grade <SS/A/D/...>] [-combo <number>] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!]` to make me list the user's top 5 scores on any Sotarks maps."
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods."
                        + "\nWith `-acc` you can specify a bottom limit for counted accuracies. Must be a positive decimal number."
                        + "\nWith `-combo` you can specify a bottom limit for counted combos. Must be a positive integer."
                        + "\nWith `-grade` you can specify what grade counted scores will have. Must be either SS, S, A, B, C, or D"
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
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
