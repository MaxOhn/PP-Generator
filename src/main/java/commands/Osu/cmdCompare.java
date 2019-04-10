package main.java.commands.Osu;

import de.maxikg.osuapi.model.*;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.maxikg.osuapi.model.Mod.parseFlagSum;
import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.mods_flag;

public class cmdCompare implements ICommand {

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

        List<String> argList = new LinkedList<>(Arrays.asList(args));
        Set<Mod> mods = new HashSet<>();
        int mIdx = argList.indexOf("-m");
        if (mIdx == -1) mIdx = argList.indexOf("-mod");
        if (mIdx == -1) mIdx = argList.indexOf("-mods");
        if (mIdx != -1) {
            argList.remove(mIdx);
            if (argList.size() > mIdx) {
                mods = parseFlagSum(mods_flag(argList.get(mIdx).toUpperCase()));
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
                        break;
                    }
                } else {
                    String description = msgEmbed.getDescription();
                    if (description.startsWith("**1.**")) {
                        Pattern p = Pattern.compile("(.*)(\\(https:\\/\\/osu.*\\/b\\/([0-9]{1,8}))(.*)");
                        Matcher m = p.matcher(description);
                        if (m.find()) {
                            mapID = m.group(3);
                            break;
                        }
                    }
                }
            }
            if (--counter == 0) {
                event.getTextChannel().sendMessage("Could not find last `" + statics.prefix + "recent" + getName() + "`, must " +
                        "be too old").queue();
                return;
            }
        }

        Collection<BeatmapScore> scores =  Main.osu.getScores(Integer.parseInt(mapID)).mode(getMode()).username(name).query();
        if (scores.size() == 0) {
            event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                    mapID + "`").queue();
            return;
        }

        BeatmapScore score = scores.iterator().next();
        if (mIdx > -1) {
            Iterator<BeatmapScore> it = scores.iterator();
            while (it.hasNext() && !(score = it.next()).getEnabledMods().equals(mods)) ;
            if (!score.getEnabledMods().equals(mods)) {
                event.getTextChannel().sendMessage("Could not find any scores of `" + name + "` on beatmap id `" +
                        mapID + "` with mods `" + abbrvModSet(mods) + "`").queue();
                score = scores.iterator().next();
            }
        }
        User user;
        try {
            user = Main.osu.getUserByUsername(name).mode(getMode()).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("Could not find osu user `" + name + "`").queue();
            return;
        }
        Beatmap map;
        try {
            map = DBProvider.getBeatmap(Integer.parseInt(mapID));
        } catch (SQLException | ClassNotFoundException e) {
            map = Main.osu.getBeatmaps().beatmapId(Integer.parseInt(mapID)).mode(getMode()).limit(1).query().iterator().next();
            try {
                DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        Collection<UserScore> topPlays = Main.osu.getUserBestByUsername(name).mode(getMode()).limit(50).query();
        Collection<BeatmapScore> globalPlays = Main.osu.getScores(map.getBeatmapId()).mode(getMode()).query();
        new BotMessage(event, BotMessage.MessageType.COMPARE).user(user).map(map).beatmapscore(score)
                .mode(getMode()).topplays(topPlays, globalPlays).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "compare" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "compare" + getName() + "[osu name] [-m <nm/hd/nfeznc/...>]` to make "
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
}
