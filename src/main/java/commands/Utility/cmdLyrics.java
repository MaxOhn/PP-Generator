package main.java.commands.Utility;

import main.java.commands.PrivilegedCommand;
import main.java.core.DBProvider;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;

/*
    Set whether the song commands can be used on a server or not
 */
public class cmdLyrics extends PrivilegedCommand {

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (!secrets.WITH_DB) {
            event.getTextChannel().sendMessage("This command has no use without database").queue();
            return;
        }
        try {
            if (args.length == 1) {
                switch (args[0]) {
                    // Toggle
                    case "-t":
                        if (DBProvider.getLyricsState(event.getGuild().getId())) {
                            DBProvider.setLyricsState(event.getGuild().getId(), false);
                            event.getTextChannel().sendMessage("Users can no longer use song commands!").queue();
                        } else {
                            DBProvider.setLyricsState(event.getGuild().getId(), true);
                            event.getTextChannel().sendMessage("Users can now use song commands!").queue();
                        }
                        break;
                    // Current
                    case "-c":
                        if (DBProvider.getLyricsState(event.getGuild().getId()))
                            event.getTextChannel().sendMessage("Users are currently allowed to use song commands!").queue();
                        else
                            event.getTextChannel().sendMessage("Users are currently prohibited from using song commands!").queue();
                        break;
                    default:
                        event.getTextChannel().sendMessage(help(0)).queue();
                        break;
                }
            } else if (args.length == 2) {
                // Set
                if (args[0].equals("-s")) {
                    switch (args[1]) {
                        case "on":
                            DBProvider.setLyricsState(event.getGuild().getId(), true);
                            event.getTextChannel().sendMessage("Users can now use song commands!").queue();
                            break;
                        case "off":
                            DBProvider.setLyricsState(event.getGuild().getId(), false);
                            event.getTextChannel().sendMessage("Users can no longer use song commands!").queue();
                            break;
                        default:
                            event.getTextChannel().sendMessage(help(2)).queue();
                            break;
                    }
                } else {
                    event.getTextChannel().sendMessage(help(0)).queue();
                }
            } else {
                event.getTextChannel().sendMessage(help(0)).queue();
            }
        } catch (SQLException | ClassNotFoundException e) {
            event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            logger.error("Error while retrieving lyricsAvailable state: " + e);
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "lyrics -h` for more help)";
        String roles = secrets.WITH_DB ? "smth went wrong, ping bade or smth" : String.join(", ", statics.authorities);
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "lyrics [-t / -s <on/off> / -c]` to either (`-t`) toggle or (`-s <on/off>`)" +
                        " set the permission to use lyrics commands on or off, or (`-c`) check the current state of the" +
                        " permission.\nUsing this command requires either the admin permission or one of the current "
                        + "authority roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "'-s' must be followed by either `on` or `off`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
