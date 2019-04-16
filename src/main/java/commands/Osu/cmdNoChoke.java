package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointUserBests;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.core.Performance;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
        Logger logger = Logger.getLogger(this.getClass());
        String name;
        if (args.length == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return;
            }
        } else {
            List<String> argsList = Arrays.stream(args)
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toList());
            name = String.join(" ", argsList);
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(
                    new EndpointUsers.ArgumentsBuilder(name).setMode(GameMode.STANDARD).build()
            );
        } catch (Exception e) {
            event.getTextChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        event.getTextChannel().sendMessage("Gathering data for `" + user.getUsername() + "`, I'll ping you once I'm done").queue(message -> {
            try {
                int currScore = 0;
                double ppThreshold = 0;
                ArrayList<OsuScore> scoresList = new ArrayList<>(Main.osu.userBests.query(
                        new EndpointUserBests.ArgumentsBuilder(name).setMode(GameMode.STANDARD).setLimit(100).build()
                ));
                Performance p = new Performance();
                ArrayList<OsuBeatmap> maps = new ArrayList<>();
                for (OsuScore score : scoresList) {
                    double progress = 100 * (double)currScore / scoresList.size();
                    if (progress > 7 && ThreadLocalRandom.current().nextInt(0, 6) > 4)
                        message.editMessage("Gathering data for `" + user.getUsername() + "`: "
                                + (int)progress + "%").queue();
                    if (++currScore == 5) ppThreshold = score.getPp() * 0.94;
                    OsuBeatmap map;
                    try {
                        map = DBProvider.getBeatmap(score.getBeatmapID());
                    } catch (SQLException | ClassNotFoundException e) {
                        try {
                            map = Main.osu.beatmaps.query(
                                    new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(score.getBeatmapID()).setLimit(1).setMode(GameMode.STANDARD).build()
                            ).get(0);
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
                    double comboRatio = (double)score.getMaxCombo()/map.getMaxCombo();
                    if (ppThreshold > 0 && score.getPp() < ppThreshold && comboRatio > 0.97) continue;
                    Main.fileInteractor.prepareFiles(map);
                    maps.add(map);
                    p.map(map).osuscore(score).noChoke();
                    score.setCount300(p.getN300());
                    score.setCount100(p.getN100());
                    score.setCountmiss(p.getNMisses());
                    score.setMaxcombo(p.getCombo());
                    score.setPp((float)p.getPpDouble());
                    score.setRank(p.getRank());
                }
                scoresList.sort(Comparator.comparing(OsuScore::getPp).reversed());
                Collection<OsuScore> scores = scoresList.subList(0, 5);
                ArrayList<OsuBeatmap> finalMaps = new ArrayList<>();
                for (OsuScore s : scores) {
                    for (OsuBeatmap m : maps) {
                        if (s.getBeatmapID() == m.getID()) {
                            finalMaps.add(m);
                            break;
                        }
                    }
                }
                maps = finalMaps;
                message.editMessage("Gathering data for `" + user.getUsername() + "`: 100%").queue();
                new BotMessage(event, BotMessage.MessageType.NOCHOKESCORES).user(user).osuscores(scores).maps(maps)
                        .mode(GameMode.STANDARD).buildAndSend(() -> message.delete().queue());
                logger.info(String.format("[%s] %s: %s", event.getGuild().getName(),
                        "Finished command: " + event.getAuthor().getName(), event.getMessage().getContentRaw()));
            } catch (Exception e0) {
                event.getTextChannel().sendMessage("There was some problem, you might wanna retry later again and maybe"
                + " ping bade or smth :p").queue();
                e0.printStackTrace();
            }
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
