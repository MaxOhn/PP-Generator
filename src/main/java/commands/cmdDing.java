package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdDing implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = {
                "Oh-oh-oh, hübsches Ding",
                "Ich versteck' mein' Ehering",
                "Klinglingeling, wir könnten's bring'n",
                "Doch wir nuckeln nur am Drink",
                "Oh-oh-oh, hübsches Ding",
                "Du bist Queen und ich bin King",
                "Wenn ich dich seh', dann muss ich sing'n:",
                "Tingalingaling, you pretty thing!"
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
