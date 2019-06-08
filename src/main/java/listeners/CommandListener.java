package main.java.listeners;

import main.java.core.commandHandler;
import main.java.core.commandParser;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {

    public void onMessageReceived(MessageReceivedEvent event) {
        if((event.getMessage().getContentRaw().startsWith(statics.prefix) || event.getMessage().getContentRaw().startsWith(statics.prefixAlt))
                && !event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            commandHandler.handleCommand(commandParser.parser(event.getMessage().getContentRaw(), event));
        }
    }
}
