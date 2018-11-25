package main.java.commands;

import main.java.core.TwitchHook;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import static main.java.util.utilGeneral.isAuthority;

public class cmdRemoveStream implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length < 1 || args.length > 2) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        } else if (!isAuthority(event)) {
            event.getTextChannel().sendMessage(help(1)).queue();
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

        if (TwitchHook.removeStreamer(args[0], event.getTextChannel().getId()))
            event.getTextChannel().sendMessage("I'm no longer tracking `" + args[0] + "`'s twitch stream.").queue();
        else
            event.getTextChannel().sendMessage("Could not remove `" + args[0] + "`'s stream. Is the name being tracked in this channel?").queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "addstream --h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "removestream <twitch name>` to make me remove the name from the" +
                        " stream-tracking list";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }
}
