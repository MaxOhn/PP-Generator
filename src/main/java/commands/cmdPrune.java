package main.java.commands;

import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import java.sql.SQLException;

import static main.java.util.utilGeneral.isAuthority;

public class cmdPrune implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        try {
            if (!isAuthority(event.getMember(), event.getGuild().getId())) {
                event.getTextChannel().sendMessage(help(1)).queue();
                return false;
            }
        } catch (SQLException | ClassNotFoundException e) {
            Logger logger = Logger.getLogger(this.getClass());
            logger.error("Error while retrieving authorityRoles: " + e);
            event.getTextChannel().sendMessage("Something went wrong, ping bade or smth :p").queue();
            return false;
        }
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
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "prune <amount>` to make me delete the last <amount> many messages" +
                        " in this channel (up to 100 at a time)\nUsing this command requires either the admin " + "" +
                        "permission or one of these roles: `[" + String.join(", ", statics.authorities) + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            case 2:
                return "This command requires between 1 argument!" + help;
            case 3:
                return "Amount must be greater than 0" + help;
            default:
                return help(0);
        }
    }
}
