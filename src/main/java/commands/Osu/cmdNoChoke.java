package main.java.commands.Osu;

import de.maxikg.osuapi.model.Beatmap;
import de.maxikg.osuapi.model.GameMode;
import de.maxikg.osuapi.model.User;
import de.maxikg.osuapi.model.UserScore;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.core.Performance;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class cmdNoChoke implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String name;
        if (args.length == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return;
            }
        } else
            name = String.join(" ", args);
        User user;
        try {
            user = Main.osu.getUserByUsername(name).mode(GameMode.STANDARD).query().iterator().next();
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        event.getTextChannel().sendMessage("Gathering data, I'll ping you once I'm done").queue(message -> {
            ArrayList<UserScore> scoresList = new ArrayList<>(Main.osu.getUserBestByUsername(name).mode(GameMode.STANDARD).limit(100).query());
            Performance p = new Performance();
            ArrayList<Beatmap> maps = new ArrayList<>();
            for (UserScore score : scoresList) {
                Beatmap map;
                try {
                    map = DBProvider.getBeatmap(score.getBeatmapId());
                } catch (SQLException | ClassNotFoundException e) {
                    try {
                        map = Main.osu.getBeatmaps().beatmapId(score.getBeatmapId()).limit(1).mode(GameMode.STANDARD).query().iterator().next();
                        try {
                            Thread.sleep(600);
                        } catch (InterruptedException ignored) {}
                        try {
                            DBProvider.addBeatmap(map);
                        } catch (ClassNotFoundException | SQLException e2) {
                            e2.printStackTrace();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        continue;
                    }
                }
                Main.fileInteractor.prepareFiles(map);
                maps.add(map);
                p.map(map).userscore(score).noChoke();
                if (ThreadLocalRandom.current().nextInt(0, 4) > 2)
                    message.editMessage("Gathering data: " + (int)(100 * (double)scoresList.indexOf(score) / scoresList.size()) + "%").queue();
                score.setCount300(p.getN300());
                score.setCountMiss(p.getNMisses());
                score.setMaxCombo(p.getCombo());
                score.setPp((float)p.getPpDouble());
                score.setRank(p.getRank());
            }
            scoresList.sort(Comparator.comparing(UserScore::getPp).reversed());
            Collection<UserScore> scores = scoresList.subList(0, 5);
            ArrayList<Beatmap> finalMaps = new ArrayList<>();
            for (UserScore s : scores) {
                for (Beatmap m : maps) {
                    if (s.getBeatmapId() == m.getBeatmapId()) {
                        finalMaps.add(m);
                        break;
                    }
                }
            }
            maps = finalMaps;
            message.delete().queue();
            new BotMessage(event, BotMessage.MessageType.NOCHOKESCORES).user(user).userscore(scores).maps(maps)
                    .mode(GameMode.STANDARD).buildAndSend();
        });
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "nochoke -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "nochoke [osu name]` to make me list the user's top 5 scores"
                        + " with only unchoked plays.\nIf no player name specified, your discord must be linked to an osu profile via `"
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
}
