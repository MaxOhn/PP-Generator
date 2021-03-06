package main.java.commands;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static main.java.util.utilGeneral.isAuthority;

/*
    Some commands can only be used by authorities i.e. members that have admin permitions or have certain authority roles
 */
public abstract class PrivilegedCommand implements ICommand {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected String serverID;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            event.getChannel().sendMessage("This command is not usable in private chat").queue();
            return false;
        }
        serverID = event.getGuild().getId();
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
            logger.error("Error while retrieving authorityRoles: ", e);
            event.getTextChannel().sendMessage("Something went wrong, blame bade").queue();
            return false;
        }
        return customCalled(args, event);
    }

    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        return true;
    }
}
