package main.java.core;

import main.java.commands.Command;
import org.apache.log4j.Logger;

import java.util.HashMap;

public class commandHandler {

    // parse parses the command into the processed object commandContainer
    public static final commandParser parse = new commandParser();

    // commands contains all invoke words with their corresponding action
    static HashMap<String, Command> commands = new HashMap<>();

    public static void handleCommand(commandParser.commandContainer cmd) {
        // If the word following the prefix is an invoke word
        if(commands.containsKey(cmd.invoke)) {
            // Check if called conditions are satisfied
            boolean safe = commands.get(cmd.invoke).called(cmd.args,cmd.event);
            // If so, perform the action
            if(safe)
                commands.get(cmd.invoke).action(cmd.args,cmd.event);
            // Log the occurrence of the invoke
            Logger logger = Logger.getLogger(commands.get(cmd.invoke).getClass());
            logger.info(String.format("[%s] %s: %s", cmd.event.getGuild().getName() + ":" + cmd.event.getTextChannel().getName(),
                    cmd.event.getAuthor().getName(), cmd.event.getMessage().getContentRaw()));
        } else {
            Logger logger = Logger.getLogger("core.commandHandler");
            logger.warn(cmd.invoke + " is no valid command");
        }
    }
}
