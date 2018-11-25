package main.java.commands;

import main.java.core.Main;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdLink implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length < 1) {
            event.getTextChannel().sendMessage(help(0)).queue();
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
        if (Main.discLink.addLink(event.getAuthor().getId(), args[0]))
            event.getTextChannel().sendMessage("I linked discord's `" + event.getAuthor().getName() +
                    "` with osu's `" + args[0] + "`").queue();
        else
            event.getTextChannel().sendMessage("I could not link the accounts, blame bade").queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "link --h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "link <osu name>` to make me link your discord with that osu name";
            default:
                return help(0);
        }
    }
}
