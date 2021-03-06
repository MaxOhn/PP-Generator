package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import main.java.util.utilOsu;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/*
    Display a score of a users top score list that was recently made
 */
public class cmdRecentBest implements INumberedCommand {

    private int number = 0;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        number = 0;
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (number > 100) {
            event.getChannel().sendMessage("The number must be between 1 and 100").queue();
            return;
        }
        ArrayList<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        // Get name either from arguments or from database link
        String name;
        if (argList.size() == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
        } else if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getChannel().sendMessage(help(0)).queue();
            return;
        } else {
            name = String.join(" ", argList);
        }
        // Check if name is given as mention
        if (event.isFromType(ChannelType.TEXT) && event.getMessage().getMentionedMembers().size() > 0) {
            name = Main.discLink.getOsu(event.getMessage().getMentionedMembers().get(0).getUser().getId());
            if (name == null) {
                event.getChannel().sendMessage("The mentioned user is not linked, I don't know who you mean").queue();
                return;
            }
        }
        // Retrieve osu user data
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("`" + name + "` was not found").queue();
            return;
        }
        // Retrieve user's top scores
        List<OsuScore> topPlays;
        try {
            topPlays = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        // Sort scores by date
        List<OsuScore> topPlaysByDate = new ArrayList<>(topPlays);
        topPlaysByDate.sort(Comparator.comparing(OsuScore::getDate).reversed());
        if (number > 0) {
            final Iterator<OsuScore> itr = topPlaysByDate.iterator();
            OsuScore rbScore = itr.next();
            // Get the appropriate score
            while (itr.hasNext() && --number > 0)
                rbScore = itr.next();
            // Retrieve the score's map
            OsuBeatmap map;
            try {
                map = utilOsu.getBeatmap(rbScore.getBeatmapID());
            } catch (OsuAPIException e) {
                event.getChannel().sendMessage("Some osu! API issue, blame bade").queue();
                return;
            }
            // Retrieve the global leaderboard of the map
            Collection<OsuScore> globalPlays;
            try {
                globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).build());
            } catch (OsuAPIException e) {
                event.getChannel().sendMessage("Could not retrieve global scores").queue();
                return;
            }
            // Construct the message
            new BotMessage(event.getChannel(), BotMessage.MessageType.RECENTBEST).user(user).map(map).osuscore(rbScore).mode(getMode())
                    .topplays(topPlays, globalPlays).buildAndSend();
        } else {
            topPlaysByDate = topPlaysByDate.stream().limit(5).collect(Collectors.toList());
            List<OsuBeatmap> maps = new ArrayList<>();
            Map<Integer, OsuBeatmap> allMaps;
            try {
                allMaps = utilOsu.getBeatmaps(topPlays).stream().collect(Collectors.toMap(OsuBeatmap::getID, m -> m));
            } catch (SQLException | ClassNotFoundException | OsuAPIException e) {
                event.getChannel().sendMessage("Something went wrong, blame bade").queue();
                logger.error("Error while retrieving maps in bulk: ", e);
                return;
            }
            LinkedList<Integer> indices = new LinkedList<>();
            for (OsuScore s : topPlaysByDate) {
                indices.addLast(topPlays.indexOf(s) + 1);
                maps.add(allMaps.get(s.getBeatmapID()));
            }
            new BotMessage(event.getChannel(), BotMessage.MessageType.RECENTBESTS).user(user)
                    .osuscores(topPlaysByDate).indices(indices).maps(maps).mode(getMode()).buildAndSend();
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "recentbest -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "recentbest" + getName() + "[number] [osu name]` "
                        + "to make me respond with the users selected best recent performance."
                        + "\nIf a number is specified, e.g. `" + statics.prefix + "rb" + getName() + "8`, I will skip the most recent 7 top scores "
                        + "and show the 8-th score, defaults to 1."
                        + "\nIf no player name specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
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

    @Override
    public cmdRecentBest setNumber(int number) {
        this.number = number;
        return this;
    }

    public String getName() {
        return "";
    }

    public GameMode getMode() {
        return GameMode.STANDARD;
    }
}
