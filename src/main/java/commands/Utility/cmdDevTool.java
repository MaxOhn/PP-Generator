package main.java.commands.Utility;

import main.java.commands.ICommand;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class cmdDevTool implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (utilGeneral.isDev(event.getAuthor())) return true;
        else event.getTextChannel().sendMessage("Only devs can use the devtools command !!!").queue();
        return false;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length == 0) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return;
        }
        switch (args[0]) {
            case "ppRating":
            case "ppRatings":
            case "ppSaved":
                if (args.length == 1) {
                    event.getTextChannel().sendMessage("Second argument must be `count` or `avg`").queue();
                    break;
                } else if (args[1].equals("count")) {
                    if (!secrets.WITH_DB) {
                        event.getTextChannel().sendMessage("Won't work without database").queue();
                        break;
                    }
                    try {
                        if (args.length == 2 || args[2].equals("all")) {
                            StringBuilder msg = new StringBuilder("Amount of saved pp values for all mods:\n");
                            for (String mod : new String[] { "NM", "HD", "HR", "DT", "HDHR", "HDDT"})
                                msg.append(mod).append(": ").append(DBProvider.getAmount(mod)).append("\n");
                            event.getTextChannel().sendMessage(msg.toString()).queue();
                        } else {
                            int response = DBProvider.getAmount(args[2]);
                            event.getTextChannel().sendMessage("Amount of saved pp values for `" + args[2] + "` scores: " + response).queue();
                        }
                    } catch (SQLException | ClassNotFoundException e) {
                        event.getTextChannel().sendMessage("Third argument must be `all` or a valid mod combination").queue();
                        break;
                    }
                } else if (args[1].equals("avg")) {
                    if (!secrets.WITH_DB) {
                        event.getTextChannel().sendMessage("Won't work without database").queue();
                        break;
                    }
                    try {
                        if (args.length == 2 || args[2].equals("all")) {
                            StringBuilder msg = new StringBuilder("Averages of saved pp values for all mods:\n");
                            for (String mod : new String[] { "NM", "HD", "HR", "DT", "HDHR", "HDDT"})
                                msg.append(mod).append(": ").append(DBProvider.getAverage(mod)).append("\n");
                            event.getTextChannel().sendMessage(msg.toString()).queue();
                        } else {
                            double response = DBProvider.getAverage(args[2]);
                            event.getTextChannel().sendMessage("Average of saved pp values for `" + args[2] + "` scores: " + response).queue();
                        }
                    } catch (SQLException | ClassNotFoundException e) {
                        event.getTextChannel().sendMessage("Third argument must be `all` or a valid mod combination").queue();
                        break;
                    }
                } else {
                    event.getTextChannel().sendMessage("Second argument must be `count` or `avg`").queue();
                    break;
                }
                break;
            case "send":
            case "sendmessage":
            case "sendmsg":
                try {
                    List<String> argList = Arrays.asList(args);
                    String channelID = args[1];
                    String msg = String.join(" ", argList.subList(2, args.length));
                    Main.jda.getTextChannelById(channelID).sendMessage(msg).queue();
                    event.getTextChannel().sendMessage("Message was sent").queue();
                } catch (Exception e) {
                    event.getTextChannel().sendMessage(help(1)).queue();
                }
                break;
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
            case "checkstreamer":
            case "cs":
            case "checkstreams":
            case "checkstream":
                event.getTextChannel().sendMessage("Checking for online streamers...").queue();
                Main.twitch.streamerCheckIteration();
                break;
            case "streamonline":
            case "onlines":
            case "onlinestream":
            case "online":
                event.getTextChannel().sendMessage("Currently online streamers: `" + Main.twitch.getIsOnline().toString() + "`").queue();
                break;
            case "getlink":
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

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.UTILITY;
    }
}
