package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.FileInteractor;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_flag;

public class cmdSimulateMap extends cmdModdedCommand implements INumberedCommand {

    protected int number = 1;

    private static final GameMod[][] allMods = {
            {},
            { GameMod.HIDDEN },
            { GameMod.HARD_ROCK },
            { GameMod.HIDDEN, GameMod.HARD_ROCK },
            { GameMod.DOUBLE_TIME },
            { GameMod.HIDDEN, GameMod.DOUBLE_TIME }
    };

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

        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

        // Retrieve mods from argument list
        boolean noMods = true;
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
            noMods =  false;
            argList.remove(mIdx);
        }

        // Retrieve hit object parameters from argument list
        double acc = -1;
        int combo = 0, nM = 0, score = 0, n320 = 0, n300 = 0, n200 = 0, n100 = 0, n50 = 0;

        if ((mIdx = argList.indexOf("-a")) != -1 || (mIdx = argList.indexOf("-acc")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-a` must come a decimal number!");
                return;
            }
            try {
                acc = Double.parseDouble(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-a` must come a decimal number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-c")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-c` must come an integer number!");
                return;
            }
            try {
                combo = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-c` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-x")) != -1 || (mIdx = argList.indexOf("-m")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-x`/`-m` must come an integer number!");
                return;
            }
            try {
                nM = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-x`/`m` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-s")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-s` must come an integer number!");
                return;
            }
            try {
                score = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-s` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-320")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-320` must come an integer number!");
                return;
            }
            try {
                n320 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-320` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-300")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-300` must come an integer number!");
                return;
            }
            try {
                n300 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-300` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-200")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-200` must come an integer number!");
                return;
            }
            try {
                n200 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-200` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-100")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-100` must come an integer number!");
                return;
            }
            try {
                n100 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-100` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-50")) != -1) {
            if (argList.size() < mIdx + 2) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-50` must come an integer number!");
                return;
            }
            try {
                n50 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send("After `-50` must come an integer number!");
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if (acc < -1 || acc > 100 || combo < 0 || score < 0 || nM < 0 || n320 < 0 || n300 < 0 || n200 < 0 || n100 < 0 || n50 < 0 ) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("Invalid hit results. Acc must be between 0 and 100, everything else must be non-negative.");
            return;
        }

        // Retrieve map
        String mapID = getMapId(event, argList);
        if (mapID.equals("-1")) {
            return;
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

        // Check parameter contrains
        int hitSum = 0;
        FileInteractor.prepareFiles(map);
        int nTotal = FileInteractor.countTotalObjects(map.getID());
        switch (map.getMode()) {
            case STANDARD:
                hitSum = n300 + n100 + n50 + nM;
                break;
            case MANIA:
                hitSum = n320 + n300 + n200 + n100 + n50 + nM;
                if (score > 1000000) {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send("The score must be between 0 and 1000000 on mania maps");
                    return;
                }
                break;
            case TAIKO:
                hitSum = n300 + n100 + nM;
                break;
            case CATCH_THE_BEAT:
                new BotMessage(event, BotMessage.MessageType.TEXT).send("Mode not yet supported :(");
                return;
        }
        if (hitSum > nTotal) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send("The map has only " + nTotal + " objects, you gave too many (" + hitSum + ") in the parameters");
            return;
        } else if (hitSum == 0 && acc == -1) {
            if (map.getMode() == GameMode.MANIA) n320 = nTotal;
            else n300 = nTotal;
        }
        if (map.getMode() == GameMode.MANIA && score == 0) {
            score = 1000000;
        }

        // Create simulated score
        OsuScore osuscore = getScore();
        List<OsuScore> scores = new ArrayList<>();
        if (osuscore == null || map.getMode() != GameMode.STANDARD || osuscore.getRank().equals("F")) {
            if (hitSum < nTotal) {
                HashMap<String, Integer> hitresults = utilOsu.getHitResults(map.getMode(), acc >= 0 ? acc : 100, nTotal, n320, n300, n200, n100, n50, nM);
                n320 = hitresults.get("n320");
                n300 = hitresults.get("n300");
                n200 = hitresults.get("n200");
                n100 = hitresults.get("n100");
                n50 = hitresults.get("n50");
                nM = hitresults.get("nM");
            }
            if (combo == 0) combo = map.getMaxCombo();
            if (noMods) {
                for (GameMod[] mods : allMods) {
                    OsuScore s = new OsuScore(Main.osu);
                    s.setBeatmapID(map.getID());
                    s.setEnabledMods(mods);
                    s.setMaxcombo(combo);
                    s.setScore(score);
                    s.setCountgeki(n320);
                    s.setCount300(n300);
                    s.setCountkatu(n200);
                    s.setCount100(n100);
                    s.setCount50(n50);
                    s.setCountmiss(nM);
                    scores.add(s);
                }
            } else {
                OsuScore s = new OsuScore(Main.osu);
                s.setBeatmapID(map.getID());
                s.setEnabledMods(includedMods);
                s.setMaxcombo(combo);
                s.setScore(score);
                s.setCountgeki(n320);
                s.setCount300(n300);
                s.setCountkatu(n200);
                s.setCount100(n100);
                s.setCount50(n50);
                s.setCountmiss(nM);
                scores.add(s);
            }
        } else {
            osuscore = utilOsu.unchokeScore(osuscore, map.getMaxCombo(), map.getMode(), FileInteractor.countTotalObjects(map.getID()));
            scores.add(osuscore);
        }

        new BotMessage(event, BotMessage.MessageType.SIMULATE).map(map).osuscores(scores).buildAndSend();
    }

    protected OsuScore getScore() {
        return null;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "simulate -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "simulate[number] [beatmap url or beatmap id] [+<nm/hd/nfeznc/...>]"
                        + " [-a <accuracy>] [-c <combo>] [-x/-m <amount misses>] [-s <score>] [-320 <amount 320s>] [-300 <amount 300s>] [-200 <amount 200s>] [-100 <amount 100s>] [-50 <amount 50s>]`"
                        + " to make me calculate the pp of the specified score on the map, defaults to SS score."
                        + "\nFor mania scores, only the score value matters so don't bother adding acc, misses, 320s, ..."
                        + "\nIf a number is specified and no beatmap, e.g. `" + statics.prefix + "simulate8`, I will skip the most recent 7 score embeds "
                        + "and choose the 8-th score embed, defaults to 1."
                        + "\nWith `+` you can choose mods, e.g. `+ezhddt`."
                        + "\nIf no mods are specified, I will simulate for the mods NM, HD, HR, DT, and HDDT."
                        + "\nBeatmap urls from both the new and old website are supported."
                        + "\nIf no beatmap is specified, I will search the channel's history for scores instead and consider the map of the [number]-th score, default to 1.";
            default:
                return help(0);
        }
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
                            if (--number <= 0)
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
        return "-1";    // never reached
    }

    @Override
    public cmdSimulateMap setNumber(int number) {
        this.number = number;
        return this;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
