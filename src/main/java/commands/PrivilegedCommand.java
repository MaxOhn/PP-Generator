package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static main.java.util.utilGeneral.isAuthority;

public abstract class PrivilegedCommand implements Command {

    protected Logger logger = Logger.getLogger(this.getClass());

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        try {
            if (!isAuthority(event.getMember(), event.getGuild().getId())) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return false;
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
            event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            return false;
        }
        return customCalled(args, event);
    }

    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        return true;
    }
}
