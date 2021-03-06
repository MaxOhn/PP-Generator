package main.java.commands.Utility;

import main.java.commands.PrivilegedCommand;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    Select a discord message and a role so that if someone reacts on that message they get the role
    If they unreact again, they lose the role.
 */
public class cmdRoleAssign extends PrivilegedCommand {

    @Override
    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        if (args.length != 3) {
            event.getTextChannel().sendMessage(help(2)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String guild = event.getGuild().getId();
        // Check for the channel
        String channel = null;
        Pattern p = Pattern.compile("^(([0-9]*)|(<#([0-9]*)>))$");
        Matcher m = p.matcher(args[0]);
        if (m.find())
            channel = m.group(1);
        if (channel == null || channel.startsWith("<#"))
            channel = m.group(4);
        if (channel == null) {
            event.getChannel().sendMessage(help(3)).queue();
            return;
        }
        // Check for the message
        String message;
        try {
            message = Long.parseLong(args[1]) + "";
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(help(4)).queue();
            return;
        }
        // Check for the role
        String role = null;
        p = Pattern.compile("^(([0-9]*)|(<@&([0-9]*)>))$");
        m = p.matcher(args[2]);
        if (m.find()) {
            role = m.group(1);
            if (role == null || role.startsWith("<@&"))
                role = m.group(4);
        }
        if (role == null) {
            event.getChannel().sendMessage(help(5)).queue();
            return;
        }
        Role r = event.getGuild().getRoleById(role);
        if (r == null) {
            event.getChannel().sendMessage(help(6)).queue();
            return;
        }
        // Add combination to database
        Main.reactionHandler.addRoleAssign((guild + channel + message).hashCode(), role);
        TextChannel c = Main.jda.getGuildById(guild).getTextChannelById(channel);
        Main.jda.getGuildById(guild).getTextChannelById(channel).getMessageById(message).queue(msg -> {
            String response = "Who ever reacts on the message in " + c.getAsMention()
                    + "\n`" +msg.getContentDisplay() + "`\n"
                    + " will be assigned the `" + r.getName() + "` role!";
            event.getChannel().sendMessage(response).queue();
        });
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "roleassign -h` for more help)";
        String roles = secrets.WITH_DB ? "smth went wrong, ping bade or smth" : String.join(", ", statics.authorities);
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles:");
            e.printStackTrace();
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "roleassign <channel-mention or channel id> <message id> <role-mention or role id>`"
                        + " to make me assign the given role to anyone who reacts on that message. If a user removes all their"
                        + " reactions on the message again, they will lose the role."
                        + "\nUsing this command requires either the admin permission or one of the current "
                        + "authority roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "This command requires 3 arguments" + help;
            case 3:
                return "The first argument must be either a mention of the textchannel with the message or just the channel's id" + help;
            case 4:
                return "The second argument must be the message id" + help;
            case 5:
                return "The third argument must be either a mention of the role to be assigned or just the role's id" + help;
            case 6:
                return "Could not find the given role in this server. Are the ids correct?" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
