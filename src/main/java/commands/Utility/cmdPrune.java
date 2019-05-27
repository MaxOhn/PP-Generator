package main.java.commands.Utility;

import main.java.commands.PrivilegedCommand;
import main.java.core.DBProvider;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;

public class cmdPrune extends PrivilegedCommand {

    @Override
    public boolean customCalled(String[] args, MessageReceivedEvent event) {
        if (args.length != 1) {
            event.getTextChannel().sendMessage(help(2)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }
        if (amount < 1) {
            event.getTextChannel().sendMessage(help(3)).queue();
            return;
        }
        final String response = "I deleted the last " + (Math.min(amount, 100)) + " messages";
        amount = Math.min(amount + 1, 100);

        event.getTextChannel().getIterableHistory().takeAsync(amount).thenApply(event.getTextChannel()::purgeMessages)
                .thenAccept(arg -> event.getTextChannel().sendMessage(response).queue(message -> {
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ignored) {
                    } finally {
                        message.delete().queue();
                    }
                })
         );
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "prune -h` for more help)";
        String roles = "smth went wrong, ping bade or smth";
        try {
            roles = String.join(", ", DBProvider.getAuthorityRoles(serverID));
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while retrieving authorityRoles: " + e);
        }
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "prune <amount>` to make me delete the last <amount> many messages" +
                        " in this channel (up to 100 at a time)\nUsing this command requires either the admin " + "" +
                        "permission or one of these roles: `[" + roles + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "This command requires 1 argument!" + help;
            case 3:
                return "Amount must be greater than 0" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
