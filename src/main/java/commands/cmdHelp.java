package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdHelp implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        event.getTextChannel().sendMessage("Ask bade xd").queue();
    }

    @Override
    public String help(int hCode) {
        return null;
    }
}
