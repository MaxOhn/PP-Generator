package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointUserRecents;
import main.java.core.Main;
import main.java.util.statics;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

/*
    Simulate a no-choke play out of the recent score
 */
public class cmdSimulateRecent extends cmdSimulateMap {

    private OsuScore recent;

    // Retrieve the map id
    @Override
    protected OsuScore retrieveScore(MessageReceivedEvent event, List<String> argList) {
        if (number > 50) {
            event.getChannel().sendMessage("The number must be between 1 and 50").queue();
            return null;
        }
        recent = null;
        // Get the name either from arguments or from database link
        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return null;
            }
        } else {
            name = String.join(" ", argList);
        }
        // Check if name is given as mention
        if (event.isFromType(ChannelType.TEXT) && event.getMessage().getMentionedMembers().size() > 0) {
            name = Main.discLink.getOsu(event.getMessage().getMentionedMembers().get(0).getUser().getId());
            if (name == null) {
                event.getChannel().sendMessage("The mentioned user is not linked, I don't know who you mean").queue();
                return null;
            }
        }
        // Retrieve recent scores of user
        ArrayList<OsuScore> userRecents;
        try {
            userRecents = new ArrayList<>(Main.osu.userRecents.query(
                    new EndpointUserRecents.ArgumentsBuilder(name).setMode(getMode()).setLimit(50).build())
            );
            recent = userRecents.get(0);
            while (--number > 0 && userRecents.size() > 1) {
                userRecents.remove(0);
                recent = userRecents.get(0);
            }
            if (number > 0) {
                event.getChannel().sendMessage("User's recent history doesn't go that far back").queue();
                return null;
            }
        } catch (Exception e) {
            event.getChannel().sendMessage("`" + name + "` was not found or no recent plays").queue();
            return null;
        }
        if (getMode() == GameMode.STANDARD && !recent.isPerfect())
            recent.setScore(0);
        return recent;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "simulate" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "simulate" + getName() + "[number] [osu name] [+<nm/hd/nfeznc/...>]"
                        + " [-a <accuracy>] [-c <combo>] [-x/-m <amount misses>] [-s <score>] [-300 <amount 300s>] [-100 <amount 100s>] [-50 <amount 50s>]`"
                        + " to make me simulate a score on the user's most recently played map."
                        + "\nIf the score is a pass on a osu!std map, I will simulate a nochoke, otherwise (fail or non-osu!std score) I consider the given parameters, defaults to SS."
                        + "\nFor mania scores, only the score value matters so don't bother adding acc, misses, 320s, ..."
                        + "\nIf a number is specified and no beatmap, e.g. `" + statics.prefix + "simulate" + getName() + "8`, I will skip the most recent 7 scores "
                        + "and choose the 8-th recent score, defaults to 1."
                        + "\nWith `+` you can choose mods, e.g. `+ezhddt`."
                        + "\nIf no mods are specified, I will simulate for the mods NM, HD, HR, DT, and HDDT."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            default:
                return help(0);
        }
    }

    protected String getName() {
        return "recent";
    }

    protected GameMode getMode() {
        return GameMode.STANDARD;
    }

    @Override
    public cmdSimulateRecent setNumber(int number) {
        this.number = number;
        return this;
    }
}
