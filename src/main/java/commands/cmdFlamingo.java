package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdFlamingo implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = {
                "Black, white, green or blue,",
                "show off your natural hue",
                "Flamingo, oh oh owoah",
                "If you're multicouloured thats cool too",
                "You, dont, need to change,",
                "it's boring bein' the same",
                "Flamingo, oh oh oh",
                "You're pretty either way!"
        };
        int delay = 3000;
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
