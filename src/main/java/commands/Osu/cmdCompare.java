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
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }

        if (number > 50) {
            event.getTextChannel().sendMessage("The number must be between 1 and 50").queue();
            return;
        }

        List<String> argList = new LinkedList<>(Arrays.asList(args));
        GameMod[] mods = new GameMod[] {};
        int mIdx = argList.indexOf("-m");
        if (mIdx == -1) mIdx = argList.indexOf("-mod");
        if (mIdx == -1) mIdx = argList.indexOf("-mods");
        if (mIdx != -1) {
            argList.remove(mIdx);
            if (argList.size() > mIdx) {
                mods = GameMod.get(mods_flag(argList.get(mIdx).toUpperCase()));
                argList.remove(mIdx);
            } else {
                event.getTextChannel().sendMessage(help(2)).queue();
                return;
            }
        }

        String name = argList.size() > 0 ? String.join(" ", argList) : Main.discLink.getOsu(event.getAuthor().getId());
        if (name == null) {
            event.getTextChannel().sendMessage(help(1)).queue();
            return;
        }
        String mapID = "";

        int counter = 100;
        for (Message msg: event.getTextChannel().getIterableHistory()) {
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
                event.getTextChannel().sendMessage("Could not find last `" + statics.prefix + "recent" + getName() + "`, must " +
                        "be too old").queue();
                return;
            }
        }

        List<OsuScore> scores;
        try {
            scores = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(
                    Integer.parseInt(mapID)).setMode(getMode()).setUserName(name).setLimit(1).build()
            );
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve score of `" + name + "` on map id `" + mapID + "`").queue();
            return;
        }
        if (scores.size() == 0) {
            event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                    mapID + "`").queue();
            return;
        }

        OsuScore score = scores.get(0);
        if (mIdx > -1) {
            Iterator<OsuScore> it = scores.iterator();
            while (it.hasNext() && !Arrays.equals((score = it.next()).getEnabledMods(), mods));
            if (!Arrays.equals(score.getEnabledMods(), mods)) {
                event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                        mapID + "` with mods `" + abbrvModSet(mods) + "`").queue();
                score = scores.iterator().next();
            }
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(score.getUserID()).setMode(getMode()).build());
        } catch (Exception e) {
            event.getTextChannel().sendMessage("Could not find osu user `" + name + "`").queue();
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
                event.getTextChannel().sendMessage("Could not retrieve beatmap with id `" + mapID + "`").queue();
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
            topPlays = user.getTopScores(50).get();
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve top scores of `" + name + "`").queue();
            return;
        }
        List<OsuScore> globalPlays;
        try {
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).setMode(getMode()).build());
        } catch (OsuAPIException e) {
            event.getTextChannel().sendMessage("Could not retrieve global scores of map id `" + map.getID() + "`").queue();
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
                return "Enter `" + statics.prefix + "compare" + getName() + " [osu name] [-m <nm/hd/nfeznc/...>]` to make "
                        + "me show your best play on the map of the last `" + statics.prefix + "recent" + getName() + "`.\n"
                        + "If `-m` is added with a given mod combination, I will only take these mods into account.\n"
                        + "If no player name is specified, your discord must be linked to an osu profile via `"
                        + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>" + "`" + help;
            case 2:
                return "Specify a mod combination after `-m` such as `nm`, `hdhr`, ..." + help;
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
