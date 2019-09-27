package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointUserRecents;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class cmdRecentLeaderboard extends cmdMapLeaderboard implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        number = 1;
        return super.called(args, event);
    }

    @Override
    protected String getMapId(MessageReceivedEvent event, List<String> argList) {

        if (number > 50) {
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("The number must be between 1 and 50");
            return "-1";
        }

        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                return "-1";
            }
        } else {
            name = String.join(" ", argList);
        }
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("The mentioned user is not linked, I don't know who you mean");
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
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("User's recent history doesn't go that far back");
                return "-1";
            }
        } catch (Exception e) {
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("`" + name + "` was not found or no recent plays");
            return "-1";
        }
        return recent.getBeatmapID() + "";
    }

    @Override
    public cmdRecentLeaderboard setNumber(int number) {
        this.number = number;
        return this;
    }

    protected GameMode getMode() {
        return GameMode.STANDARD;
    }

    protected String getName() {
        return "recent";
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + getName() + "leaderboard -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + getName() + "leaderboard[number] [osu name]` to make me show the national top 10 scores on the beatmap of the user's last play."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + getName() + "leaderboard8`, I will skip the most recent 7 scores "
                        + "and show the leaderboard of the 8-th recent score, defaults to 1.";
            case 1:
                return "The first argument must either be the link to a beatmap e.g. `https://osu.ppy.sh/b/1613091&m=0`, or just the id of the beatmap" + help;
            default:
                return help(0);
        }
    }
}
