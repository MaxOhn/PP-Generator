package main.java.core;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import main.java.util.statics;

import java.util.ArrayList;
import java.util.Collections;

public class commandParser {

    // Object containing processed information of command
    public static class commandContainer {

        final String raw;
        final String beheaded;
        final String[] splitBeheaded;
        final String invoke;
        final String[] args;
        public final MessageReceivedEvent event;

        commandContainer(String rw, String beheaded, String[] splitBeheaded, String invoke, String[] args, MessageReceivedEvent event) {
            this.raw = rw;                          // raw command
            this.beheaded = beheaded;               // command without prefix
            this.splitBeheaded = splitBeheaded;     // array of string where each word of command is element
            this.invoke = invoke;                   // first word of command, i.e. invoke word
            this.args = args;                       // all other arguments in command
            this.event = event;                     // given event
        }
    }

    // This function processes the command and returns commandContainer
    public static commandContainer parser(String raw, MessageReceivedEvent event) {
        String beheaded = raw.replaceFirst(statics.prefix,"");
        String[] splitbeheaded = beheaded.split(" ");
        String invoke = splitbeheaded[0];
        ArrayList<String> split = new ArrayList<>();
        Collections.addAll(split, splitbeheaded);
        String[] args = new String[split.size()-1];
        split.subList(1, split.size()).toArray(args);
        return new commandContainer(raw, beheaded, splitbeheaded, invoke, args, event);
    }

}
