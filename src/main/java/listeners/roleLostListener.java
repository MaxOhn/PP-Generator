package main.java.listeners;

import main.java.core.Main;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

public class roleLostListener extends ListenerAdapter {

    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (event.getRoles().iterator().next().getName().equals("Not Checked")) {
            Logger logger = Logger.getLogger(this.getClass());
            String welcomeMessage = "welcome on the server " + event.getUser().getName() + " o/";
            Main.sendCustomMessage(welcomeMessage, secrets.welcomeMsgChannelID);
            logger.info(event.getUser().getName() + " lost the 'Not Checked'-role");
        }
    }
}
