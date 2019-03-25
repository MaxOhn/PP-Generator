package main.java.commands.Twitch;

import main.java.commands.PrivilegedCommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdRemoveStream extends PrivilegedCommand {

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
        if (Main.twitch.removeStreamer(args[0], event.getTextChannel().getId()))
            event.getTextChannel().sendMessage("I'm no longer tracking `" + args[0] + "`'s twitch stream.").queue();
        else
            event.getTextChannel().sendMessage("Could not remove `" + args[0] + "`'s stream. Is the name being tracked in this channel?").queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "addstream -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "removestream <twitch name>` to make me remove the name from the" +
                        " stream-tracking list\nUsing this command requires either the admin " + "" +
                        "permission or one of these roles: `[" + String.join(", ", statics.authorities) + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TWITCH;
    }
}
