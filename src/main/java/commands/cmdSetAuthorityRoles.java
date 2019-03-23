package main.java.commands;

import main.java.core.DBProvider;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static main.java.util.utilGeneral.isAuthority;

public class cmdSetAuthorityRoles implements Command {

    private String serverID;
    private Logger logger;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        serverID = event.getGuild().getId();
        logger = Logger.getLogger(this.getClass());
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        try {
            if (!isAuthority(event.getMember(), serverID)) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return false;
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
            event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-d") ||args[0].equals("-default"))) {
            try {
                DBProvider.setAuthorityRoles(serverID, statics.authorities);
                event.getTextChannel().sendMessage("Authority roles have been updated!").queue();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Error while setting authorityRoles: " + e);
                event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            }
        } else if (args.length > 0 && (args[0].equals("-c") ||args[0].equals("-current"))) {
            try {
                event.getTextChannel().sendMessage("Current authority roles: `[" +
                        String.join(", ", DBProvider.getAuthorityRoles(serverID)) + "]`").queue();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Error while retrieving authorityRoles: " + e);
                event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            }
        } else {
            try {
                DBProvider.setAuthorityRoles(serverID, args);
                event.getTextChannel().sendMessage("Authority roles have been updated!").queue();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Error while setting authorityRoles: " + e);
                event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            }
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "setauthorityroles -h` for more help)";
        String roles = "~~ smth went wrong, ping bade or smth :p ~~";
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "setauthorityroles [role1 role2 role3 ...] [-c] [-d]` so only users " +
                        "with one of those roles are allowed to use privileged commands. If `-c` is specified, I will " +
                        "only respond with the current authority roles instead. If `-d` is specified, I will set the " +
                        "default authority roles.\nUsing this command requires either the admin " +
                        "permission or one of the current authority roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }
}
