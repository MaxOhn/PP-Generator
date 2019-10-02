package main.java.commands.Fun;

import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public abstract class cmdSong implements ICommand {

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
            Logger.getLogger(this.getClass()).error("Error while interacting with lyrics database:", e);
            return false;
        }
        return !Main.runningLyrics.contains(busyID);
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        Main.runningLyrics.add(busyID);
        String[] lyrics = getLyrics();
        int delay = getDelay();
        final Thread t = new Thread(() -> {
            for (String lyric: lyrics) {
                try {
                    event.getChannel().sendMessage("♫ " + lyric + " ♫").queue();
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
            }
            try {
                Thread.sleep(getCooldown());
            } catch (InterruptedException ignored) {
            } finally {
                Main.runningLyrics.remove(busyID);
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
