package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdTest implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        System.out.println("no usage for test command");
    }

    @Override
    public String help(int hCode) {
        return null;
    }
}
