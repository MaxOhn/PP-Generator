package main.java.commands.Osu;

import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/*
    Link the authors discord id to a given osu username (or unlink)
 */
public class cmdLink implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        // No name as argument -> unlink
        if (args.length == 0) {
            if (Main.discLink.removeLink(event.getAuthor().getId())) {
                event.getChannel().sendMessage("You are no longer linked").queue();
                return;
            } else {
                event.getChannel().sendMessage("I could not remove the link, blame bade").queue();
                return;
            }
        }
        if (args[0].equals("-h") || args[0].equals("-help")) {
            event.getChannel().sendMessage(help(0)).queue();
            return;
        }
        String name = String.join(" ", args);
        // Check if name is valid
        try {
           Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).build());
        } catch (Exception e) {
            event.getChannel().sendMessage("Could not find osu user with name `" + name + "`").queue();
            return;
        }
        // Add link
        if (Main.discLink.addLink(event.getAuthor().getId(), name))
            event.getChannel().sendMessage("I linked discord's `" + event.getAuthor().getName()
                    + "` with osu's `" + name + "`").queue();
        else
            event.getChannel().sendMessage("I could not link the accounts, blame bade").queue();
    }

    @Override
    public String help(int hCode) {
        return "Enter `" + statics.prefix + "link [osu name]` to make me link your discord with that osu name."
                + "\nIf no name is specified, I will unlink you instead.";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU_GENERAL;
    }
}
