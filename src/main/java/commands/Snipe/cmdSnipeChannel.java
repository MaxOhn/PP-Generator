package main.java.commands.Snipe;

import main.java.commands.PrivilegedCommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdSnipeChannel extends PrivilegedCommand {

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (Main.snipeManager.addSnipeChannel(event.getTextChannel())) {
            event.getTextChannel()
                    .sendMessage("Added channel successfully. Now this channel gets spammed with snipe notifications too :))")
                    .queue();
        } else {
            event.getTextChannel().sendMessage("Something went wrong while adding this channel, blame bade :p").queue();
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "snipechannel -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "snipechannel` to make me notify this channel whenever a beatmap" +
                        " gets a new national #1 score.\nUsing this command requires either the admin " + "" +
                        "permission or one of these roles: `[" + String.join(", ", statics.authorities) + "]`";
            case 1:
                return "This command is only for the big boys. Your privilege is too low, yo" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.SNIPE;
    }
}
