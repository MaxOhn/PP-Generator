package main.java.core;

import main.java.util.statics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class commandParser {

    // Object containing processed information of command
    public static class commandContainer {

        final String raw;
        final String beheaded;
        final String[] splitBeheaded;
        final String invoke;
        final int number;
        final String[] args;
        public final MessageReceivedEvent event;

        commandContainer(String rw, String beheaded, String[] splitBeheaded, String invoke, int number, String[] args, MessageReceivedEvent event) {
            this.raw = rw;                          // raw command
            this.beheaded = beheaded;               // command without prefix
            this.splitBeheaded = splitBeheaded;     // array of string where each word of command is element
            this.invoke = invoke;                   // first word of command, i.e. invoke word
            this.number = number;
            this.args = args;                       // all other arguments in command
            this.event = event;                     // given event
        }
    }

    // This function processes the command and returns commandContainer
    public static commandContainer parser(String raw, MessageReceivedEvent event) {
        String beheaded = raw.replaceFirst(statics.prefix,"");
        String[] splitbeheaded = beheaded.split(" ");
        String invoke = splitbeheaded[0];
        int number = 1;
        if (invoke.matches(".*[1-9][0-9]*")) {
            Pattern p = Pattern.compile("(\\D*)([1-9][0-9]*)");
            Matcher m = p.matcher(invoke);
            if (m.find()) {
                invoke = m.group(1);
                number = Integer.parseInt(m.group(2));
            }
        }
        ArrayList<String> split = new ArrayList<>();
        Collections.addAll(split, splitbeheaded);
        String[] args = new String[split.size()-1];
        split.subList(1, split.size()).toArray(args);
        return new commandContainer(raw, beheaded, splitbeheaded, invoke, number, args, event);
    }

}
