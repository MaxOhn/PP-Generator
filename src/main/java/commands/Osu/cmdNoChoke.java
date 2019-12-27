package main.java.commands.Osu;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointUserBests;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.commands.ICommand;
import main.java.core.*;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/*
    Calculate the top 5 of a user if all scores of their top score list would be no-chokes i.e. remove misses and make them fullcombo
 */
public class cmdNoChoke implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        // Parse name either from args or from database link
        String name;
        if (args.length == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
        } else {
            List<String> argsList = Arrays.stream(args)
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toList());
            name = String.join(" ", argsList);
        }
        // Check if name is given as mention
        if (event.isFromType(ChannelType.TEXT) && event.getMessage().getMentionedMembers().size() > 0) {
            name = Main.discLink.getOsu(event.getMessage().getMentionedMembers().get(0).getUser().getId());
            if (name == null) {
                event.getChannel().sendMessage("The mentioned user is not linked, I don't know who you mean").queue();
                return;
            }
        }
        final String oName = name;
        // Retrieve osu user data
        OsuUser user;
        try {
            user = Main.osu.users.query(
                    new EndpointUsers.ArgumentsBuilder(name).setMode(GameMode.STANDARD).build()
            );
        } catch (Exception e) {
            event.getChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        event.getChannel().sendMessage("Gathering data for `" + user.getUsername() + "`, I'll ping you once I'm done").queue(message -> {
            try {
                int currScore = 0;  // score index
                double ppThreshold = 0;
                // Retrieve top scores of user
                ArrayList<OsuScore> scoresList = new ArrayList<>(Main.osu.userBests.query(
                        new EndpointUserBests.ArgumentsBuilder(oName).setMode(GameMode.STANDARD).setLimit(100).build()
                ));
                Performance p = new Performance();
                ArrayList<OsuBeatmap> maps = new ArrayList<>();
                for (OsuScore score : scoresList) {
                    double progress = 100 * (double)currScore / scoresList.size();
                    // Display progress
                    if (progress > 6 && ThreadLocalRandom.current().nextInt(0, 6) > 4)
                        message.editMessage("Gathering data for `" + user.getUsername() + "`: "
                                + (int)progress + "%").queue();
                    // ppThreshold equals 94% of the pp of the 5th score
                    if (++currScore == 5) ppThreshold = score.getPp() * 0.94;
                    // Retrieve map of current score
                    OsuBeatmap map;
                    try {
                        if (!secrets.WITH_DB)
                            throw new SQLException();
                        map = DBProvider.getBeatmap(score.getBeatmapID());
                    } catch (SQLException | ClassNotFoundException e) {
                        try {
                            map = Main.osu.beatmaps.query(
                                    new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(score.getBeatmapID()).setLimit(1).setMode(GameMode.STANDARD).build()
                            ).get(0);
                            try {
                                if (secrets.WITH_DB)
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
                    // If the score comes after the 5th score, its combo is at least almost a fullcombo, and its pp is less than the threshold score, skip no-choke calculation
                    if (ppThreshold > 0 && score.getPp() < ppThreshold && comboRatio > 0.97) continue;
                    FileInteractor.prepareFiles(map);
                    // Save map as potential top-5 candidate
                    maps.add(map);
                    p.map(map).osuscore(score);
                    // Make it no-choke if required
                    if (p.getCombo() < p.getMaxCombo()) {
                        p.noChoke(300);
                        score.setCount300(p.getN300());
                        score.setCount100(p.getN100());
                        score.setCountmiss(p.getNMisses());
                        score.setMaxcombo(p.getCombo());
                        score.setPp((float) p.getPpDouble());
                        score.setRank(p.getRank());
                    }
                }
                // As pp values of the score list were modified, reorder them by pp value and take the top 5
                scoresList.sort(Comparator.comparing(OsuScore::getPp).reversed());
                List<OsuScore> scores = scoresList.subList(0, 5);
                // Save the maps of the top 5 scores
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
                // Create final message
                message.editMessage("Gathering data for `" + user.getUsername() + "`: 100%\nBuilding message...").queue();
                new BotMessage(event.getChannel(), BotMessage.MessageType.NOCHOKESCORES).author(event.getAuthor()).user(user)
                        .osuscores(scores).maps(maps).mode(GameMode.STANDARD).buildAndSend(() -> message.delete().queue());
            } catch (Exception e0) {
                event.getChannel().sendMessage("There was some problem, you might wanna retry later again and maybe ping bade or smth :p").queue();
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
