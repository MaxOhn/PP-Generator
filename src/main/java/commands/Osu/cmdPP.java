package main.java.commands.Osu;

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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/*
    Check what single score a user is missing to reach the given total pp
 */
public class cmdPP implements ICommand {
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
        // Check if its the proper argument
        double pp;
        try {
            pp = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(help(1)).queue();
            return;
        }
        if (pp < 0) {
            event.getChannel().sendMessage(help(2)).queue();
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
        if (event.getMessage().getMentionedMembers().size() > 0) {
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
        EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail("https://a.ppy.sh/" + user.getID());
        eb.setAuthor(user.getUsername() + ": "
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getPPRaw()) + "pp (#"
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getRank()) + " "
                        + user.getCountry()
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getCountryRank()) + ")",
                "https://osu.ppy.sh/u/" + user.getID(), "attachment://thumb.jpg");
        File flagIcon = new File(statics.flagPath + user.getCountry() + ".png");
        eb.setTitle("What score is missing for " + user.getUsername() + " to reach " + pp + "pp?");
        StringBuilder description = new StringBuilder();
        // pp too low
        if (user.getPPRaw() > pp) {
            description.append(user.getUsername()).append(" already has ").append(user.getPPRaw()).append("pp which is more than ")
                    .append(pp).append("pp.\nNo more scores are required.");
        } else {
            // Retrieve the top plays of a osu user
            List<OsuScore> topPlays;
            try {
                topPlays = user.getTopScores(100).get();
            } catch (OsuAPIException e) {
                event.getChannel().sendMessage("Could not retrieve top scores").queue();
                return;
            }
            double[] topPP = topPlays.stream().map(OsuScore::getPp).mapToDouble(elem -> elem).toArray();
            // Calculate how the pp value of the required score
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
                required = (required + (factor *= 0.95) * topPP[idx++]) / factor;
            if (size < 100)
                required -= topPP[size - 1] * Math.pow(0.95, size - 1);
            description.append("To reach ").append(pp).append("pp with one additional score, ").append(user.getUsername()).append(" needs to perform a **")
                    .append(round(required)).append("pp** score which would be the top #").append(++idx).append(".");
        }
        eb.setDescription(description);
        event.getChannel().sendMessage(eb.build()).addFile(flagIcon, "thumb.jpg").queue();
    }

    private double round(double num) {
        return Math.round(num * 100) / 100D;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "pp" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "pp" + getName() + " [number] [osu name]` to make me calculate what score "
                        + "is required for the player to have <number> total pp."
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "The first argument must be of the form `<number>` e.g. `321.45`, afterwards you can specify the name" + help;
            case 2:
                return "The number must be positive you clown :D" + help;
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
