package main.java.commands;

import main.java.core.Main;
import main.java.core.TwitchHook;
import main.java.util.secrets;
import main.java.util.statics;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import static main.java.util.utilGeneral.isAuthority;

public class cmdAddStream implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length < 1 || args.length > 3) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        } else if (!isAuthority(event)) {
            event.getTextChannel().sendMessage(help(3)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (args[0].equals("--h") || args[0].equals("--help")) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }

        String name = "";
        if (args[0].equals("--l") || args[0].equals("--link")) {
            if (args.length  < 2) {
                event.getTextChannel().sendMessage(help(0)).queue();
                return;
            }
            if (args[1].matches("((https?:\\/\\/)?(www\\.)?)?twitch\\.tv\\/\\w+"))
                name =  args[1].substring(args[1].lastIndexOf("/"));
            else
                event.getTextChannel().sendMessage(help(2)).queue();
        } else
            name = args[0];
        if (Main.twitch.isTracked(name, event.getTextChannel().getId())) {
            event.getTextChannel().sendMessage(help(1)).queue();
            return;
        }
        if (Main.twitch.addStreamer(name, event.getTextChannel().getId()))
            event.getTextChannel().sendMessage("I'm now tracking `" + name + "`'s twitch stream.").queue();
        else {
            MessageBuilder builder = new MessageBuilder("Could not track `" + name +
                    "`'s stream for some reason, blame ").append(event.getGuild().getMemberById(secrets.badewanne3ID));
            event.getTextChannel().sendMessage(builder.build()).queue();
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "addstream --h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "addstream <twitch name>` or `" + statics.prefix + "addstream --link <link to twitch stream>`" +
                        "to make me respond whenever the stream comes online";
            case 1:
                return "User is already being tracked in this channel!" + help;
            case 2:
                return "The stream link should be of the form `https://www.twitch.tv/<twitch name>`" + help;
            case 3:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }
}
