package main.java.commands.Osu;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.mods_flag;

public class cmdCompare implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
            return;
        }

        if (number > 50) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("The number must be between 1 and 50");
            return;
        }

        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(LinkedList::new));

        GameMod[] mods = new GameMod[] {};
        Pattern p = Pattern.compile("\\+.*");
        int mIdx = -1;
        for (String s : argList)
            if (p.matcher(s).matches())
                mIdx = argList.indexOf(s);
        if (mIdx != -1) {
            mods = GameMod.get(mods_flag(argList.get(mIdx).substring(1).toUpperCase()));
            argList.remove(mIdx);
        }

        String name = argList.size() > 0 ? String.join(" ", argList) : Main.discLink.getOsu(event.getAuthor().getId());
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
        String mapID = "";

        int counter = 100;
        for (Message msg: (event.isFromType(ChannelType.PRIVATE) ? event.getChannel() : event.getTextChannel()).getIterableHistory()) {
            if (msg.getAuthor().equals(event.getJDA().getSelfUser()) && msg.getEmbeds().size() > 0) {
                MessageEmbed msgEmbed = msg.getEmbeds().iterator().next();
                List<MessageEmbed.Field> fields = msgEmbed.getFields();
                if (fields.size() > 0) {
                    if (fields.get(0).getValue().matches(getRegex())
                            || (fields.size() >= 5 &&
                            fields.get(5).getValue().matches(getRegex()))) {
                        mapID = msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/") + 1);
                        if (--number <= 0) break;
                    }
                }
            }
            if (--counter == 0) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find last `" + statics.prefix
                        + "recent" + getName() + "`, must " + "be too old");
                return;
            }
        }

        List<OsuScore> scores;
        try {
            scores = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(
                    Integer.parseInt(mapID)).setMode(getMode()).setUserName(name).setLimit(1).build()
            );
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve score of `" + name + "` on map id `" + mapID + "`");
            return;
        }
        if (scores.size() == 0) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find any scores of `" + name + "` on beatmap id `" +
                    mapID + "`");
            return;
        }

        OsuScore score = scores.get(0);
        if (mIdx > -1) {
            Iterator<OsuScore> it = scores.iterator();
            while (it.hasNext() && !Arrays.equals((score = it.next()).getEnabledMods(), mods));
            if (!Arrays.equals(score.getEnabledMods(), mods)) {
                String msgEnd = mods.length == 0 ? " without mods" : " with mods `" + abbrvModSet(mods) + "`";
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find any scores of `" + name
                        + "` on beatmap id `" + mapID + "`" + msgEnd);
                score = scores.iterator().next();
            }
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(score.getUserID()).setMode(getMode()).build());
        } catch (Exception e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find osu user `" + name + "`");
            return;
        }
        OsuBeatmap map;
        try {
            map = DBProvider.getBeatmap(Integer.parseInt(mapID));
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = Main.osu.beatmaps.query(
                        new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(Integer.parseInt(mapID)).setMode(getMode()).setLimit(1).build()
                ).get(0);
            } catch (OsuAPIException e1) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve beatmap with id `" + mapID + "`");
                return;
            }
            try {
                DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        List<OsuScore> topPlays;
        try {
            topPlays = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve top scores of `" + name + "`");
            return;
        }
        List<OsuScore> globalPlays;
        try {
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).setMode(getMode()).build());
        } catch (OsuAPIException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve global scores of map id `" + map.getID() + "`");
            return;
        }
        new BotMessage(event, BotMessage.MessageType.COMPARE).user(user).map(map).osuscore(score)
                .mode(getMode()).topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "compare" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "compare" + getName() + "[number] [osu name] [+<nm/hd/nfeznc/...>]` to make "
                        + "me show your best play on the map of the last `" + statics.prefix + "recent" + getName() + "`."
                        + "\nIf `+` is added with a given mod combination, i.e. `<c +dtez`, I will only take these mods into account."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + getName() + "8`, I will skip the most recent 8-1 scores "
                        + "and show the 8-th score, defaults to 1."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `"
                        + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }

    GameMode getMode() {
        return GameMode.STANDARD;
    }

    String  getRegex() {
        return ".*\\{( ?\\d+ ?\\/){3} ?\\d+ ?\\}.*";
    }

    String getName() {
        return "";
    }

    @Override
    public INumberedCommand setNumber(int number) {
        this.number = number;
        return this;
    }
}
