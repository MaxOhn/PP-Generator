package main.java.listeners;

import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.time.ZonedDateTime;

public class MemberJoinListener extends ListenerAdapter {

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        logger.info("User " + event.getUser().getName() + " joined server " + event.getGuild().getName());
        if (!secrets.RELEASE || event.getGuild().getId().equals(secrets.mainGuildID)) {
            try {
                ZonedDateTime now = ZonedDateTime.now();
                Main.memberHandler.addUncheckedUser(event.getUser().getId() + "", now);
                if (secrets.WITH_DB)
                    DBProvider.addUncheckedUser(event.getUser().getId() + "", now);
            } catch (ClassNotFoundException | SQLException e) {
                logger.error("Error while adding unchecked user to DB:");
                e.printStackTrace();
            }
        }
    }
}
