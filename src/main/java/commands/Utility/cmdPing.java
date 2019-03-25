package main.java.commands.Utility;

import main.java.commands.Command;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdPing implements Command {

    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    public void action(String[] args, MessageReceivedEvent event) {
        long time = System.currentTimeMillis();
        if(args.length == 0)
            event.getTextChannel().sendMessage("Pong!").queue(message ->
                    message.editMessageFormat("Pong! (%dms)", System.currentTimeMillis() - time).queue());
        else
            event.getTextChannel().sendMessage(help(0)).queue();
    }

    public String help(int hCode) {
        return "Enter `" + statics.prefix + "ping` to make me respond";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }

}
