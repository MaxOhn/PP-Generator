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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class cmdReach implements ICommand {
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
        List<String> argList = Arrays.stream(args)
                .filter(arg -> !arg.isEmpty())
                .collect(Collectors.toList());
        LinkedList<String> names = new LinkedList<>();

        // Get the names inbetween quotes
        if (argList.stream().anyMatch(w -> w.contains("\""))) {
            String argString = String.join(" ", args);
            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(argString);
            while (m.find()) {
                names.add(m.group(1));
                argString = argString.replace("\"" + m.group(1) + "\"", "");
            }
            argList = Arrays.stream(argString.split(" "))
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toList());
        }

        // If names not yet found, get them as single words now
        while (!argList.isEmpty()) {
            names.addLast(argList.get(0));
            argList.remove(0);
        }if (names.size() == 0) {
            event.getChannel().sendMessage(help(4)).queue();
            return;
        } else if (names.size() == 1) {
            String n = Main.discLink.getOsu(event.getAuthor().getId());
            if (n == null) {
                event.getChannel().sendMessage(help(1)).queue();
                return;
            }
            names.addFirst(n);
        }
        if (names.size() > 2) {
            event.getChannel().sendMessage(help(2)).queue();
            return;
        }
        names = names.stream().map(name ->
                name.startsWith("<@") && name.endsWith(">")
                    ? Main.discLink.getOsu(name.replaceAll("[<>@!]", ""))
                    : name
        ).collect(Collectors.toCollection(LinkedList::new));
        if (names.stream().anyMatch(Objects::isNull)) {
            event.getChannel().sendMessage("A mentioned user is not linked, I don't know who you mean").queue();
            return;
        }

        // Retrieve osu user data
        OsuUser user1;
        try {
            user1 = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(names.get(0)).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("No osu! user `" + names.get(0) + "` was found").queue();
            return;
        }
        OsuUser user2;
        try {
            user2 = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(names.get(1)).setMode(getMode()).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("No osu! user `" + names.get(1) + "` was found").queue();
            return;
        }
        // Prepare the message
        EmbedBuilder eb = new EmbedBuilder()
                .setThumbnail("https://a.ppy.sh/" + user1.getID())
                .setAuthor(user1.getUsername() + ": "
                                + NumberFormat.getNumberInstance(Locale.US).format(user1.getPPRaw()) + "pp (#"
                                + NumberFormat.getNumberInstance(Locale.US).format(user1.getRank()) + " "
                                + user1.getCountry()
                                + NumberFormat.getNumberInstance(Locale.US).format(user1.getCountryRank()) + ")",
                        "https://osu.ppy.sh/u/" + user1.getID(), "attachment://thumb.jpg")
                //.setTitle("How many pp is " + user1.getUsername() + " missing to reach rank " + rankPrefix + NumberFormat.getNumberInstance(Locale.US).format(rank) + "?");
                .setTitle("How many pp is " + user1.getUsername() + " missing to reach " + user2.getUsername() + "?");
        File flagIcon = new File(statics.flagPath + user1.getCountry() + ".png");
        StringBuilder description = new StringBuilder();
        if (user1.getPPRaw() > user2.getPPRaw()) {
            description.append(user1.getUsername()).append(" is already **")
                    .append(round(user1.getPPRaw() - user2.getPPRaw())).append("pp** above ")
                    .append(user2.getUsername()).append("'s **").append(user2.getPPRaw()).append("pp**.");
        } else {
            double pp = round(user2.getPPRaw());
            // Retrieve the top plays of osu user
            List<OsuScore> topPlays;
            try {
                topPlays = user1.getTopScores(100).get();
            } catch (OsuAPIException e) {
                event.getChannel().sendMessage("Could not retrieve top scores").queue();
                return;
            }
            double[] topPP = topPlays.stream().map(OsuScore::getPp).mapToDouble(elem -> elem).toArray();
            // Calculate how the pp value of the required score
            int size = topPP.length, idx = size - 1;
            double factor = Math.pow(0.95, idx), top = user1.getPPRaw(), bot = 0, current = topPP[idx];
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
            description.append(user1.getUsername()).append(" is missing **").append(round(pp - user1.getPPRaw()))
                    .append("pp** to reach ").append(user2.getUsername()).append("'s **").append(user2.getPPRaw())
                    .append("pp**, achievable by a single score worth **").append(round(required)).append("pp**.");
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
        String help = " (`" + statics.prefix + "reach" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "reach" + getName() + " <osu name 1> [osu name 2]` to make me calculate how many more pp "
                        + "the first user needs to reach the second user."
                        + "\nIf you're not linked via `" + statics.prefix + "link <osu name>`, you must specify at least two names, otherwise"
                        + "I compare your linked account with the specified name."
                        + "\n**User names that contain spaces must be encapsulated with \"** e.g. \"nathan on osu\"";
            case 1:
                return "Arguments must contain at least one osu username." + help;
            case 2:
                return "Arguments must contain at most two osu usernames. Maybe try encapsulating the names with \"" + help;
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
