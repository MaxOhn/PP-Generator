package main.java.commands.Osu;

import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import main.java.util.utilOsu;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class cmdScores implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        number = 1;
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (number > 50) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("The number must be between 1 and 50");
            return;
        }

        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(LinkedList::new));

        String mapID = "-1";
        if (argList.size() > 0) {
            mapID = utilOsu.getIdFromString(args[0]);
            if (!mapID.equals("-1"))
                argList.remove(0);
        }
        if (mapID.equals("-1")) {
            int counter = 100;
            for (Message msg : (event.isFromType(ChannelType.PRIVATE) ? event.getChannel() : event.getTextChannel()).getIterableHistory()) {
                if (msg.getAuthor().equals(event.getJDA().getSelfUser()) && msg.getEmbeds().size() > 0) {
                    MessageEmbed msgEmbed = msg.getEmbeds().iterator().next();
                    if (msgEmbed.getAuthor() != null && msgEmbed.getAuthor().getUrl().contains("/b/")) {
                        mapID = msgEmbed.getAuthor().getUrl().substring(msgEmbed.getAuthor().getUrl().lastIndexOf("/") + 1);
                        if (--number <= 0) break;
                    } else {
                        List<MessageEmbed.Field> fields = msgEmbed.getFields();
                        if (fields.size() > 0) {
                            if (fields.get(0).getValue().matches(".*\\{( ?\\d+ ?\\/){2,} ?\\d+ ?\\}.*")
                                    || (fields.size() >= 5 && fields.get(5).getValue().matches(".*\\{( ?\\d+ ?\\/){2,} ?\\d+ ?\\}.*"))) {
                                mapID = msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/") + 1);
                                if (--number <= 0) break;
                            }

                        }
                    }
                }
                if (--counter == 0) {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find last score embed, must be too old");
                    return;
                }
            }
        }

        String name = argList.size() > 0
                ? String.join(" ", argList)
                : Main.discLink.getOsu(event.getAuthor().getId());
        if (name == null) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
            return;
        }
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("The mentioned user is not linked, I don't know who you mean");
                return;
            }
        }

        OsuBeatmap map;
        try {
            if (!secrets.WITH_DB)
                throw new SQLException();
            map = DBProvider.getBeatmap(Integer.parseInt(mapID));
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = Main.osu.beatmaps.query(
                        new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(Integer.parseInt(mapID)).build()
                ).get(0);
            } catch (OsuAPIException e1) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve beatmap");
                return;
            }
            try {
                if (secrets.WITH_DB)
                    DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        } catch (IndexOutOfBoundsException e1) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find beatmap. Did you give a mapset id instead of a map id?");
            return;
        }

        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(map.getMode()).build());
        } catch (Exception e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find osu user `" + name + "`");
            return;
        }

        List<OsuScore> scores;
        try {
            scores = Main.osu.scores.query(
                    new EndpointScores.ArgumentsBuilder(Integer.parseInt(mapID)).setUserName(name).setMode(map.getMode()).build()
            );
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve scores");
            return;
        }
        if (scores.size() == 0) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find any scores of `" + name
                    + "` on beatmap id `" + mapID + "`");
            return;
        }
        new BotMessage(event, BotMessage.MessageType.SCORES).user(user).map(map).osuscores(scores).mode(map.getMode()).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "scores -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "scores[number] [beatmap url or beatmap id] [osu name]` to make me show the user's "
                        + "top scores for each mod combination of the specified map."
                        + "\nIf a number is specified and no beatmap, e.g. `" + statics.prefix + "scores8`, I will skip the most recent 7 score embeds "
                        + "and show the 8-th score embed, defaults to 1."
                        + "\nBeatmap urls from both the new and old website are supported."
                        + "\nIf no beatmap is specified, I will search through the channel's history and pick the map of [number]-th score embed I can find, number defaults to 1."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`"
                        + "\nIf both a map and a name are specified, be sure to give the map as first argument and the name as following argument.";
            case 1:
                return "Either specify an osu name as second argument or link your discord to an osu profile via `" +
                        statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }

    @Override
    public INumberedCommand setNumber(int number) {
        this.number = number;
        return this;
    }
}
