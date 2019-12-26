package main.java.listeners;

import main.java.core.DBProvider;
import main.java.util.secrets;
import main.java.util.statics;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ServerJoinListener extends ListenerAdapter {

    // Set basic properties to new server
    public void onGuildJoin(GuildJoinEvent event) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Joined a new server: " + event.getGuild().getName());
        try {
            if (secrets.WITH_DB) {
                logger.info("Trying to initialize serverProperties for server id " + event.getGuild().getId() + "...");
                DBProvider.setLyricsState(event.getGuild().getId(), true);
                DBProvider.setAuthorityRoles(event.getGuild().getId(), statics.authorities);
                logger.info("Successfully initialized serverProperties");
            }
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Error while setting serverProperties for server id " + event.getGuild().getId() + ":");
            e.printStackTrace();
        }
    }
}
