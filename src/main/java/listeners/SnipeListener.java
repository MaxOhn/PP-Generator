package main.java.listeners;

import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.core.DBProvider;
import main.java.core.Main;
import net.dv8tion.jda.core.entities.Message;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SnipeListener {

    private Logger logger = Logger.getLogger(this.getClass());
    private Map<String, Message> channels;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    {
        try {
            channels = DBProvider.getSnipeChannels().stream().collect(Collectors.toMap(
                    channelID -> channelID, value -> null
            ));
            channels = DBProvider.getSnipeChannels().stream()
                    .collect(HashMap::new, (m, v) -> m.put(v, null), HashMap::putAll);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not retrieve snipe channels from database:");
            e.printStackTrace();
            channels = new HashMap<>();
        }
    }

    public void onStartUpdateRanking() {
        for (String channelID : channels.keySet()) {
            if (channels.get(channelID) != null) channels.get(channelID).delete().queue();
            Main.jda.getTextChannelById(channelID)
                    .sendMessage("Initiate rebuild...")
                    .queue(message -> channels.put(channelID, message));
        }
    }

    public void onSnipe(String mapID, String sniperID, String snipeeID) {
        String snipeeName = "User id " + snipeeID;
        String sniperName = "user id " + sniperID;
        String mapString = "https://osu.ppy.sh/b/" + mapID;
        try {
            sniperName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(sniperID)).build()).getUsername();
            snipeeName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(snipeeID)).build()).getUsername();
        } catch (Exception ignored) {}
        for (String channelID : channels.keySet()) {
            Main.jda.getTextChannelById(channelID).sendMessage(snipeeName + " was sniped by " + sniperName
                    + "\n" + mapString).queue();
        }
    }

    public void onClaim(String mapID, String claimerID) {
        String claimerName = "user id " + claimerID;
        String mapString = "https://osu.ppy.sh/b/" + mapID;
        try {
            claimerName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(claimerID)).build()).getUsername();
        } catch (Exception ignored) {}
        for (String channelID : channels.keySet()) {
            Main.jda.getTextChannelById(channelID).sendMessage("New first place is " + claimerName + "\n" + mapString).queue();
        }
    }

    public void onUpdateRankingProgress(int currentIdx, int totalAmount, int amountFailed) {
        String editedMessage = "Building: " + df.format((double)currentIdx/totalAmount) + "% ("
                + currentIdx + " of " + totalAmount + ") | " + amountFailed + " failed";
        if (ThreadLocalRandom.current().nextBoolean()) {
            for (Message message : channels.values())
                message.editMessage(editedMessage).queue();
        }
    }

}
