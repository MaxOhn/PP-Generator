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

/*
    Add a streamer - channel combination to the database so that the bot sends a notification in that channel
    if the streamer comes online
 */
public class cmdAddStream extends PrivilegedCommand {

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
        // Must be specified whether the streamer is on twitch or mixer
        if (!args[0].equals("twitch") && !args[0].equals("mixer")) {
            event.getTextChannel().sendMessage(help(3)).queue();
            return;
        }
        // Check if streamer - channel combination already in database
        String name = args[1];
        if (Main.streamHook.isTracked(name, event.getTextChannel().getId())) {
            event.getTextChannel().sendMessage(help(2)).queue();
            return;
        }
        // Add streamer - channel combination to database
        if (Main.streamHook.addStreamer(name, event.getTextChannel().getId(), args[0]))
            event.getTextChannel().sendMessage("I'm now tracking `" + name + "`'s " + args[0] + " stream.").queue();
        else {
            MessageBuilder builder = new MessageBuilder("Could not track `" + name + "`'s stream for some reason, blame bade");
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
            logger.error("Error while retrieving authorityRoles:");
            e.printStackTrace();
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "addstream twitch/mixer <stream name>` to make me respond whenever the stream comes online." +
                        "The platform specification `twitch` or `mixer` is so that I know where to look for the stream name." +
                        "\nUsing this command requires either the admin permission or one of these roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "Streamer is already being tracked in this channel" + help;
            case 3:
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
