package main.java.listeners;

import main.java.core.Main;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleLostListener extends ListenerAdapter {

    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (event.getRoles().iterator().next().getName().equals("Not Checked")
                && event.getGuild().getId().equals(secrets.mainGuildID)) {
            Main.memberHandler.checkedUser(event.getUser().getId());
            Logger logger = LoggerFactory.getLogger(this.getClass());
            String welcomeMessage = "welcome " + event.getUser().getName() + ", enjoy ur stay o/";
            Main.jda.getTextChannelById(secrets.welcomeMsgChannelID).sendMessage(welcomeMessage).queue();
            logger.info(event.getUser().getName() + " lost the 'Not Checked'-role");
        }
    }
}
