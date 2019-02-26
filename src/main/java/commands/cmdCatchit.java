package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdCatchit implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = {
                "This song is one you won't forget",
                "It will get stuck -- in your head",
                "If it does, then you can't blame me",
                "Just like I said - too catchy"
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
