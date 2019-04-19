package main.java.commands.Snipe;

import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cmdCheckSnipe implements ICommand {
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
        Pattern p = Pattern.compile("((https:\\/\\/osu\\.ppy\\.sh\\/b\\/)([0-9]{1,8})(.*))|((https:\\/\\/osu\\.ppy\\.sh\\/beatmapsets\\/[0-9]*\\#[a-z]*\\/)([0-9]{1,8})(.*))");
        String mapID = "-1";
        try {
            Matcher m = p.matcher(args[0]);
            if (m.find()) {
                mapID = m.group(3);
                if (mapID == null) mapID = m.group(7);
            }
            if (mapID.equals("-1")) mapID = Integer.parseInt(args[0]) + "";
        } catch (Exception e) {
            event.getTextChannel().sendMessage(help(1)).queue();
            return;
        }
        if (mapID.equals("-1")) {
            event.getTextChannel().sendMessage("Could not retrieve map id from the command. Have you specified the map id and not only the map set id?").queue();
            return;
        }
        String leaderID = Main.snipeManager.checkScores(Integer.parseInt(mapID));
        switch (leaderID) {
            case "-1":
                event.getTextChannel().sendMessage("No map with this id").queue();
                break;
            case "-2":
                event.getTextChannel().sendMessage("No national scores on the map\nhttps://osu.ppy.sh/b/" + mapID).queue();
                break;
            case "-3":
                event.getTextChannel().sendMessage("Something went wrong, blame bade :p").queue();
                break;
            case "-4":
                switch (Main.snipeManager.getMessageId(event.getTextChannel())) {
                    case "-2":
                        String userName = "User id "  + leaderID;
                        try {
                            userName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(leaderID).build()).getUsername();
                        } catch (OsuAPIException ignored) {}
                        event.getTextChannel().sendMessage(userName + " is the current leader\nhttps://osu.ppy.sh/b/" + mapID).queue();
                        break;
                    default: break;
                }
                break;
            default:
                String userName = "User id "  + leaderID;
                try {
                    userName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(leaderID)).build()).getUsername();
                } catch (OsuAPIException ignored) {}
                event.getTextChannel().sendMessage(userName + " is the current leader\nhttps://osu.ppy.sh/b/" + mapID).queue();
                break;
        }
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "checksnipe -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "checksnipe <map id | map url>` to make me check who currently holds " +
                        "national first place on the specified map.";
            case 2:
                return "The first argument must either be the link to a beatmap e.g. `https://osu.ppy.sh/b/1613091&m=0`, or just the id of the beatmap" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.SNIPE;
    }
}
