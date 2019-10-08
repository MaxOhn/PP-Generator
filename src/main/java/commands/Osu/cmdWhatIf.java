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

public class cmdWhatIf implements ICommand {

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
        if (!args[0].startsWith("+")) {
            event.getChannel().sendMessage(help(1)).queue();
            return;
        }
        double pp;
        try {
            pp = Double.parseDouble(args[0].substring(1));
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(help(1)).queue();
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
        List<OsuScore> topPlays;
        try {
            topPlays = user.getTopScores(100).get();
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Could not retrieve top scores").queue();
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
        eb.setTitle("What if " + user.getUsername() + " got a new " + pp + "pp score?");
        StringBuilder description = new StringBuilder();
        if (pp < topPlays.get(topPlays.size() - 1).getPp()) {
            description.append("A ").append(pp).append("pp play wouldn't even be in ").append(user.getUsername())
                    .append("'s top 100 plays.\nThere would not be any significant pp change.");
        } else {
            double actual = 0, factor = 1;
            for (OsuScore score : topPlays) {
                actual += score.getPp() * factor;
                factor *= 0.95;
            }
            double bonus = user.getPPRaw() - actual, potential = 0;
            boolean used = false;
            int newPos = -1;
            factor = 1;
            for (int i = 0; i < topPlays.size() - 1; i++) {
                if (!used && topPlays.get(i).getPp() < pp) {
                    used = true;
                    potential += pp * factor;
                    factor *= 0.95;
                    newPos = i + 1;
                }
                potential += topPlays.get(i).getPp() * factor;
                factor *= 0.95;
            }
            description.append("A ").append(pp).append("pp play would be ").append(user.getUsername()).append("'s #")
                    .append(newPos).append(" best play.\nTheir pp would change by **+")
                    .append(round(potential + bonus - user.getPPRaw())).append("** to **")
                    .append(round(potential + bonus)).append("pp**.");
        }
        eb.setDescription(description);
        event.getChannel().sendMessage(eb.build()).addFile(flagIcon, "thumb.jpg").queue();
    }

    private double round(double num) {
        return Math.round(num * 100) / 100D;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "whatif" + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "whatif";
            case 1:
                return "The first argument must be of the form `+<number>` e.g. `+321.45`, afterwards you can specify the name" + help;
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
