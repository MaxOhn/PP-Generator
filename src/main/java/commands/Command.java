package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Command {

    // called can check if certain conditions are satisfied to make the bot respond
    boolean called(String[] args, MessageReceivedEvent event);

    // action defines what the bot does for the given command
    void action(String[] args, MessageReceivedEvent event);

    // help explains the use of the command
    String help(int hCode);

}
