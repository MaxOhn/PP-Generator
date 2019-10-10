package main.java.core;

import main.java.util.secrets;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;

public class ReactionHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private HashMap<Integer, String> roleAssigns = new HashMap<>();

    public ReactionHandler() {
        try {
            if (secrets.WITH_DB)
                roleAssigns = DBProvider.getRoleAssigns();
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not retrieve role assigns from DB:");
            e.printStackTrace();
        }
    }

    public void addRoleAssign(int hash, String roleID) {
        roleAssigns.put(hash, roleID);
        try {
            if (secrets.WITH_DB)
                DBProvider.addRoleAssign(hash, roleID);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not add role assign to DB:");
            e.printStackTrace();
        }
    }

    public void removeRoleAssign(int hash) {
        roleAssigns.remove(hash);
        try {
            if (secrets.WITH_DB)
                DBProvider.removeRoleAssign(hash);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not remove role assign from DB:");
            e.printStackTrace();
        }
    }

    public void addedReaction(GuildMessageReactionAddEvent event, int hash) {
        if (roleAssigns.containsKey(hash)) {
            Role role = event.getGuild().getRoleById(roleAssigns.get(hash));
            event.getGuild().getController()
                    .addSingleRoleToMember(event.getMember(), role)
                    .queue();
            logger.info("Assigned role '" + role.getName() + "' to member " + event.getMember().getEffectiveName());
        }
    }

    public void removedReaction(GuildMessageReactionRemoveEvent event, int hash) {
        if (roleAssigns.containsKey(hash)) {
            Role role = event.getGuild().getRoleById(roleAssigns.get(hash));
            event.getGuild().getController()
                    .removeSingleRoleFromMember(event.getMember(), role)
                    .queue();
            logger.info("Removed role '" + role.getName() + "' from member " + event.getMember().getEffectiveName());
        }
    }
}
