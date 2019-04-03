package main.java.core;

import de.maxikg.osuapi.model.Beatmap;
import main.java.util.secrets;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DBProvider {

    private static String prepareMods(String mods) {
        return mods.equals("") ? "NM" : mods.replace("NC", "DT");
    }

    /*
     * ------------------------
     *      pp ratings
     * ------------------------
     */

    public static double getPpRating(int mapID, String mods)
            throws ClassNotFoundException, SQLException, IllegalAccessException, IllegalArgumentException {
        try {
            mods = prepareMods(mods);
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
            Statement stmnt = c.createStatement();
            ResultSet rs = stmnt.executeQuery("select " + mods + " from ppRatings where mapID='" + mapID + "'");
            rs.next();
            double response = rs.getDouble(mods);
            if (response > -1)
                return response;
            else
                throw new IllegalAccessException("Mods '" + mods + "' not yet calculated for mapID " + mapID);
        } catch (SQLException e) {
            switch (e.getErrorCode()) {
                case 1054: throw new IllegalArgumentException("Mods '" + mods + "' not available");
                default: throw e;
            }
        }
    }

    public static int getAmount(String mods) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select count(" + mods + ") as " + mods + " from ppRatings where " + mods + "!=-1");
        rs.next();
        return rs.getInt(mods);
    }

    public static double getAverage(String mods) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select avg(" + mods + ") as " + mods + " from ppRatings where " + mods + "!=-1");
        rs.next();
        return rs.getDouble(mods);
    }

    public static void addMapPp(int mapID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into ppRatings values ('" + mapID + "', -1, -1, -1, -1, -1, -1)");
    }

    public static void  addModsPp(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update ppRatings set " + mods + "=" + rating + " where mapID='" + mapID + "'");
    }

    /*
     * ------------------------
     *      star ratings
     * ------------------------
     */

    public static double getStarRating(int mapID, String mods)
            throws ClassNotFoundException, SQLException, IllegalAccessException, IllegalArgumentException {
        try {
            mods = prepareMods(mods);
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
            Statement stmnt = c.createStatement();
            ResultSet rs = stmnt.executeQuery("select " + mods + " from starRatings where mapID='" + mapID + "'");
            rs.next();
            double response = rs.getDouble(mods);
            if (response > -1)
                return response;
            else
                throw new IllegalAccessException("Mods '" + mods + "' not yet calculated for mapID " + mapID);
        } catch (SQLException e) {
            switch (e.getErrorCode()) {
                case 1054: throw new IllegalArgumentException("Mods '" + mods + "' not available");
                default: throw e;
            }
        }
    }

    public static void addMapStars(int mapID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into starRatings values ('" + mapID + "', -1, -1)");
    }

    public static void addModsStars(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update starRatings set " + mods + "=" + rating + " where mapID='" + mapID + "'");
    }

    /*
     * -------------------------
     *      server properties
     * -------------------------
     */

    public static boolean getLyricsState(String serverID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select lyricsAvailable from serverProperties where server='" + serverID + "'");
        rs.next();
        return rs.getBoolean("lyricsAvailable");
    }

    public static void setLyricsState(String serverID, boolean bool) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update serverProperties set lyricsAvailable = " + bool + " where server='" + serverID + "'");
    }

    public static String[] getAuthorityRoles(String serverID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select authorityRoles from serverProperties where server='" + serverID + "'");
        rs.next();
        return rs.getString("authorityRoles").split("##");
    }

    public static void setAuthorityRoles(String serverID, String[] roles) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update serverProperties set authorityRoles = '" + String.join("##", roles) +
                "' where server='" + serverID + "'");
    }

    public static void setAuthorityRoles(String serverID, List<String> roles) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update serverProperties set authorityRoles = '" + String.join("##", roles) +
                "' where server='" + serverID + "'");
    }

    /*
     * ------------------------
     *         discosu
     * ------------------------
     */

    public static String getOsuLink(String discordID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select osu from discosu where discord='" + discordID + "'");
        rs.next();
        return rs.getString("osu");
    }

    static void addLink(String discordID, String osuname) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from discosu where discord='" + discordID + "'");
        stmnt.execute("insert into discosu(discord, osu) values ('" + discordID + "', '" + osuname + "')");
    }

    static HashMap<String, String> getDiscosu() throws SQLException, ClassNotFoundException {
        HashMap<String, String> links = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("SELECT * FROM discosu");
        while(rs.next()) {
            links.put(rs.getString("discord"), rs.getString("osu"));
        }
        return links;
    }

    /*
     * ------------------------
     *         twitch
     * ------------------------
     */

    public static ArrayList<String> streamersForChannel(String channelID) throws ClassNotFoundException, SQLException {
        ArrayList<String> streamers = new ArrayList<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select name from twitch where channel='" + channelID + "'");
        while (rs.next())
            streamers.add(rs.getString("name"));
        return streamers;
    }

    public static ArrayList<String> channelsForStreamer(String streamer) throws ClassNotFoundException, SQLException {
        ArrayList<String> channels = new ArrayList<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select channel from twitch where name='" + streamer + "'");
        while (rs.next())
            channels.add(rs.getString("channel"));
        return channels;
    }

    static void removeStreamer(String streamer, String channelID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from twitch where name='" + streamer + "' and channel='" + channelID + "'");
    }

    static void addStreamer(String streamer, String channelID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into twitch(name, channel) values ('" + streamer + "', '" + channelID + "')");
    }

    public static HashMap<String, ArrayList<String>> getTwitch() throws SQLException, ClassNotFoundException {
        HashMap<String, ArrayList<String>> streamers = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from twitch");
        while(rs.next()) {
            if (streamers.containsKey(rs.getString("name")))
                streamers.get(rs.getString("name")).add(rs.getString("channel"));
            else
                streamers.put(rs.getString("name"),
                        new ArrayList<>(Collections.singletonList(rs.getString("channel"))));
        }
        return streamers;
    }
}
