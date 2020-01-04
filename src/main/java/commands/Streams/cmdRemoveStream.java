package main.java.commands.Streams;

import main.java.commands.PrivilegedCommand;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;

/*
    Remove streamer - channel combination so that the bot longer notifies in the channel when the streamer comes online
 */
public class cmdRemoveStream extends PrivilegedCommand {

    @Override
    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        if (args.length != 2) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        // Platform must be specified
        if (!args[0].equals("twitch") && !args[0].equals("mixer")) {
            event.getTextChannel().sendMessage(help(2)).queue();
            return;
        }
        if (Main.streamHook.removeStreamer(args[1], event.getTextChannel().getId(), args[0]))
            event.getTextChannel().sendMessage("I'm no longer tracking `" + args[1] + "`'s " + args[0] + "stream.").queue();
        else
            event.getTextChannel().sendMessage("Could not remove `" + args[1] + "`'s stream. Is the name being tracked in this channel?").queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "addstream -h` for more help)";
        String roles = secrets.WITH_DB ? "smth went wrong, ping bade or smth" : String.join(", ", statics.authorities);
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles:");
            e.printStackTrace();
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "removestream twitch/mixer <stream name>` to make me remove the name from the stream-tracking list" +
                        "\nUsing this command requires either the admin permission or one of these roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "The first argument must either be `twitch` or `mixer`, the second one must be the name of the stream" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TWITCH;
    }
}
