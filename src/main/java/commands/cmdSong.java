package main.java.commands;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public abstract class cmdSong implements Command {

    abstract String[] getLyrics();

    abstract int getDelay();

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String[] lyrics = getLyrics();
        int delay = getDelay();
        final Thread t = new Thread(() -> {
            for (String lyric: lyrics) {
                try {
                    event.getTextChannel().sendMessage("♫ " + lyric + " ♫").queue();
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
        });
        t.start();
    }

    @Override
    public String help(int hCode) {
        return null;
    }
}
