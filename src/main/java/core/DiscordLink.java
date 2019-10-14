package main.java.core;

import main.java.util.secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;

/*
    Keeping track of which discord user id is linked to which osu username
 */
public class DiscordLink {

    private HashMap<String, String> link;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DiscordLink() {
        try {
            link = secrets.WITH_DB ? DBProvider.getDiscosu() : new HashMap<>();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not load links:", e);
            link = new HashMap<>();
        }
    }

    public HashMap<String, String> getLink() {
        return link;
    }

    // Add link to database
    public boolean addLink(String discordID, String osuname) {
        try {
            if (secrets.WITH_DB)
                DBProvider.addLink(discordID, osuname);
            link.put(discordID, osuname);
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not link user:");
            e.printStackTrace();
        }
        return false;
    }

    // Remove link from database
    public boolean removeLink(String discordID) {
        try {
            if (secrets.WITH_DB)
                DBProvider.removeLink(discordID);
            link.remove(discordID);
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Could not remove link:");
            e.printStackTrace();
        }
        return false;
    }

    // Given discord user id, return osu username
    public String getOsu(String discordID) {
        return link.get(discordID);
    }
}
