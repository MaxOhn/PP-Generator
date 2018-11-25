package main.java.commands;

import main.java.util.statics;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdPing implements Command {

    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    public void action(String[] args, MessageReceivedEvent event) {
        long time = System.currentTimeMillis();
        if(args.length == 0)
            event.getTextChannel().sendMessage("Ping!").queue(message ->
                    message.editMessageFormat("Ping! (%dms)", System.currentTimeMillis() - time).queue());
        else
            event.getTextChannel().sendMessage(help(0)).queue();
    }

    public String help(int hCode) {
        return "Enter `" + statics.prefix + "ping` to make me respond";
    }

}
