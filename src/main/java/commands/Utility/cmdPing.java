package main.java.commands.Utility;

import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdPing implements ICommand {

    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    public void action(String[] args, MessageReceivedEvent event) {
        long time = System.currentTimeMillis();
        if(args.length == 0)
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("Pong!", message ->
                    message.editMessageFormat("Pong! (%dms)", System.currentTimeMillis() - time).queue());
        else
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(0));
    }

    public String help(int hCode) {
        return "Enter `" + statics.prefix + "ping` to make me pong";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }

}
