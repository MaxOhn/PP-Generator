package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdBrainpower implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = {
                "Andrenaline is pumping",
                "Andrenaline is pumpiiing",
                "Generator",
                "Automatic Lover",
                "Atomic",
                "Atomiiic",
                "Ooooverdrive",
                "Blockbuster",
                "Brain Power",
                "Call me a leader -- cocaine",
                "Don't you try it",
                "Don't you tryyy it",
                "Innovator",
                "Killer Machine",
                "There's no fate",
                "Take control",
                "Brain Poweeeer",
                "O-oooooooooo AAAAE-A-A-I-A-U- EO-"
        };
        int delay = 2500;
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
