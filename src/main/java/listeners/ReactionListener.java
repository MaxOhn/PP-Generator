package main.java.listeners;

import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import java.util.HashMap;

public class ReactionListener extends ListenerAdapter {

    private Logger logger = Logger.getLogger(this.getClass());
    private HashMap<Integer, String> roleAssigns = new HashMap<>();

    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        int hash = getHash(event);
        if (roleAssigns.containsKey(hash)) {
            Role role = event.getGuild().getRoleById(roleAssigns.get(hash));
            event.getGuild().getController()
                    .addSingleRoleToMember(event.getMember(), role)
                    .queue();
            logger.info("Assigned role " + role.getName() + " to member " + event.getMember().getNickname());
        }
    }

    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        int hash = getHash(event);
        if (roleAssigns.containsKey(hash)) {
            Role role = event.getGuild().getRoleById(roleAssigns.get(hash));
            event.getGuild().getController()
                    .removeSingleRoleFromMember(event.getMember(), role)
                    .queue();
            logger.info("Removed role " + role.getName() + " from member " + event.getMember().getNickname());
        }
    }

    private int getHash(GenericGuildMessageReactionEvent event) {
        return (event.getGuild().getId() + event.getChannel().getId() + event.getMessageId()).hashCode();
    }
}
