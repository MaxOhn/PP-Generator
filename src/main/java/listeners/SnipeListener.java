package main.java.listeners;

import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.core.DBProvider;
import main.java.core.Main;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.HashSet;

public class SnipeListener {

    private Logger logger = Logger.getLogger(this.getClass());
    private HashSet<String> channels;

    {
        try {
            channels = DBProvider.getSnipeChannels();
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not retrieve snipe channels from database:");
            e.printStackTrace();
            channels = new HashSet<>();
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
        for (String channelID : channels) {
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
        for (String channelID : channels) {
            Main.jda.getTextChannelById(channelID).sendMessage("New first place is " + claimerName + "\n" + mapString).queue();
        }
    }

}
