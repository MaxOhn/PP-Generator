package main.java.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.oopsjpeg.osu4j.ApprovalState;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.util.Utility;
import main.java.util.secrets;
import main.java.commands.Fun.BgGameRanking;

import java.sql.*;
import java.time.ZonedDateTime;
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
     *     background game
     * ------------------------
     */

    public static void updateBgPlayerRanking(HashSet<BgGameRanking> rankings) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        PreparedStatement stmnt = c.prepareStatement("update bgGame set score=? , mu=? , sigma=? , rating=? where discord=?");
        for (BgGameRanking ranking : rankings) {
            stmnt.setInt(1, ranking.getScore());
            stmnt.setDouble(2, ranking.getRating().getMean());
            stmnt.setDouble(3, ranking.getRating().getStandardDeviation());
            stmnt.setDouble(4, ranking.getRating().getConservativeRating());
            stmnt.setLong(5, ranking.getDiscordUser());
            stmnt.addBatch();
        }
        stmnt.executeBatch();
        stmnt.close();
        c.close();
    }

    public static BgGameRanking getBgPlayerRanking(long discord) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from bgGame where discord=" + discord);
        int score; double mu, sigma;
        if (rs.next()) {
            score = rs.getInt("score");
            mu = rs.getDouble("mu");
            sigma = rs.getDouble("sigma");
        } else {
            stmnt = c.createStatement();
            stmnt.executeQuery("insert into bgGame (discord, score, mu, sigma, rating) values (" +
                    discord + ", 0, 1500, 500, 0)");
            score = 0; mu = 1500; sigma = 500;
        }
        stmnt.close();
        c.close();
        return new BgGameRanking(discord, score, mu, sigma);
    }

    public static HashMap<Long, Double> getBgTopRatings(int amount) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select discord, rating from bgGame order by rating desc limit " + amount);
        HashMap<Long, Double> topRatings = new HashMap<>();
        while (rs.next()) {
            topRatings.put(rs.getLong("discord"), rs.getDouble("rating"));
        }
        stmnt.close();
        c.close();
        return topRatings;
    }

    public static HashMap<Long, Double> getBgTopScores(int amount) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select discord, score from bgGame order by score desc limit " + amount);
        HashMap<Long, Double> topScores = new HashMap<>();
        while (rs.next()) {
            topScores.put(rs.getLong("discord"), (double)rs.getInt("score"));
        }
        stmnt.close();
        c.close();
        return topScores;
    }

    /*
     * ------------------------
     *     role assigns
     * ------------------------
     */

    public static HashMap<Integer, String> getRoleAssigns() throws ClassNotFoundException, SQLException {
        HashMap<Integer, String> roleAssigns = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from roleAssigns");
        while(rs.next()) {
            roleAssigns.put(rs.getInt("hash"), rs.getString("roleID"));
        }
        stmnt.close();
        c.close();
        return roleAssigns;
    }

    public static void addRoleAssign(int hash, String roleID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into roleAssigns(hash, roleID) values (" + hash
                + ", '" + roleID + "')"
                + " on duplicate key update roleID='" + roleID + "'");
        stmnt.close();
        c.close();
    }

    public static void removeRoleAssign(int hash) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from roleAssigns where hash=" + hash);
        stmnt.close();
        c.close();
    }

    /*
     * ------------------------
     *     unchecked users
     * ------------------------
     */

    public static HashMap<String, ZonedDateTime> getUncheckedUsers() throws ClassNotFoundException, SQLException {
        HashMap<String, ZonedDateTime> users = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from uncheckedUsers");
        while(rs.next()) {
            users.put(rs.getString("discord"), Utility.parseDate(rs.getString("date")));
        }
        stmnt.close();
        c.close();
        return users;
    }

    public static void addUncheckedUser(String discordID, ZonedDateTime date) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into uncheckedUsers(discord, date) values ('" + discordID
                + "', '" + Utility.toMySqlString(date) + "')"
                + " on duplicate key update date='" + Utility.toMySqlString(date) + "'");
        stmnt.close();
        c.close();
    }

    public static void removeUncheckedUser(String discordID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from uncheckedUsers where discord='" + discordID + "'");
        stmnt.close();
        c.close();
    }

    /*
     * ------------------------
     *      snipe channels
     * ------------------------
     */

    public static HashSet<String> getSnipeChannels() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select channelID from snipeChannels");
        HashSet<String> channels = new HashSet<>();
        while (rs.next())
            channels.add(rs.getString("channelID"));
        stmnt.close();
        c.close();
        return channels;
    }

    public static void addSnipeChannel(String channelID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into snipeChannels values (" + channelID + ")");
        stmnt.close();
        c.close();
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

    public static void updateRanking(String mapID, String[] rankings) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        PreparedStatement stmnt = c.prepareStatement("update mapRanking set R1=?, R2=?, R3=?, R4=?, R5=?, R6=?, R7=?, R8=?, R9=?, R10=? where mapID='"
                + mapID + "'");
        for (int i = 0; i < 10; i++) {
            stmnt.setString(i + 1, i < rankings.length ? rankings[i] : null);
        }
        stmnt.executeUpdate();
        stmnt.close();
        c.close();
    }

    public static String[] getRanking(String mapID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from mapRanking where mapID='" + mapID + "'");
        rs.next();
        List<String> ranking = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String userID = rs.getString(i + 2);
            if (userID != null && !userID.isEmpty())
                ranking.add(userID);
        }
        stmnt.close();
        c.close();
        return ranking.toArray(new String[0]);
    }

    public static TreeMap<Integer, String[]> getRankings() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from mapRanking");
        TreeMap<Integer, String[]> rankings = new TreeMap<>();
        while (rs.next()) {
            int id = rs.getInt("mapID");
            List<String> ranking = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String userID = rs.getString(i + 2);
                if (userID != null && !userID.isEmpty())
                    ranking.add(userID);
            }
            rankings.put(id, ranking.toArray(new String[0]));
        }
        stmnt.close();
        c.close();
        return rankings;
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

    public static void addMapWithRankings(Integer mapID, String[] rankings) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        PreparedStatement stmnt = c.prepareStatement("insert into mapRanking values ('" + mapID + "' , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        for (int i = 0; i < 10; i++) {
            stmnt.setString(i + 1, i < rankings.length ? rankings[i] : null);
        }
        stmnt.execute();
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
                    + Utility.toMySqlString(map.getApprovedDate()) + "','"
                    + Utility.toMySqlString(map.getLastUpdate()) + "')" );
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
        String date = rs.getString("date");
        m.setApprovedDate(date.equals("null") ? null : Utility.parseDate(date));
        date = rs.getString("updated");
        m.setApprovedDate(date.equals("null") ? null : Utility.parseDate(date));
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
        String newMods = mods.equals("") ? "NM" :
                mods.replace("NC", "DT")
                        .replace("SD", "")
                        .replace("PF", "");
        return newMods.equals("") ? "NM" : newMods;
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
     *       manual links
     * ------------------------
     */

    public static BiMap<String, String> getManualLinks() throws SQLException, ClassNotFoundException {
        BiMap<String, String> links = HashBiMap.create();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from manualLinks");
        while(rs.next()) {
            links.put(rs.getString("discord"), rs.getString("osu"));
        }
        stmnt.close();
        c.close();
        return links;
    }

    /*
     * ------------------------
     *         discosu
     * ------------------------
     */

    static void addLink(String discordID, String osuname) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into discosu(discord, osu) values ('" + discordID + "', '" + osuname + "')"
                + " on duplicate key update osu='" + osuname + "'");
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

    static void removeStreamer(String streamer, String channelID, String platform) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from streams where name='" + streamer + "' and channel='" + channelID + "' and platform='" + platform + "'");
        stmnt.close();
        c.close();
    }

    static void addStreamer(String streamer, String channelID, String platform) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into streams(name, channel, platform) values ('" + streamer + "', '" + channelID + "', '" + platform + "')");
        stmnt.close();
        c.close();
    }

    public static HashMap<String, ArrayList<String>> getMixer() throws SQLException, ClassNotFoundException {
        HashMap<String, ArrayList<String>> streamers = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from streams");
        while(rs.next()) {
            if (rs.getString("platform").equals("mixer")) {
                String mixerName = rs.getString("name");
                String channelID = rs.getString("channel");
                if (streamers.containsKey(mixerName))
                    streamers.get(mixerName).add(channelID);
                else
                    streamers.put(mixerName, new ArrayList<>(Collections.singletonList(channelID)));
            }
        }
        stmnt.close();
        c.close();
        return streamers;
    }

    public static HashMap<String, ArrayList<String>> getTwitch() throws SQLException, ClassNotFoundException {
        HashMap<String, ArrayList<String>> streamers = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from streams");
        while(rs.next()) {
            if (rs.getString("platform").equals("twitch")) {
                String twitchName = rs.getString("name");
                String channelID = rs.getString("channel");
                if (streamers.containsKey(twitchName))
                    streamers.get(twitchName).add(channelID);
                else
                    streamers.put(twitchName, new ArrayList<>(Collections.singletonList(channelID)));
            }
        }
        stmnt.close();
        c.close();
        return streamers;
    }
}
