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

        String name = "";
        for(int i = 0; i < arguments.length; i++) {
            if (i == 0) name = name + arguments[i]; else name = name + " " + arguments[i];
        }

        if (Main.discLink.addLink(event.getAuthor().getId(), name))
            event.getTextChannel().sendMessage("I linked discord's `" + event.getAuthor().getName() +
                    "` with osu's `" + name + "`").queue();
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
