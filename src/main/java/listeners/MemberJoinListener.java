package main.java.listeners;

import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.ZonedDateTime;

import static main.java.util.secrets.newMembersChannel;

public class MemberJoinListener extends ListenerAdapter {

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("User " + event.getUser().getName() + " joined server " + event.getGuild().getName());
        if (event.getGuild().getId().equals(secrets.mainGuildID)) {
            event.getGuild().getTextChannelById(newMembersChannel)
                    .sendMessage(event.getMember().getAsMention() + " just joined the server, awaiting approval")
                    .queue();
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
