package main.java.listeners;

import main.java.core.DBProvider;
import main.java.util.statics;
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
            DBProvider.setAuthorityRoles(event.getGuild().getId(), statics.authorities);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Error while setting serverProperties entry: " + e);
        }
    }
}
