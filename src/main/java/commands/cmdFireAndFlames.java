package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdFireAndFlames implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = {
                "So far away we wait for the day-yay",
                "For the lives all so wasted and gooone",
                "We feel the pain of a lifetime lost in a thousand days",
                "Through the fire and the flames we carry ooooooon"
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
