package main.java.core;

import com.oopsjpeg.osu4j.ApprovalState;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.util.Utility;
import main.java.util.secrets;

import java.sql.*;
import java.util.*;

public class DBProvider {

    private static String addReplacer(String str) {
        return str.replaceAll("'", "ö");
    }

    private static String removeReplacer(String str) {
        return str.replaceAll("ö", "'");
    }

    /*
     * ------------------------
     *      map ranking
     * ------------------------
     */

    public static TreeSet<Integer> getMapIds() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select mapID from mapRanking");
        TreeSet<Integer> mapIDs = new TreeSet<>();
        while (rs.next())
            mapIDs.add(rs.getInt("mapID"));
        stmnt.close();
        c.close();
        return mapIDs;
    }

    public static void addMaps(List<Integer> mapIDs) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        PreparedStatement stmnt = c.prepareStatement("insert into mapRanking (mapID) values (?)");
        for (Integer i : mapIDs) {
            stmnt.setString(1, i + "");
            stmnt.addBatch();
        }
        stmnt.executeBatch();
        stmnt.close();
        c.close();
    }

    /*
     * ------------------------
     *      beatmap info
     * ------------------------
     */

    public static void addBeatmap(OsuBeatmap map) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        try {
            stmnt.execute("insert into beatmapInfo values ('" + map.getID() + "','"
                    + map.getBeatmapSetID() + "','"
                    + map.getApproved().getID() + "','"
                    + addReplacer(map.getVersion()) + "','"
                    + addReplacer(map.getTitle()) + "','"
                    + addReplacer(map.getArtist()) + "',"
                    + map.getMode().getID() + ","
                    + map.getDrain() + ","
                    + map.getSize() + ","
                    + map.getApproach() + ","
                    + map.getOverall() + ","
                    + map.getDifficulty() + ","
                    + map.getTotalLength() + ","
                    + map.getHitLength() + ","
                    + map.getBPM() + ","
                    + map.getMaxCombo() + ",'"
                    + addReplacer(map.getCreatorName()) + "','"
                    + addReplacer(map.getSource()) + "','"
                    + addReplacer(Utility.toMySqlString(map.getApprovedDate())) + "','"
                    + addReplacer(Utility.toMySqlString(map.getLastUpdate())) + "')" );
        } catch (SQLIntegrityConstraintViolationException ignore) {}
        stmnt.close();
        c.close();

    }

    public static OsuBeatmap getBeatmap(int mapID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from beatmapInfo where mapID='" + mapID + "'");
        rs.next();
        OsuBeatmap m = new OsuBeatmap(Main.osu);
        m.setBeatmapID(rs.getInt("mapID"));
        m.setBeatmapSetID(rs.getInt("mapSetID"));
        switch (rs.getInt("approved")) {
            case 4:
            case 3: m.setApproved(ApprovalState.QUALIFIED); break;
            case 2: m.setApproved(ApprovalState.APPROVED); break;
            case 1: m.setApproved(ApprovalState.RANKED); break;
            case 0: m.setApproved(ApprovalState.PENDING); break;
            case -1: m.setApproved(ApprovalState.WIP); break;
            default: m.setApproved(ApprovalState.GRAVEYARD); break;
        }
        m.setVersion(removeReplacer(rs.getString("version")));
        m.setTitle(removeReplacer(rs.getString("title")));
        m.setArtist(removeReplacer(rs.getString("artist")));
        switch (rs.getInt("mode")) {
            case 0: m.setMode(GameMode.STANDARD); break;
            case 1: m.setMode(GameMode.TAIKO); break;
            case 2: m.setMode(GameMode.CATCH_THE_BEAT); break;
            default: m.setMode(GameMode.MANIA); break;
        }
        m.setDiffDrain((float)rs.getDouble("hp"));
        m.setDiffSize((float)rs.getDouble("cs"));
        m.setDiffApproach((float)rs.getDouble("ar"));
        m.setDiffOverall((float)rs.getDouble("od"));
        m.setDifficultyrating(rs.getFloat("stars"));
        m.setTotalLength(rs.getInt("tlength"));
        m.setHitLength(rs.getInt("hlength"));
        m.setBpm(rs.getInt("bpm"));
        m.setMaxCombo(rs.getInt("combo"));
        m.setCreatorName(removeReplacer(rs.getString("creator")));
        m.setSource(removeReplacer(rs.getString("source")));
        m.setApprovedDate(Utility.parseDate(rs.getString("date")));
        m.setApprovedDate(Utility.parseDate(rs.getString("updated")));
        stmnt.close();
        c.close();
        return m;
    }

    /*
     * ------------------------
     *      pp ratings
     * ------------------------
     */

    private static String prepareMods(String mods) {
        return mods.equals("") ? "NM" :
                mods.replace("NC", "DT")
                        .replace("SD", "")
                        .replace("PF", "");
    }

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
            stmnt.close();
            c.close();
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
        try {
            stmnt.execute("insert into ppRatings values ('" + mapID + "', -1, -1, -1, -1, -1, -1)");
        } catch (SQLIntegrityConstraintViolationException ignore) {}
        stmnt.close();
        c.close();
    }

    public static void  addModsPp(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update ppRatings set " + mods + "=" + rating + " where mapID='" + mapID + "'");
        stmnt.close();
        c.close();
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
        try {
            stmnt.execute("insert into starRatings values ('" + mapID + "', -1, -1)");
        } catch (SQLIntegrityConstraintViolationException ignore) {}
        stmnt.close();
        c.close();
    }

    public static void addModsStars(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update starRatings set " + mods + "=" + rating + " where mapID='" + mapID + "'");
        stmnt.close();
        c.close();
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
        stmnt.close();
        c.close();
    }

    public static void setAuthorityRoles(String serverID, List<String> roles) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update serverProperties set authorityRoles = '" + String.join("##", roles) +
                "' where server='" + serverID + "'");
        stmnt.close();
        c.close();
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
        stmnt.close();
        c.close();
    }

    static void removeLink(String discordID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from discosu where discord='" + discordID + "'");
        stmnt.close();
        c.close();
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
        stmnt.close();
        c.close();
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
        stmnt.close();
        c.close();
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
        stmnt.close();
        c.close();
        return channels;
    }

    static void removeStreamer(String streamer, String channelID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from twitch where name='" + streamer + "' and channel='" + channelID + "'");
        stmnt.close();
        c.close();
    }

    static void addStreamer(String streamer, String channelID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into twitch(name, channel) values ('" + streamer + "', '" + channelID + "')");
        stmnt.close();
        c.close();
    }

    public static HashMap<String, ArrayList<String>> getTwitch() throws SQLException, ClassNotFoundException {
        HashMap<String, ArrayList<String>> streamers = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from twitch");
        while(rs.next()) {
            String twitchName = rs.getString("name");
            String channelID = rs.getString("channel");
            if (streamers.containsKey(twitchName))
                streamers.get(twitchName).add(channelID);
            else
                streamers.put(twitchName, new ArrayList<>(Collections.singletonList(channelID)));
        }
        stmnt.close();
        c.close();
        return streamers;
    }
}
