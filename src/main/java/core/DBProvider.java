package main.java.core;

import main.java.util.secrets;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class DBProvider {

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

    static HashMap<String, ArrayList<String>> getTwitch() throws SQLException, ClassNotFoundException {
        HashMap<String, ArrayList<String>> streamers = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("SELECT * FROM twitch");
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
