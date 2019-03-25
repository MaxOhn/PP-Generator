package main.java.commands.Fun;

import main.java.commands.Command;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class cmdSong implements Command {

    abstract String[] getLyrics();

    abstract int getDelay();

    private int getCooldown() {
        return 2000;
    }

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
        return !Main.runningLyrics.contains(event.getGuild().getId());
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        Main.runningLyrics.add(event.getGuild().getId());
        String[] lyrics = getLyrics();
        int delay = getDelay();
        final Thread t = new Thread(() -> {
            for (String lyric: lyrics) {
                try {
                    event.getTextChannel().sendMessage("♫ " + lyric + " ♫").queue();
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
            try {
                Thread.sleep(getCooldown());
            } catch (InterruptedException ignored) {
            } finally {
                Main.runningLyrics.remove(event.getGuild().getId());
            }
        });
        t.start();
    }

    @Override
    public String help(int hCode) {
        return null;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.FUN;
    }
}
