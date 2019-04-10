package main.java.commands.Osu;

import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdLink implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {

        if (args.length == 0) {
            if (Main.discLink.removeLink(event.getAuthor().getId())) {
                event.getTextChannel().sendMessage("You are no longer linked").queue();
                return;
            } else {
                event.getTextChannel().sendMessage("I could not remove the link, blame bade").queue();
                return;
            }
        }

        if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }

        String name = String.join(" ", args);

        if (Main.discLink.addLink(event.getAuthor().getId(), name))
            event.getTextChannel().sendMessage("I linked discord's `" + event.getAuthor().getName() +
                    "` with osu's `" + name + "`").queue();
        else
            event.getTextChannel().sendMessage("I could not link the accounts, blame bade").queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "link -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "link [osu name]` to make me link your discord with that osu name."
                        + "\nIf no name is specified, I will unlink you instead.";
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
