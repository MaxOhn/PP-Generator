package main.java.core;

import main.java.commands.ICommand;
import main.java.commands.INumberedICommand;
import main.java.util.utilGeneral;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class commandHandler {

    // commands contains all invoke words with their corresponding action
    static HashMap<String, ICommand> commands = new HashMap<>();

    public static Set<String> getCommands() {
        return commands.keySet();
    }

    public static Set<String> getCommands(utilGeneral.Category c) {
        return commands.keySet()
                .stream()
                .filter(invoke -> commands.get(invoke).getCategory() == c)
                .collect(Collectors.toSet());
    }

    public static void handleCommand(commandParser.commandContainer cmd) {

        String invoke = cmd.invoke.toLowerCase();

        // If the word following the prefix is an invoke word
        if(commands.containsKey(invoke)) {
            // Check if called conditions are satisfied
            boolean safe = commands.get(invoke).called(cmd.args, cmd.event);
            Logger logger = Logger.getLogger(commands.get(invoke).getClass());
            try {
                // If so, perform the action in new thread
                if (safe) {
                    final Thread t = new Thread(() -> {
                        if (commands.get(invoke) instanceof INumberedICommand)
                            ((INumberedICommand) commands.get(invoke)).setNumber(cmd.number).action(cmd.args, cmd.event);
                        else
                            commands.get(invoke).action(cmd.args, cmd.event);
                    });
                    t.start();
                }
                // Log the occurrence of the invoke
                logger.info(String.format("[%s] %s: %s", cmd.event.getGuild().getName() + ":" + cmd.event.getTextChannel().getName(),
                        cmd.event.getAuthor().getName(), cmd.event.getMessage().getContentRaw()));
            } catch (Exception e) {
                logger.error(String.format("%s: %s [%s]", cmd.event.getAuthor().getName(), cmd.event.getMessage().getContentRaw(), e.getMessage()));
                e.printStackTrace();
            }
        }
    }
}
