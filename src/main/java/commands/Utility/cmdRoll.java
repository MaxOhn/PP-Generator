package main.java.commands.Utility;

import main.java.commands.ICommand;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.concurrent.ThreadLocalRandom;

/*
    General roll command to get a random number
 */
public class cmdRoll implements ICommand {

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if(args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        int max = 100;
        if(args.length > 0) {
            try {
                max = Integer.parseInt(args[0]);
            // Catch non-number
            } catch(NumberFormatException e) {
                event.getChannel().sendMessage(help(2)).queue();
                return;
            }
        }
        // Give random number
        event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", I rolled for you: " + ThreadLocalRandom.current().nextInt(1,max + 1)).queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "roll -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "roll [limit]` to make me give you a number between 1 and <limit>." +
                        "\nDefault limit is 100";
            case 1:
                return "Limit must be bigger than 1" + help;
            case 2:
                return "First argument must be a number!" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
