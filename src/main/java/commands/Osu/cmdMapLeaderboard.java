package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_flag;

public class cmdMapLeaderboard extends cmdModdedCommand implements INumberedCommand {

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
                .collect(Collectors.toList());

        Pattern p = Pattern.compile("\\+[^!]*!?");
        setInitial();
        int mIdx = -1;
        for (String s : argList) {
            if (p.matcher(s).matches()) {
                mIdx = argList.indexOf(s);
                break;
            }
        }
        if (mIdx != -1) {
            String word = argList.get(mIdx);
            if (word.contains("!")) {
                status = cmdModdedCommand.modStatus.EXACT;
                word = word.substring(1, word.length() - 1);
            } else {
                status = word.equals("+nm") ? modStatus.EXACT : cmdModdedCommand.modStatus.CONTAINS;
                word = word.substring(1);
            }
            includedMods = GameMod.get(mods_flag(word.toUpperCase()));
            argList.remove(mIdx);
        }
        p = Pattern.compile("-[^!]*!");
        mIdx = -1;
        for (String s : argList) {
            if (p.matcher(s).matches()) {
                mIdx = argList.indexOf(s);
                break;
            }
        }
        if (mIdx != -1) {
            String word = argList.get(mIdx);
            word = word.substring(1, word.length()-1);
            excludedMods.addAll(Arrays.asList(GameMod.get(mods_flag(word.toUpperCase()))));
            if (word.contains("nm"))
                excludeNM = true;
            argList.remove(mIdx);
        }

        String mapID = getMapId(event, argList);
        if (mapID.equals("-1")) return;
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
            } catch (IndexOutOfBoundsException e1) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find beatmap. Did you give a mapset id instead of a map id?");
                return;
            }
            try {
                if (secrets.WITH_DB)
                    DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }

        List<OsuScore> scores;
        try {
            int limit = status != modStatus.WITHOUT ? 50 : 10;
            switch (getType()) {
                case NATIONAL:
                    scores = Main.customOsu.getScores(mapID).stream().limit(limit)
                            .filter(this::isValidScore)
                            .collect(Collectors.toList());
                    break;
                case GLOBAL:
                    scores = Main.customOsu.getScores(mapID, false).stream().limit(limit)
                            .filter(this::isValidScore)
                            .collect(Collectors.toList());
                    break;
                default:
                    scores = new ArrayList<>();
            }
        } catch (IOException e) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not retrieve scores of the beatmap, blame bade");
            e.printStackTrace();
            return;
        }
        new BotMessage(event, BotMessage.MessageType.LEADERBOARD).map(map).osuscores(scores).mode(map.getMode()).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "leaderboard -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "leaderboard[number] [beatmap url or beatmap id] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!]` to make me show the beatmap's "
                        + (getType() == lbType.NATIONAL ? "national" : "global") + " top 10 scores."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + "leaderboard8`, I will skip the most recent 7 score embeds "
                        + "and show the 8-th score embed, defaults to 1."
                        + "\nWith `+` you can choose included mods, e.g. `+hddt`, with `+mod!` you can choose exact mods, and with `-mod!` you can choose excluded mods."
                        + "\nBeatmap urls from both the new and old website are supported."
                        + "\nIf no beatmap is specified, I will search the channel's history for scores instead and consider the map of the [number]-th score, default to 1.";
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

    protected enum lbType {
        NATIONAL,
        GLOBAL
    }

    protected String getMapId(MessageReceivedEvent event, List<String> argList) {
        if (argList.size() > 0)
            return utilOsu.getIdFromString(argList.get(0));
        else {
            int counter = 100;
            for (Message msg: (event.isFromType(ChannelType.PRIVATE) ? event.getChannel() : event.getTextChannel()).getIterableHistory()) {
                if (msg.getAuthor().equals(event.getJDA().getSelfUser()) && msg.getEmbeds().size() > 0) {
                    MessageEmbed msgEmbed = msg.getEmbeds().iterator().next();
                    List<MessageEmbed.Field> fields = msgEmbed.getFields();
                    if (fields.size() > 0) {
                        if (fields.get(0).getValue().matches(".*\\{( ?\\d+ ?/){2,} ?\\d+ ?}.*")
                                || (fields.size() >= 5 && fields.get(5).getValue().matches(".*\\{( ?\\d+ ?/){2,} ?\\d+ ?}.*"))) {
                            return msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/") + 1);
                        }
                    }
                }
                if (--counter == 0) {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send("Could not find last score embed, must be too old");
                    return "-1";
                }
            }
        }
        return "-1"; // never reached
    }

    protected lbType getType() {
        return lbType.NATIONAL;
    }
}
