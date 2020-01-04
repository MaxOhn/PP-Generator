package main.java.commands.Streams;

import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/*
    Displaying all streamer that are tracked in the current channel
 */
public class cmdTrackedStreams implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            event.getChannel().sendMessage("This command is not usable in private chat").queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help")))
            event.getTextChannel().sendMessage(help(0)).queue();
        else
            event.getTextChannel().sendMessage("I'm currently tracking in this channel: `" + Main.streamHook.trackedStreamers(event.getTextChannel().getId()) + "`").queue();
    }

    @Override
    public String help(int hCode) {
        return "Enter `" + statics.prefix + "trackedstreams` to make me display all streams I'm currently tracking";
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TWITCH;
    }
}
