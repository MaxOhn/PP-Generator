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
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
        int rank;
        try {
            rank = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(help(1)).queue();
            return;
        }
        if (rank < 0) {
            event.getChannel().sendMessage(help(2)).queue();
            return;
        } else if (rank > 10000) {
            event.getChannel().sendMessage(help(3)).queue();
            return;
        }
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
        if (name.startsWith("<@") && name.endsWith(">")) {
            name = Main.discLink.getOsu(name.substring(2, name.length()-1));
            if (name == null) {
                event.getChannel().sendMessage("The mentioned user is not linked, I don't know who you mean").queue();
                return;
            }
        }
        OsuUser user;
        try {
            user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("No osu! user `" + name + "` was found").queue();
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail("https://a.ppy.sh/" + user.getID());
        eb.setAuthor(user.getUsername() + ": "
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getPPRaw()) + "pp (#"
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getRank()) + " "
                        + user.getCountry()
                        + NumberFormat.getNumberInstance(Locale.US).format(user.getCountryRank()) + ")",
                "https://osu.ppy.sh/u/" + user.getID(), "attachment://thumb.jpg");
        File flagIcon = new File(statics.flagPath + user.getCountry() + ".png");
        eb.setTitle("How many pp is " + user.getUsername() + " missing to reach rank #" + NumberFormat.getNumberInstance(Locale.US).format(rank) + "?");
        StringBuilder description = new StringBuilder();
        if (user.getRank() <= rank) {
            description.append(user.getUsername()).append(" already has rank #").append(NumberFormat.getNumberInstance(Locale.US).format(user.getRank()))
                    .append(" and is thus already above rank #")
                    .append(NumberFormat.getNumberInstance(Locale.US).format(rank)).append(".\nNo more pp are required.");
        } else {
            double pp;
            try {
                pp = Main.customOsu.getPpOfRank(rank, getMode());
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not retrieve pp of rank", e);
                event.getChannel().sendMessage("Some thing went wrong, blame bade").queue();
                return;
            }
            List<OsuScore> topPlays;
            try {
                topPlays = user.getTopScores(100).get();
            } catch (OsuAPIException e) {
                event.getChannel().sendMessage("Could not retrieve top scores").queue();
                return;
            }
            double[] topPP = topPlays.stream().map(OsuScore::getPp).mapToDouble(elem -> elem).toArray();
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
            description.append("Rank #").append(NumberFormat.getNumberInstance(Locale.US).format(rank))
                    .append(" currently requires **").append(pp).append("pp**, so ").append(user.getUsername())
                    .append(" is missing **").append(round(pp - user.getPPRaw())).append("** raw pp, achievable by a single score worth **")
                    .append(round(required)).append("pp**.");
        }
        eb.setDescription(description);
        event.getChannel().sendMessage(eb.build()).addFile(flagIcon, "thumb.jpg").queue();
    }

    private double round(double num) {
        return Math.round(num * 100) / 100D;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "rank" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "rank" + getName() + " [number] [osu name]` to make me calculate how many more pp "
                        + "are required for the player to reach rank <number>.\n"
                        + "\nIf no player name is specified, your discord must be linked to an osu profile via `" + statics.prefix + "link <osu name>" + "`";
            case 1:
                return "The first argument must be an integer of the form `<number>` e.g. `321`, afterwards you can specify the name" + help;
            case 2:
                return "The number must be positive you clown :D" + help;
            case 3:
                return "I'm unfortunately only able to calculate the pp for ranks up to 10000 :(";
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
