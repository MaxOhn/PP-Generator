package main.java.commands;

import main.java.core.DBProvider;
import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class cmdSong implements Command {

    abstract String[] getLyrics();

    abstract int getDelay();

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        try {
            if (!DBProvider.getLyricsState(event.getGuild().getId())) {
                event.getTextChannel().sendMessage("The server's big boys have disabled song commands. " +
                        "Modify the settings via `" + statics.prefix + "lyrics`.").queue();
                return false;
            }
        } catch (ClassNotFoundException | SQLException e) {
            event.getTextChannel().sendMessage("Something went wrong, ping bade or smth xd").queue();
            Logger logger = Logger.getLogger(this.getClass());
            logger.error("Error while interacting with lyrics database: " + e);
            return false;
        }
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
