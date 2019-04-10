package main.java.commands.Utility;

import main.java.commands.ICommand;
import main.java.core.commandHandler;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;

public class cmdHelp implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        MessageBuilder mb = new MessageBuilder("Prefix: `" + statics.prefix + "`\n");
        mb.append("To get help for a specific command, type `" + statics.prefix + "[command] -h`");
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.green)
                .setAuthor("Command list", "https://github.com/MaxOhn/PP-Generator");
        for (utilGeneral.Category c : utilGeneral.Category.values()) {
            eb.addField("__**" + c.getName() + "**__",
                    String.join(", ", commandHandler.getCommands(c)), false);
        }
        event.getTextChannel().sendMessage(mb.build()).embed(eb.build()).queue();
    }

    @Override
    public String help(int hCode) {
        return null;
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
