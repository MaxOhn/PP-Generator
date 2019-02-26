package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdPretender implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = {
                "What if I say I'm not like the others?",
                "What if I say I'm not just another oooone of your plays?",
                "You're the pretender",
                "What if I say that I will never surrender?"
        };
        int delay = 3500;
        final Thread t = new Thread(() -> {
            for (int i = 0; i < lyrics.length; i++) {
                try {
                    event.getTextChannel().sendMessage("♫ " + lyrics[i] + " ♫").queue();
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    i--;
                }
            }
        });
        t.start();
    }

    @Override
    public String help(int hCode) {
        return null;
    }
}
