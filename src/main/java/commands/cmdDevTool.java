package main.java.commands;

import main.java.core.Main;
import main.java.core.commandHandler;
import main.java.core.commandParser;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class cmdDevTool implements Command {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return utilGeneral.isDev(event);
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length == 0) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }
        switch (args[0]) {
            case "streamer":
            case "streamers":
                if (args.length == 1)
                    event.getTextChannel().sendMessage("Current streamers: `" + Main.twitch.getStreamers().toString() + "`").queue();
                else {
                    if (args[1].equals("r") && args.length >= 4) {
                        if (!Main.twitch.removeStreamer(args[2], args[3]))
                            event.getTextChannel().sendMessage(help(1)).queue();
                        else
                            event.getTextChannel().sendMessage("Removed `(" + args[2] + ", " + args[3] + ")` from streamers").queue();
                    } else if (args[1].equals("a") && args.length >= 4) {
                        if (!Main.twitch.addStreamer(args[2], args[3]))
                            event.getTextChannel().sendMessage(help(1)).queue();
                        else
                            event.getTextChannel().sendMessage("Added `(" + args[2] + ", " + args[3] + ")` to streamers").queue();
                    }
                }
                break;
            case "streamonline":
            case "onlines":
            case "onlinestream":
            case "online":
                event.getTextChannel().sendMessage("Currently online streamers: `" + Main.twitch.getIsOnline().toString() + "`").queue();
                break;
            case "discosu":
            case "discordosu":
            case "discordosulink":
            case "discordlink":
            case "osulink":
                event.getTextChannel().sendMessage("Current discord-osu links: `" + Main.discLink.getLink().toString() + "`").queue();
                break;
            default:
                event.getTextChannel().sendMessage(help(0)).queue();
                break;
        }
    }

    @Override
    public String help(int hCode) {
        switch(hCode) {
            case 0:
                return "Use this properly ...";
            case 1:
                return "Something went wrong";
            default:
                return help(0);
        }
    }
}
