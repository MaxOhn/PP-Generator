package main.java.core;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;

public class DiscordLink {

    private HashMap<String, String> link;
    private static Logger logger = Logger.getLogger("DiscordLink");

    public DiscordLink() {
        try {
            this.link = DBProvider.getDiscosu();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not load links: " + e);
            this.link = new HashMap<>();
        }
    }

    public boolean addLink(String discordID, String osuname) {
        try {
            DBProvider.addLink(discordID, osuname);
            link.put(discordID, osuname);
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not link user: " + e);
        }
        return false;
    }

    public String getOsu(String discordID) {
        return link.get(discordID);
    }

}
