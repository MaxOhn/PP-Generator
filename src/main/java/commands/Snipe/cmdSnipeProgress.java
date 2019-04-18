package main.java.commands.Snipe;

import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdSnipeProgress implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        String msgId = Main.snipeManager.getMessageId(event.getTextChannel());
        switch (msgId) {
            case "-1":
                event.getTextChannel().sendMessage(help(2)).queue();
                break;
            case "-2":
                event.getTextChannel().sendMessage(help(1)).queue();
                break;
            default:
                String currentMapID = Main.snipeManager.getCurrentMapID();
                event.getTextChannel().sendMessage("Current map id: " + currentMapID + "\nhttps://discordapp.com/channels/" + event.getGuild().getId()
                        + "/" + event.getTextChannel().getId() + "/" + msgId).queue();
                break;
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "snipeprogress -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "snipeprogress` to make me link to the rebuild progress of the" +
                        " snipe scores.\nThis command only works while rebuilding is in progress in channels that were "
                        + "linked via `" + statics.prefix + "snipechannel`.";
            case 1:
                return "This command only works in channels in which `" + statics.prefix + "snipechannel` was used" + help;
            case 2:
                return "Rankings are currently not being updated" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.SNIPE;
    }
}
