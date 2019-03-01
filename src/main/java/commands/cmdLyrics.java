package main.java.commands;

import main.java.core.DBProvider;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static main.java.util.utilGeneral.isAuthority;

public class cmdLyrics implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (!isAuthority(event)) {
            event.getTextChannel().sendMessage(help(2)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        try {
            if (args.length == 1) {
                switch (args[0]) {
                    case "-t":
                        if (DBProvider.getLyricsState(event.getGuild().getId())) {
                            DBProvider.setLyricsState(event.getGuild().getId(), false);
                            event.getTextChannel().sendMessage("User can no longer use song commands!").queue();
                        } else {
                            DBProvider.setLyricsState(event.getGuild().getId(), true);
                            event.getTextChannel().sendMessage("User can now use song commands!").queue();
                        }
                        break;
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
                if (args[0].equals("-s")) {
                    switch (args[1]) {
                        case "on":
                            DBProvider.setLyricsState(event.getGuild().getId(), true);
                            event.getTextChannel().sendMessage("User can now use song commands!").queue();
                            break;
                        case "off":
                            DBProvider.setLyricsState(event.getGuild().getId(), false);
                            event.getTextChannel().sendMessage("User can no longer use song commands!").queue();
                            break;
                        default:
                            event.getTextChannel().sendMessage(help(1)).queue();
                            break;
                    }
                } else {
                    event.getTextChannel().sendMessage(help(0)).queue();
                }
            } else {
                event.getTextChannel().sendMessage(help(0)).queue();
            }
        } catch (SQLException | ClassNotFoundException e) {
            event.getTextChannel().sendMessage("Something went wrong, ping bade or smth xd").queue();
            logger.error("Error while interacting with lyrics database: " + e);
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "lyrics -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "lyrics [-t / -s <on/off> / -c]` to either (`-t`) toggle or (`-s <on/off>`)" +
                        " set the permission to use lyrics commands on or off, or (`-c`) check the current state of the" +
                        " permission.\nUsing this command requires one of these roles: `[" +
                        String.join(", ", statics.authorities) + "]`";
            case 1:
                return "'-s' must be followed by either `on` or `off`" + help;
            case 2:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }
}
