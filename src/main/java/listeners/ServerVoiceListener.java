package main.java.listeners;

import main.java.util.secrets;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerVoiceListener extends ListenerAdapter {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Distribute 'In VC' role to members who join a voice chat
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getGuild().getId().equals(secrets.mainGuildID)) {
            Role role = event.getGuild().getRoleById(673633138207096833L);
            event.getGuild().getController()
                    .addSingleRoleToMember(event.getMember(), role)
                    .queue();
            logger.info("Assigned member " + event.getMember().getEffectiveName() + " the \"In VC\" role");
        }
    }

    // Remove 'In VC' role from members upon leaving voice chat
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getGuild().getId().equals(secrets.mainGuildID)) {
            Role role = event.getGuild().getRoleById(673633138207096833L);
            event.getGuild().getController()
                    .removeSingleRoleFromMember(event.getMember(), role)
                    .queue();
            logger.info("Removed \"In VC\" role from member " + event.getMember().getEffectiveName());
        }
    }
}
