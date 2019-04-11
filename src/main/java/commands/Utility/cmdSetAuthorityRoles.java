package main.java.commands.Utility;

import main.java.commands.PrivilegedCommand;
import main.java.core.DBProvider;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class cmdSetAuthorityRoles extends PrivilegedCommand {

    private String serverID;

    @Override
    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        serverID = event.getGuild().getId();
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-d") ||args[0].equals("-default"))) {
            try {
                DBProvider.setAuthorityRoles(serverID, statics.authorities);
                event.getTextChannel().sendMessage("Authority roles have been updated!").queue();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Error while setting authorityRoles: " + e);
                event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            }
        } else if (args.length > 0 && (args[0].equals("-c") ||args[0].equals("-current"))) {
            try {
                event.getTextChannel().sendMessage("Current authority roles: `[" +
                        String.join(", ", DBProvider.getAuthorityRoles(serverID)) + "]`").queue();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Error while retrieving authorityRoles: " + e);
                event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            }
        } else {
            try {
                List<String> argList = new ArrayList<>();
                if (args.length > 0) {
                    int idx = 0;
                    while (idx < args.length) {
                        if (args[idx].startsWith('"' + "")) {
                            int startIdx = idx++;
                            while (idx < args.length && !args[idx].endsWith('"' + "")) idx++;
                            if (idx == args.length) {
                                argList.add(args[startIdx]);
                                idx = startIdx + 1;
                            } else {
                                StringBuilder roleWithSpaces = new StringBuilder();
                                roleWithSpaces.append(args[startIdx], 1, args[startIdx++].length()).append(" ");
                                for (; startIdx < idx; startIdx++)
                                    roleWithSpaces.append(args[startIdx]).append(" ");
                                argList.add(roleWithSpaces.append(args[idx], 0, args[idx++].length()-1).toString());
                            }
                        } else
                            argList.add(args[idx++]);
                    }
                }
                boolean remainsAuthority = utilGeneral.isDev(event.getAuthor());
                for (Role r : event.getMember().getRoles()) {
                    remainsAuthority |= r.hasPermission(Permission.ADMINISTRATOR);
                    remainsAuthority |= argList.contains(r.getName()) || argList.contains(r.getName().toLowerCase());
                    if (remainsAuthority) break;
                }
                if (!remainsAuthority) {
                    event.getTextChannel().sendMessage("This would remove your privilege!\nIf you dont have the server "
                            + "administration permission, you must include at least one of your own roles as authority role.").queue();
                    return;
                }
                DBProvider.setAuthorityRoles(serverID, argList);
                event.getTextChannel().sendMessage("Authority roles have been updated!").queue();
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("Error while setting authorityRoles: " + e);
                event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            }
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "setauthorityroles -h` for more help)";
        String roles = "~~ smth went wrong, ping bade or smth :p ~~";
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "setauthorityroles [role1 role2 role3 ...] [-c] [-d]` so only users " +
                        "with one of those roles are allowed to use privileged commands.\nIf a role contains a whitespace" +
                        ", encapsulate the role with `\"` e.g. `\"bot commander\"`.\nIf `-c` is specified, I will " +
                        "only respond with the current authority roles instead.\nIf `-d` is specified, I will set the " +
                        "default authority roles.\nUsing this command requires either the admin " +
                        "permission or one of the current authority roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
