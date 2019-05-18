package main.java.commands.Utility;

import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.concurrent.ThreadLocalRandom;

public class cmdRoll implements ICommand {

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if(args.length > 0) {
            if(args.length > 1 || args[0].equals("-h")) {
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
                return false;
            }
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
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(2));
                return;
            }
        }
        // Give random number
        int rand = ThreadLocalRandom.current().nextInt(1,max+1);
        String out = event.getAuthor().getAsMention() + ", I rolled for you: " + rand;
        new BotMessage(event, BotMessage.MessageType.TEXT).send(out);
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "roll -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "roll [limit]` to make me give you a number between 1 and <limit>." +
                        "\nDefault: limit=10";
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
