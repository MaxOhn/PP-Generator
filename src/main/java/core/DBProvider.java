package main.java.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.oopsjpeg.osu4j.ApprovalState;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.util.Utility;
import main.java.commands.Fun.BgGameRanking;
import main.java.util.secrets;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/*
    Responsible for all communication with the database
 */
public class DBProvider {

    // Replacing / removing certain symbols so that they're more easily stored in the database
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

    // Update score and rating of bg players
    public static void updateBgPlayerRanking(HashSet<BgGameRanking> rankings) throws ClassNotFoundException, SQLException {
        if (rankings.isEmpty()) return;
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        PreparedStatement stmnt = c.prepareStatement("update bgGame set score=score+? , rating=rating+? where discord=?");
        for (BgGameRanking ranking : rankings) {
            stmnt.setInt(1, ranking.getScore());
            stmnt.setDouble(2, ranking.getRating());
            stmnt.setLong(3, ranking.getDiscordUser());
            stmnt.addBatch();
        }
        int[] results = stmnt.executeBatch();
        Iterator<BgGameRanking> it = rankings.iterator();
        for (int result : results) {
            BgGameRanking current = it.next();
            if (result == 0) {
                Statement stmntNew = c.createStatement();
                stmntNew.execute("insert into bgGame (discord, score, rating) values (" +
                        current.getDiscordUser() + ", " + current.getScore() + ", " + current.getRating() + ")");
                stmntNew.close();
            }
        }
        stmnt.close();
        c.close();
    }

    // Return score and rating of a single bg player
    public static HashMap<String, Double> getBgPlayerStats(long discord) throws ClassNotFoundException, SQLException {
        double minRating = getMinRating();
        minRating = minRating < 0 ? minRating * -1 : 0;
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select score, rating from bgGame where discord=" + discord);
        double score, rating;
        if (rs.next()) {
            score = rs.getInt("score");
            rating = rs.getDouble("rating") + minRating;
        } else {
            stmnt = c.createStatement();
            stmnt.execute("insert into bgGame (discord, score, rating) values (" +
                    discord + ", 0, 0)");
            score = rating = 0;
        }
        stmnt.close();
        c.close();
        HashMap<String, Double> stats = new HashMap<>();
        stats.put("Score", score);
        stats.put("Rating", rating);
        return stats;
    }

