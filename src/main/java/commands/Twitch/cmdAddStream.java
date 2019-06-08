package main.java.commands.Twitch;

import main.java.commands.PrivilegedCommand;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;

public class cmdAddStream extends PrivilegedCommand {

    @Override
    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        if ((args.length < 1 || args.length > 3)) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String name = "";
        if (args[0].equals("-l") || args[0].equals("-link")) {
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
            event.getTextChannel().sendMessage(help(3)).queue();
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
        String help = " (`" + statics.prefix + "addstream -h` for more help)";
        String roles = secrets.WITH_DB ? "smth went wrong, ping bade or smth" : String.join(", ", statics.authorities);
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "addstream <twitch name>` or `" + statics.prefix + "addstream -link <link to twitch stream>`" +
                        "to make me respond whenever the stream comes online\nUsing this command requires either the admin " + "" +
                        "permission or one of these roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "The stream link should be of the form `https://www.twitch.tv/<twitch name>`" + help;
            case 3:
                return "User is already being tracked in this channel!" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TWITCH;
    }
}
