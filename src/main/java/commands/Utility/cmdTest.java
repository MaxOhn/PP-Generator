package main.java.commands.Utility;

import main.java.commands.Command;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdTest implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        System.out.println("no usage for test command");
        event.getTextChannel().sendMessage("Useless command as of now").queue();
    }

    @Override
    public String help(int hCode) {
        return null;
    }
}
