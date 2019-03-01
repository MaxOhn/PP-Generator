package main.java.commands;

import main.java.core.commandHandler;
import main.java.util.statics;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdHelp implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        MessageBuilder mb = new MessageBuilder("Prefix: `" + statics.prefix + "`\n");
        mb.append("Commands: `[");
        for (String cmd: commandHandler.getCommands()) {
            mb.append(cmd).append(", ");
        }
        mb.replaceLast(", ", "");
        mb.append("]`\n");
        mb.append("To get help for a specific command, type `" + statics.prefix + "[command] -h`");
        event.getTextChannel().sendMessage(mb.build()).queue();
    }

    @Override
    public String help(int hCode) {
        return null;
    }
}