    // Return all ratings, sorted descendingly
    public static TreeMap<Long, Double> getBgTopRatings() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select discord, rating from bgGame order by rating desc");
        TreeMap<Long, Double> topRatings = new TreeMap<>();
        while (rs.next()) {
            topRatings.put(rs.getLong("discord"), rs.getDouble("rating"));
        }
        stmnt.close();
        c.close();
        return topRatings;
    }

    // Return all scores, sorted desceningly
    public static TreeMap<Long, Double> getBgTopScores() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select discord, score from bgGame order by score desc");
        TreeMap<Long, Double> topScores = new TreeMap<>();
        while (rs.next()) {
            topScores.put(rs.getLong("discord"), (double)rs.getInt("score"));
        }
        stmnt.close();
        c.close();
        return topScores;
    }

    // Return the minimal rating of all bg players
    public static double getMinRating() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select min(rating) from bgGame");
        rs.next();
        double result = rs.getDouble(1);
        stmnt.close();
        c.close();
        return result;
    }

    /*
     * ------------------------
     *     role assigns
     * ------------------------
     */

    // Return a hashmap of a guild-channel-msg hash mapped to a role id
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

    // Add a new guild-channel-msg hash to a role id
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

    // Remove the given guild-channel-msg hash from the database
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

    // Retrieve the ids of all unchecked users and the time when they joined
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

    // Add a new unchecked user
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

    // Remove an unchecked user i.e. the user left, was kicked, or got checked
    public static void removeUncheckedUser(String discordID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from uncheckedUsers where discord='" + discordID + "'");
        stmnt.close();
        c.close();
    }

    /* (inactive)
     * ------------------------
     *      snipe channels
     * ------------------------
     */

    // Return channel ids of channels in which the bot notifies when snipes happened
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

    // Add a new snipe-notification channel to the database
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

    // Retrieve all map id's for which national leaderboard data is saved
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

    // Update the saved national leaderboard data of the given map
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

    // Retrieve the national leaderboard data for the given map
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

    // Return the whole national leaderboard data
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

    // Add a new map to the national leaderboard data
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

    // Add a new map and a ranking to the national leaderboard data
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

    private static final int VERSION_LENGTH = 64;
    private static final int TITLE_LENGTH = 64;
    private static final int ARTIST_LENGTH = 64;
    private static final int SOURCE_LENGTH = 64;

    // Save data of a new beatmap
    public static void addBeatmap(OsuBeatmap map) throws ClassNotFoundException, SQLException, IllegalArgumentException {
        if (map.getVersion().length() > VERSION_LENGTH)
            throw new IllegalArgumentException("version");
        else if (map.getTitle().length() > TITLE_LENGTH)
            throw new IllegalArgumentException("title");
        else if (map.getArtist().length() > ARTIST_LENGTH)
            throw new IllegalArgumentException("artist");
        else if (map.getSource().length() > SOURCE_LENGTH)
            throw new IllegalArgumentException("source");
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

    // Retrieve all maps that correspond to the given OsuScore collection
    public static HashMap<Integer, OsuBeatmap> getBeatmaps(Collection<OsuScore> scores) throws ClassNotFoundException, SQLException {
        String queryCondition = scores
                .stream()
                .map(score -> "mapID='" + score.getBeatmapID() + "'")
                .collect(Collectors.joining(" OR "));
        HashMap<Integer, OsuBeatmap> maps = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from beatmapInfo where " + queryCondition);
        while (rs.next()) {
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
            maps.putIfAbsent(m.getID(), m);
        }
        stmnt.close();
        c.close();
        return maps;
    }

    // Retrieve data of the given mapID and return its map
    public static OsuBeatmap getBeatmap(int mapID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select * from beatmapInfo where mapID='" + mapID + "'");
        if (!rs.next())
            throw new SQLException("No beatmap in beatmapInfo table with mapID " + mapID);
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

    /* (saves the following mod cominations: nm, hd, hr, dt, hdhr, hddt)
     * ------------------------
     *      pp ratings
     * ------------------------
     */

    // Modify mod string so that database can handle it more easily
    private static String prepareMods(String mods) {
        String newMods = mods.replace("NC", "DT")   // dt and nc are the same pp
                        .replace("SD", "")          // sd doesn't influence pp
                        .replace("PF", "");         // pf doesn't influence pp
        return newMods.equals("") ? "NM" : newMods;
    }

    // Given a map and mods, return the maximal pp value
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

    // Return the amount of saved pp scores for the given mod combination
    public static int getAmount(String mods) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select count(" + mods + ") as " + mods + " from ppRatings where " + mods + "!=-1");
        rs.next();
        return rs.getInt(mods);
    }
    // Return the average pp value for all maps with the given mod combination
    public static double getAverage(String mods) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select avg(" + mods + ") as " + mods + " from ppRatings where " + mods + "!=-1");
        rs.next();
        return rs.getDouble(mods);
    }

    // Add a new map to the pp database
    public static void addMapPp(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        double nm = -1, hd = -1, hr = -1, dt = -1, hdhr = -1, hddt = -1;
        switch (prepareMods(mods)) {
            case "NM": nm = rating; break;
            case "HD": hd = rating; break;
            case "HR": hr = rating; break;
            case "DT": dt = rating; break;
            case "HDHR": hdhr = rating; break;
            case "HDDT": hddt = rating; break;
        }
        try {
            stmnt.execute("insert into ppRatings(mapID, NM, HD, HR, DT, HDHR, HDDT) values ('"
                    + mapID + "', " + nm + ", " + hd + ", " + hr + ", " + dt + ", " + hdhr + ", " + hddt + ")");
        } catch (SQLIntegrityConstraintViolationException ignore) {}
        stmnt.close();
        c.close();
    }

    // Add mod pp for a given map
    public static void  addModsPp(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        mods = prepareMods(mods);
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update ppRatings set " + mods + "=" + rating + " where mapID='" + mapID + "'");
        stmnt.close();
        c.close();
    }

    /* (saves the following mod cominations: hr, dt)
     * ------------------------
     *      star ratings
     * ------------------------
     */

    // Return the star rating for the given map and mod combination
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
            if (e.getErrorCode() == 1054)
                throw new IllegalArgumentException("Mods '" + mods + "' not available");
            throw e;
        }
    }

    // Add a new map to star rating database
    public static void addMapStars(int mapID, String mods, double rating) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        double hr = -1, dt = -1;
        switch (prepareMods(mods)) {
            case "HR": hr = rating; break;
            case "DT": dt = rating; break;
        }
        try {
            stmnt.execute("insert into starRatings(mapID, HR, DT) values ('"
                    + mapID + "', " + hr + ", " + dt + ")");
        } catch (SQLIntegrityConstraintViolationException ignore) {}
        stmnt.close();
        c.close();
    }

    // Add new star rating value for the given map and mod combination
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

    public static void addServer(String serverID, String[] authorities) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert ignore into serverProperties(server, lyricsAvailable, authorityRoles) values ('" + serverID
                + "', true, '" + String.join("##", Arrays.asList(authorities)) + "')");
    }

    // Return whether the song commands are activated on the given server
    public static boolean getLyricsState(String serverID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select lyricsAvailable from serverProperties where server='" + serverID + "'");
        rs.next();
        return rs.getBoolean("lyricsAvailable");
    }

    // Update permission to use song commands
    public static void setLyricsState(String serverID, boolean bool) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update serverProperties set lyricsAvailable = " + bool + " where server='" + serverID + "'");
    }

    // Return name of authority roles for the given server
    public static String[] getAuthorityRoles(String serverID) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        ResultSet rs = stmnt.executeQuery("select authorityRoles from serverProperties where server='" + serverID + "'");
        rs.next();
        return rs.getString("authorityRoles").split("##");
    }

    // Update authority roles for given server
    public static void setAuthorityRoles(String serverID, String[] roles) throws ClassNotFoundException, SQLException {
        setAuthorityRoles(serverID, Arrays.asList(roles));
    }

    // Update authority roles for given server
    public static void setAuthorityRoles(String serverID, List<String> roles) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("update serverProperties set authorityRoles = '" + String.join("##", roles) +
                "' where server='" + serverID + "'");
        stmnt.close();
        c.close();
    }

    /* (linking discord user ids to osu usernames manually)
     * ------------------------
     *       manual links
     * ------------------------
     */

    // Get all manual links
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

    /* (linking discord user id to osu username)
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

    // Return all links
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
     *         streams
     * ------------------------
     */

    // Remove streamer - channel combination from database
    static void removeStreamer(String streamer, String channelID, String platform) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("delete from streams where name='" + streamer + "' and channel='" + channelID + "' and platform='" + platform + "'");
        stmnt.close();
        c.close();
    }

    // Add streamer - channel combination to database
    static void addStreamer(String streamer, String channelID, String platform) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection c = DriverManager.getConnection(secrets.dbPath, secrets.dbUser, secrets.dbPw);
        Statement stmnt = c.createStatement();
        stmnt.execute("insert into streams(name, channel, platform) values ('" + streamer + "', '" + channelID + "', '" + platform + "')");
        stmnt.close();
        c.close();
    }

    // Return all mixer streamer names and their notif-channels
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

    // Return all twitch streamer names and their notif-channels
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
