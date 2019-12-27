package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointScores;
import com.oopsjpeg.osu4j.backend.EndpointUserRecents;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.INumberedCommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/*
    Display a recently performed score of a user
 */
public class cmdRecent implements INumberedCommand {

    private int number = 1;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        number = 1;
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (number > 50) {
            event.getChannel().sendMessage("The number must be between 1 and 50").queue();
            return;
        }
        // Get name either from arguments or from database link
        String name;
        if (args.length == 0) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
        } else if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getChannel().sendMessage(help(0)).queue();
            return;
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
        ArrayList<OsuScore> userRecents;
        OsuScore recent;
        OsuUser user;
        try {
            // Retrieve recent scores of user
            userRecents = new ArrayList<>(Main.osu.userRecents.query(
                    new EndpointUserRecents.ArgumentsBuilder(name).setMode(getMode()).setLimit(50).build())
            );
            // Get the appropriate score
            recent = userRecents.get(0);
            while (--number > 0 && userRecents.size() > 1) {
                userRecents.remove(0);
                recent = userRecents.get(0);
            }
            if (number > 0) {
                event.getChannel().sendMessage("User's recent history doesn't go that far back").queue();
                return;
            }
            // Retrieve osu user data
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(recent.getUserID()).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("`" + name + "` was not found or no recent plays").queue();
            return;
        }
        // Retrieve the score's map
        OsuBeatmap map;
        try {
            if (!secrets.WITH_DB)
                throw new SQLException();
            map = DBProvider.getBeatmap(recent.getBeatmapID());
        } catch (SQLException | ClassNotFoundException e) {
            try {
                map = Main.osu.beatmaps.query(
                        new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(recent.getBeatmapID()).setLimit(1).build()
                ).get(0);
            } catch (OsuAPIException e1) {
                event.getChannel().sendMessage("Could not retrieve beatmap id `" + recent.getBeatmapID() + "`").queue();
                return;
            }
            try {
                if (secrets.WITH_DB)
                    DBProvider.addBeatmap(map);
            } catch (ClassNotFoundException | SQLException e1) {
                e1.printStackTrace();
            }
        }
        // Retrieve top plays of user and global leaderboard of map
        Collection<OsuScore> topPlays;
        Collection<OsuScore> globalPlays;
        try {
            topPlays = map.getApproved() == ApprovalState.RANKED ? user.getTopScores(100).get() : new ArrayList<>();
            globalPlays = Main.osu.scores.query(new EndpointScores.ArgumentsBuilder(map.getID()).setMode(getMode()).build());
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        // Build the message
        new BotMessage(event.getChannel(), BotMessage.MessageType.RECENT)
                .user(user)
                .map(map)
                .osuscore(recent)
                .history(userRecents)
                .mode(getMode())
                .topplays(topPlays, globalPlays)
                .buildAndSend();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
              return "Enter `" + statics.prefix + getName() + "[number] [osu name]` to make me respond with info about the players last play."
                      + "\nIf a number is specified, e.g. `" + statics.prefix + getName() + "8`, I will skip the most recent 7 scores "
                      + "and show the 8-th recent score, defaults to 1."
                      + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
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
    public cmdRecent setNumber(int number) {
        this.number = number;
        return this;
    }

    public GameMode getMode() {
        return GameMode.STANDARD;
    }

    public String getName() {
        return "recent";
    }
}
