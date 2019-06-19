package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointUserRecents;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class cmdSimulateRecent extends cmdSimulateMap {

    @Override
    protected String getMapId(MessageReceivedEvent event, List<String> argList) {

        if (number > 50) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("The number must be between 1 and 50");
            return "-1";
        }

        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
                return "-1";
            }
        } else {
            name = String.join(" ", argList);
        }
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("The mentioned user is not linked, I don't know who you mean");
                return "-1";
            }
        }

        ArrayList<OsuScore> userRecents;
        OsuScore recent;
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
                new BotMessage(event, BotMessage.MessageType.TEXT).send("User's recent history doesn't go that far back");
                return "-1";
            }
        } catch (Exception e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("`" + name + "` was not found or no recent plays");
            return "-1";
        }
        return recent.getBeatmapID() + "";
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "simulate" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "simulate" + getName() + "[number] [osu name]"
                        + "[-a <accuracy>] [-c <combo>] [-x/-m <misses>] [-s <score>] [-320 <320s>] [-300 <300s>] [-200 <200s>] [-100 <100s>] [-50 <50s>]`"
                        + "to make me calculate the pp of the specified score on the user's most recently played map, defaults to SS score."
                        + "\nIf a number is specified and no beatmap, e.g. `" + statics.prefix + "simulate" + getName() + "8`, I will skip the most recent 7 scores "
                        + "and choose the 8-th recent score, defaults to 1."
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods."
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
