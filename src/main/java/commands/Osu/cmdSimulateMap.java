package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_strToInt;

/*
    Simulated a score on a map, be it the best possible score or with given hitresults / acc / ...
 */
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
            event.getChannel().sendMessage(help(0)).queue();
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
        boolean specifiedMods = false;
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
            specifiedMods = true;
            argList.remove(mIdx);
        }
        // Retrieve hit object parameters from argument list
        double acc = -1;
        int combo = 0, nM = 0, score = 0, n320 = 0, n300 = 0, n200 = 0, n100 = 0, n50 = 0;
        boolean withParameters = false;

        if ((mIdx = argList.indexOf("-a")) != -1 || (mIdx = argList.indexOf("-acc")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                acc = Double.parseDouble(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-a` must come a decimal number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-c")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                combo = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-c` must come an integer number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-x")) != -1 || (mIdx = argList.indexOf("-m")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                nM = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-x`/`-m` must come an integer number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-s")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                score = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-s` must come an integer number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-300")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                n300 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-300` must come an integer number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-100")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                n100 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-100` must come an integer number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if ((mIdx = argList.indexOf("-50")) != -1) {
            withParameters = true;
            try {
                if (argList.size() < mIdx + 2)
                    throw new NumberFormatException();
                n50 = Integer.parseInt(argList.get(mIdx + 1));
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("After `-50` must come an integer number!").queue();
                return;
            }
            argList.remove(mIdx);
            argList.remove(mIdx);
        }
        if (acc < -1 || acc > 100 || combo < 0 || score < 0 || nM < 0 || n300 < 0 || n100 < 0 || n50 < 0 ) {
            event.getChannel().sendMessage("Invalid hit results. Acc must be between 0 and 100, everything else must be non-negative.").queue();
            return;
        }
        // Retrieve map
        OsuScore osuscore = retrieveScore(event, argList);
        if (osuscore == null)
            return;
        OsuBeatmap map;
        try {
            if (!secrets.WITH_DB)
                throw new SQLException();
            map = DBProvider.getBeatmap(osuscore.getBeatmapID());
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = osuscore.getBeatmap().get();
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
        // Check parameter constrains
        int hitSum = 0;
        FileInteractor.prepareFiles(map);
        int nTotal = FileInteractor.countTotalObjects(map.getID());
        // Create simulated score
        switch (map.getMode()) {
            case STANDARD: {
                hitSum = n300 + n100 + n50 + nM;
                if (hitSum > nTotal) {
                    event.getChannel().sendMessage("The map has only " + nTotal + " objects, you gave too many (" + hitSum + ") in the parameters").queue();
                    return;
                }
                if (hitSum == 0 && acc == -1)
                    n300 = nTotal;
                // Handle specified parameters if there are any
                if (withParameters) {
                    if (acc > 0) {
                        HashMap<String, Integer> hitresults = utilOsu.getHitResults(map.getMode(), acc, nTotal, 0, n300, 0, n100, n50, nM);
                        n300 = hitresults.get("n300");
                        n100 = hitresults.get("n100");
                        n50 = hitresults.get("n50");
                        nM = hitresults.get("nM");
                        if (combo == 0)
                            combo = map.getMaxCombo();
                    } else if (combo > 0) {
                        if (nM == 0)
                            nM = map.getMaxCombo() / combo;
                        if (n300 == 0)
                            n300 = nTotal - nM;
                        else if (n100 == 0)
                            n100 = nTotal - n300 - nM;
                        else
                            n50 = nTotal - n300 - n100 - nM;
                    } else {
                        combo = map.getMaxCombo();
                        if (n300 == 0)
                            n300 = nTotal - n100 - n50 - nM;
                        else if (n100 == 0)
                            n100 = nTotal - n300 - n50 - nM;
                        else
                            n50 = nTotal - n300 - n100 - nM;
                    }
                    osuscore.setCount300(n300);
                    osuscore.setCount100(n100);
                    osuscore.setCount50(n50);
                    osuscore.setCountmiss(nM);
                    osuscore.setMaxcombo(combo);
                // Otherwise just unchoke the score
                } else
                    utilOsu.unchokeScore(osuscore, map.getMaxCombo(), map.getMode(), FileInteractor.countTotalObjects(map.getID()), 300);
                if (specifiedMods)
                    osuscore.setEnabledMods(includedMods);
                break;
            }
            case MANIA: {
                if (score > 1_000_000) {
                    event.getChannel().sendMessage("The score must be between 0 and 1,000,000 on mania maps").queue();
                    return;
                }
                if (score == 0)
                    score = 1_000_000;
                // Only care about score but its nicer to see proper hitresults in the message
                if (acc == -1)
                    n320 = nTotal - n300 - n200 - n100 - n50 - nM;
                else {
                    HashMap<String, Integer> hitresults = utilOsu.getHitResults(map.getMode(), acc, nTotal, n320, n300, n200, n100, n50, nM);
                    n320 = hitresults.get("n320");
                    n300 = hitresults.get("n300");
                    n200 = hitresults.get("n200");
                    n100 = hitresults.get("n100");
                    n50 = hitresults.get("n50");
                    nM = hitresults.get("nM");
                }
                osuscore.setScore(score);
                osuscore.setCountgeki(n320);
                osuscore.setCount300(n300);
                osuscore.setCountkatu(n200);
                osuscore.setCount100(n100);
                osuscore.setCount50(n50);
                osuscore.setCountmiss(nM);
                osuscore.setMaxcombo(combo);
                break;
            }
            case TAIKO: {
                event.getChannel().sendMessage("Not available for taiko :(").queue();
                return;
            }
            default:
                event.getChannel().sendMessage("Mode not yet supported :(").queue();
                return;
        }
        // Create the message
        new BotMessage(event.getChannel(), BotMessage.MessageType.SIMULATE).map(map).osuscore(osuscore).buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "simulate -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "simulate[number] [beatmap url or beatmap id] [+<nm/hd/nfeznc/...>]"
                        + " [-a <accuracy>] [-c <combo>] [-x/-m <amount misses>] [-s <score>] [-300 <amount 300s>] [-100 <amount 100s>] [-50 <amount 50s>]`"
                        + " to make me calculate the pp of the specified score on the map, defaults to SS score."
                        + "\nFor mania scores, only the score value matters so don't bother adding acc, misses, 300s, ..."
                        + "\nIf a number is specified and no beatmap, e.g. `" + statics.prefix + "simulate8`, I will skip the most recent 7 score embeds "
                        + "\nIf no beatmap is specified, I will search the channel's history for scores instead and consider the map of the [number]-th score, default to 1."
                        + "and choose the 8-th score embed, defaults to 1."
                        + "\nWith `+` you can choose mods, e.g. `+ezhddt`."
                        + "\nIf no mods are specified, I will simulate for the mods NM, HD, HR, DT, and HDDT."
                        + "\nBeatmap urls from both the new and old website are supported.";
            default:
                return help(0);
        }
    }

    protected OsuScore retrieveScore(MessageReceivedEvent event, List<String> argList) {
        if (argList.size() > 0) {
            OsuScore newScore = new OsuScore(Main.osu);
            newScore.setBeatmapID(Integer.parseInt(utilOsu.getIdFromString(argList.get(0))));
            return newScore;
        } else {
            int counter = 100;
            for (Message msg: (event.isFromType(ChannelType.PRIVATE) ? event.getChannel() : event.getTextChannel()).getIterableHistory()) {
                if (msg.getAuthor().equals(event.getJDA().getSelfUser()) && msg.getEmbeds().size() > 0) {
                    MessageEmbed msgEmbed = msg.getEmbeds().iterator().next();
                    List<MessageEmbed.Field> fields = msgEmbed.getFields();
                    if (!msg.getContentRaw().contains("Simulated") && fields.size() > 0 && fields.get(0).getValue().matches(".*\\{( ?\\d+ ?/){2,} ?\\d+ ?}.*")) {
                            if (--number <= 0) {
                                OsuScore newScore = new OsuScore(Main.osu);
                                newScore.setBeatmapID(Integer.parseInt(msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/") + 1)));
                                Pattern p = Pattern.compile(".*\\[ \\*\\*(\\d*)x\\*\\*/\\d*x ]\\t \\{ (\\d*) / (\\d*) / (\\d*)( / (\\d*)( / \\d* / (\\d*))?)? }");
                                Matcher matcher = p.matcher(fields.get(0).getValue());
                                if (matcher.find()) {
                                    newScore.setMaxcombo(Integer.parseInt(matcher.group(1)));
                                    if (matcher.group(6) != null) {
                                        newScore.setCount300(Integer.parseInt(matcher.group(2)));
                                        newScore.setCount100(Integer.parseInt(matcher.group(3)));
                                        newScore.setCount50(Integer.parseInt(matcher.group(4)));
                                        newScore.setCountmiss(Integer.parseInt(matcher.group(6)));
                                    } else {
                                        newScore.setCount300(Integer.parseInt(matcher.group(2)));
                                        newScore.setCount100(Integer.parseInt(matcher.group(3)));
                                        newScore.setCountmiss(Integer.parseInt(matcher.group(4)));
                                    }
                                }
                                int plusIdx = fields.get(0).getName().indexOf("+");
                                if (plusIdx == -1)
                                    newScore.setEnabledMods(new GameMod[0]);
                                else
                                    newScore.setEnabledMods(GameMod.get(utilOsu.mods_strToInt(fields.get(0).getName().substring(plusIdx + 1).split("\\t")[0])));
                                return newScore;
                            }
                    } else if (fields.size() >= 5 && fields.get(5).getValue().matches(".*\\{( ?\\d+ ?/){2,} ?\\d+ ?}.*")) {
                        if (--number <= 0) {
                            OsuScore newScore = new OsuScore(Main.osu);
                            newScore.setBeatmapID(Integer.parseInt(msgEmbed.getUrl().substring(msgEmbed.getUrl().lastIndexOf("/") + 1)));
                            newScore.setMaxcombo(Integer.parseInt(fields.get(4).getValue().split("x\\*\\*/")[0].substring(2)));
                            Pattern p = Pattern.compile("\\{ (\\d*) / (\\d*) / (\\d*)( / (\\d*)( / \\d* / (\\d*))?)? }");
                            Matcher matcher = p.matcher(fields.get(5).getValue());
                            if (matcher.find()) {
                                if (matcher.group(5) != null) {
                                    newScore.setCount300(Integer.parseInt(matcher.group(1)));
                                    newScore.setCount100(Integer.parseInt(matcher.group(2)));
                                    newScore.setCount50(Integer.parseInt(matcher.group(3)));
                                    newScore.setCountmiss(Integer.parseInt(matcher.group(5)));
                                } else {
                                    newScore.setCount300(Integer.parseInt(matcher.group(1)));
                                    newScore.setCount100(Integer.parseInt(matcher.group(2)));
                                    newScore.setCountmiss(Integer.parseInt(matcher.group(3)));
                                }
                            }
                            int plusIdx = fields.get(0).getValue().indexOf("+");
                            if (plusIdx == -1)
                                newScore.setEnabledMods(new GameMod[0]);
                            else
                                newScore.setEnabledMods(GameMod.get(utilOsu.mods_strToInt(fields.get(0).getValue().substring(plusIdx + 1))));
                            return newScore;
                        }
                    }
                }
                if (--counter == 0) {
                    event.getChannel().sendMessage("Could not find last score embed, must be too old").queue();
                    return null;
                }
            }
        }
        return null;    // never reached
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
