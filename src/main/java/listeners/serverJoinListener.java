package main.java.listeners;

import main.java.core.DBProvider;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class serverJoinListener extends ListenerAdapter {

    public void onGuildJoin(GuildJoinEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        logger.info("Joined a new server: " + event.getGuild().getName());
        try {
            DBProvider.setLyricsState(event.getGuild().getId(), true);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Error while interacting with lyrics database: " + e);
        }
    }
}
