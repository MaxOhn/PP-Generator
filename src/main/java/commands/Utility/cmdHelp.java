package main.java.commands.Utility;

import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.commandHandler;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

public class cmdHelp implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        MessageBuilder mb = new MessageBuilder("Prefix: `" + statics.prefix + "`")
                .append("\nTo get help for a specific command, type `" + statics.prefix + "[command] -h`")
                .append("\nCommands can also be used in private messages to me");
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.green)
                .setAuthor("Command list", "https://github.com/MaxOhn/PP-Generator");
        for (utilGeneral.Category c : utilGeneral.Category.values()) {
            Map<String, ICommand> cmds = commandHandler.getCommands(c);
            String fieldString = String.join(", ",
                    cmds.keySet().stream()
                            .collect(Collectors.groupingBy(invoke -> cmds.get(invoke).getClass()))
                            .values().stream()
                            .map(list -> list.size() == 1 ? list.get(0) : "[" + String.join(", ", list) + "]")
                            .collect(Collectors.toList())
            );
            eb.addField("__**" + c.getName() + "**__", fieldString, false);
        }
        new BotMessage(event, BotMessage.MessageType.TEXT).send(mb.build(), eb.build());
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
