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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_strToInt;

/*
    Show the leaderboard of a map
 */
public class cmdMapLeaderboard extends cmdModdedCommand implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        number = 1;
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (number > 50) {
            event.getChannel().sendMessage("The number must be between 1 and 50").queue();
            return;
        }
        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toList());

        // Parse mod combination
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
            includedMods = GameMod.get(mods_strToInt(word.toUpperCase()));
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
            excludedMods.addAll(Arrays.asList(GameMod.get(mods_strToInt(word.toUpperCase()))));
            if (word.contains("nm"))
                excludeNM = true;
            argList.remove(mIdx);
        }
        // Retrieve map id
        String mapID = getMapId(event, argList);
        if (mapID.equals("-1")) return;
        // Retrieve map data
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
                event.getChannel().sendMessage("Could not retrieve beatmap").queue();
                return;
            } catch (IndexOutOfBoundsException e1) {
                event.getChannel().sendMessage("Could not find beatmap. Did you give a mapset id instead of a map id?").queue();
                return;
            }
            try {
                if (secrets.WITH_DB)
                    DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        // Retrieve leaderboard of map
        List<OsuScore> scores;
        try {
            int limit = status != modStatus.WITHOUT ? 50 : 10;
            scores = Main.customOsu.getScores(mapID, getType() == lbType.NATIONAL, status == modStatus.WITHOUT ? null : new HashSet<>(Arrays.asList(includedMods)))
                    .stream().limit(limit)
                    .filter(this::hasValidMods)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            event.getChannel().sendMessage("Could not retrieve scores of the beatmap, blame bade").queue();
            e.printStackTrace();
            return;
        }
        new BotMessage(event.getChannel(), BotMessage.MessageType.LEADERBOARD).map(map).osuscores(scores).mode(map.getMode()).author(event.getAuthor()).buildAndSend();
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
        return utilGeneral.Category.OSU_GENERAL;
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

    // Read the history of the channel and get the map id of the corresponding message
    protected String getMapId(MessageReceivedEvent event, List<String> argList) {
        if (argList.size() > 0)
            return utilOsu.getIdFromString(argList.get(0));
        else {
            int counter = 100;  // go at most 100 msgs deep into the history
            for (Message msg: (event.isFromType(ChannelType.PRIVATE) ? event.getChannel() : event.getTextChannel()).getIterableHistory()) {
                // Author must be this bot and there must be an embed
                if (msg.getAuthor().equals(event.getJDA().getSelfUser()) && msg.getEmbeds().size() > 0) {
                    MessageEmbed msgEmbed = msg.getEmbeds().iterator().next();
                    MessageEmbed.AuthorInfo embedAuthor = msgEmbed.getAuthor();
                    List<MessageEmbed.Field> fields = msgEmbed.getFields();
                    // Get id from embed fields
                    if (fields.size() > 0) {
                        if (fields.get(0).getValue().matches(".*\\{( ?\\d+ ?/){2,} ?\\d+ ?}.*")
                                || (fields.size() >= 5 && fields.get(5).getValue().matches(".*\\{( ?\\d+ ?/){2,} ?\\d+ ?}.*"))) {
                            if (--number == 0)
                                return msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/") + 1);
                        }
                    // Get id from embed author
                    } else if (--number == 0 && embedAuthor != null && embedAuthor.getUrl().matches("https://osu.ppy.sh/b/.*")) {
                        return embedAuthor.getUrl().substring(embedAuthor.getUrl().lastIndexOf("/") + 1);
                    }
                }
                if (--counter == 0) {
                    event.getChannel().sendMessage("Could not find last score embed, must be too old").queue();
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
