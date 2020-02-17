package main.java.listeners;

import main.java.core.Main;
import main.java.util.secrets;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

public class RoleLostListener extends ListenerAdapter {

    // Welcome new users if the unchecked-role is lost
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (event.getGuild().getId().equals(secrets.mainGuildID) &&
                event.getRoles().iterator().next().getName().toLowerCase().equals("not checked")) {
            String userID = event.getUser().getId();
            Main.memberHandler.checkedUser(userID);
            Guild guild = Main.jda.getGuildById(event.getGuild().getId());
            /*
            // TODO(?): Get Member that removoed the role from GuildMemberRoleRemoveEvent
            guild.getTextChannelById(secrets.newMembersChannel)
                    .sendMessage("User " + guild.getMemberById(userID).getAsMention() +
                            " unchecked by " + guild.getMemberById(event.
                                // TODO
                            .getAsMention()))
                    .queue();
            */
            String welcomeMessage = "welcome " + event.getUser().getName() + ", enjoy ur stay o/";
            guild.getTextChannelById(secrets.welcomeMsgChannelID).sendMessage(welcomeMessage).queue();
            LoggerFactory.getLogger(this.getClass()).info(event.getUser().getName() + " lost the 'Not Checked'-role");
        }
    }
}
