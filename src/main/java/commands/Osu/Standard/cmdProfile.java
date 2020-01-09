package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.*;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import main.java.util.utilOsu;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class cmdProfile implements ICommand {

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
        // Get name either from arguments or from database link
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
        // Retrieve osu user data
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("User `" + name + "` was not found").queue();
            return;
        }
        // Retrieve top plays of user and global leaderboard of map
        List<OsuScore> topPlays;
        try {
            topPlays = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
            return;
        }
        List<OsuBeatmap> maps;
        try {
            maps = utilOsu.getBeatmaps(topPlays);
        } catch (ClassNotFoundException | SQLException | OsuAPIException e) {
            event.getChannel().sendMessage("Some osu! API issue, blame bade").queue();
            return;
        }
        new BotMessage(event.getChannel(), BotMessage.MessageType.PROFILE)
                .user(user)
                .osuscores(topPlays)
                .maps(maps)
                .mode(getMode())
                .buildAndSend();
    }

    public GameMode getMode() {
        return GameMode.STANDARD;
    }

    public String getName() {
        return "osu";
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + getName() + " [osu name]` to make me respond with stats about the player's " + getName() + " profile."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "Either specify an osu name or link your discord to an osu profile via `" + statics.prefix + "link <osu name>`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
