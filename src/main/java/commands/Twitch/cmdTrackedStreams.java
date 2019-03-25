package main.java.commands.Twitch;

import main.java.commands.Command;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdTrackedStreams implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help")))
            event.getTextChannel().sendMessage(help(0)).queue();
        else
            event.getTextChannel().sendMessage("I'm current tracking in this channel: `" + Main.twitch.trackedStreamers(event.getTextChannel().getId()) + "`").queue();
    }

    @Override
    public String help(int hCode) {
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "trackedstreams`" +
                        "to make me display all twitch streams I'm currently tracking";
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TWITCH;
    }
}
