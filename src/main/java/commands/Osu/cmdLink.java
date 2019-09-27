package main.java.commands.Osu;

import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
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
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("You are no longer linked");
                return;
            } else {
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("I could not remove the link, blame bade");
                return;
            }
        }

        if (args[0].equals("-h") || args[0].equals("-help")) {
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(0));
            return;
        }

        String name = String.join(" ", args);

        try {
           Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(name).build());
        } catch (Exception e) {
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("Could not find osu user with name `" + name + "`");
            return;
        }

        if (Main.discLink.addLink(event.getAuthor().getId(), name))
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("I linked discord's `" + event.getAuthor().getName()
                    + "` with osu's `" + name + "`");
        else
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("I could not link the accounts, blame bade");
    }

    @Override
    public String help(int hCode) {
        return "Enter `" + statics.prefix + "link [osu name]` to make me link your discord with that osu name."
                + "\nIf no name is specified, I will unlink you instead.";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU;
    }
}
