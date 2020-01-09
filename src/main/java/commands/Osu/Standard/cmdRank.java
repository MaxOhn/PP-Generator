package main.java.commands.Osu.Standard;

import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/*
    Calculate how many pp a user is missing to achieve the given rank
 */
public class cmdRank implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("-help")) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        // Parse rank from arguments
        String country = "";
        int rank;
        try {
            rank = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            try {
                rank = Integer.parseInt(args[0].substring(2));
                country = args[0].substring(0, 2);
            } catch (NumberFormatException e1) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
        }
        if (rank < 1) {
            event.getChannel().sendMessage(help(2)).queue();
            return;
        } else if (rank > 10000) {
            event.getChannel().sendMessage(help(3)).queue();
            return;
        }
        // Get the name either from arguments or from database link
        String name;
        if (args.length == 1) {
            name = Main.discLink.getOsu(event.getAuthor().getId());
            if (name == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
        } else {
            List<String> argsList = Arrays.stream(args)
                    .filter(arg -> !arg.isEmpty())
                    .skip(1)
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
            event.getChannel().sendMessage("No osu! user `" + name + "` was found").queue();
            return;
        }
        // Prepare the message
        String rankPrefix = country.isEmpty() ? "#" : country.toUpperCase();
        EmbedBuilder eb = new EmbedBuilder()
                .setThumbnail("https://a.ppy.sh/" + user.getID())
                .setAuthor(user.getUsername() + ": "
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getPPRaw()) + "pp (#"
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getRank()) + " "
                        + user.getCountry()
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + user.getID(), "attachment://thumb.jpg")
                .setTitle("How many pp is " + user.getUsername() + " missing to reach rank " + rankPrefix + NumberFormat.getNumberInstance(Locale.US).format(rank) + "?");
        File flagIcon = new File(statics.flagPath + user.getCountry() + ".png");
        StringBuilder description = new StringBuilder();
        if (country.isEmpty() && user.getRank() <= rank) {
            description.append(user.getUsername()).append(" already has rank ").append(rankPrefix)
                    .append(NumberFormat.getNumberInstance(Locale.US).format(user.getRank()))
                    .append(" and is thus already above rank ").append(rankPrefix)
                    .append(NumberFormat.getNumberInstance(Locale.US).format(rank)).append(".");
        } else {
            // Retrieve the user at the required rank
            OsuUser rankHolder;
            try {
                rankHolder = Main.osu.users.query(
                        new EndpointUsers.ArgumentsBuilder(Main.customOsu.getUserIdOfRank(rank, getMode(), country)).setMode(getMode()).build()
                );
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not retrieve user of rank", e);
                event.getChannel().sendMessage("Some thing went wrong, blame bade").queue();
                return;
            } catch (IllegalArgumentException e) {
                event.getChannel().sendMessage(help(4)).queue();
                return;
            }
            double pp = round(rankHolder.getPPRaw());
            if (user.getPPRaw() >= pp) {
                description.append("Rank ").append(rankPrefix).append(NumberFormat.getNumberInstance(Locale.US).format(rank))
                        .append(" is currently held by ").append(rankHolder.getUsername())
                        .append(" with **").append(round(pp)).append("pp**, so ").append(user.getUsername())
                        .append(" is with **").append(round(user.getPPRaw())).append("pp** already above that.");
            } else {
                // Retrieve the top plays of osu user
                List<OsuScore> topPlays;
                try {
                    topPlays = user.getTopScores(100).get();
                } catch (OsuAPIException e) {
                    event.getChannel().sendMessage("Could not retrieve top scores").queue();
                    return;
                }
                double[] topPP = topPlays.stream().map(OsuScore::getPp).mapToDouble(elem -> elem).toArray();
                // Calculate the pp value of the required score
                int size = topPP.length, idx = size - 1;
                double factor = Math.pow(0.95, idx), top = user.getPPRaw(), bot = 0, current = topPP[idx];
                for (; top + bot < pp; idx--) {
                    top -= current * factor;
                    if (idx == 0)
                        break;
                    current = topPP[idx - 1];
                    bot += current * factor;
                    factor /= 0.95;
                }
                double required = pp - top - bot;
                if (top + bot >= pp)
                    required = (required + (factor *= 0.95) * topPP[idx]) / factor;
                if (size < 100)
                    required -= topPP[size - 1] * Math.pow(0.95, size - 1);
                description.append("Rank ").append(rankPrefix).append(NumberFormat.getNumberInstance(Locale.US).format(rank))
                        .append(" is currently held by ").append(rankHolder.getUsername())
                        .append(" with **").append(pp).append("pp**, so ").append(user.getUsername())
                        .append(" is missing **").append(round(pp - user.getPPRaw())).append("** raw pp, achievable by a single score worth **")
                        .append(round(required)).append("pp**.");
            }
        }
        eb.setDescription(description)
                .setColor(Color.green);
        event.getChannel().sendMessage(eb.build()).addFile(flagIcon, "thumb.jpg").queue();
    }

    // Auxiliary rounding function
    private double round(double num) {
        return Math.round(num * 100) / 100D;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "rank" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "rank" + getName() + " [number / <country acronym>number] [osu name]` to make me calculate how many more pp "
                        + "are required for the player to reach rank <number>."
                        + "\nIf the number is specified with a country acronym, e.g. `be15`, I will consider the number as national rank instead of global rank."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "The first argument must either be a positive integer, e.g. `15`, or a country acronym followed by a positive integer, e.g. `be15`, afterwards you can specify the name" + help;
            case 2:
                return "The number must be positive you clown :D" + help;
            case 3:
                return "Unfortunately I'm only able to calculate the pp for ranks up to 10000 :(";
            case 4:
                return "Seems like you didn't give a valid country acronym. It must be something like `be`, `de`, `us`, `kr`, ..." + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }

    public String getName() {
        return "";
    }

    public GameMode getMode() {
        return GameMode.STANDARD;
    }
}
