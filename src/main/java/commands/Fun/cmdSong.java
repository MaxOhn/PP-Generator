package main.java.commands.Fun;

import main.java.commands.ICommand;
import main.java.core.DBProvider;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;

/*
    Making the bot sing some song
    Only usable if the server authorities didn't disable it on the server
 */
public abstract class cmdSong implements ICommand {
    // Check which channel has a song command running currently
    private static HashSet<String> runningLyrics = new HashSet<>();

    // Either the id of the private channel to a user or the id of a guild so that at most one song command at a time
    // can be used across all channels on a server
    private String busyID;

    abstract String[] getLyrics();

    abstract int getDelay();

    private int getCooldown() {
        return 2000;
    }

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        boolean privateMsg = event.isFromType(ChannelType.PRIVATE);
        busyID = privateMsg ? event.getChannel().getId() : event.getGuild().getId();
        try {
            if (!privateMsg && secrets.WITH_DB && !DBProvider.getLyricsState(busyID)) {
                event.getTextChannel().sendMessage("The server's big boys have disabled song commands. " +
                        "Modify the settings via `" + statics.prefix + "lyrics`.").queue();
                return false;
            }
        } catch (ClassNotFoundException | SQLException e) {
            event.getChannel().sendMessage("Something went wrong, blame bade").queue();
            LoggerFactory.getLogger(this.getClass()).error("Error while interacting with lyrics database:", e);
            return false;
        }
        return !runningLyrics.contains(busyID);
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        runningLyrics.add(busyID);
        String[] lyrics = getLyrics();
        new Thread(() -> {
            for (String lyric: lyrics) {
                try {
                    event.getChannel().sendMessage("♫ " + lyric + " ♫").queue();
                    Thread.sleep(getDelay());
                } catch (InterruptedException ignored) {}
            }
            try {
                Thread.sleep(getCooldown());
            } catch (InterruptedException ignored) {
            } finally {
                runningLyrics.remove(busyID);
            }
        }).start();
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
