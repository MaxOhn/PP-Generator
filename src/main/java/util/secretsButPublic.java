package main.java.util;

/*
 *  !!!
 *  Rename this class and file to "secrets" instead of "secretsButPublic"
 *  !!!
 */
public class secretsButPublic {

    // Whether the compiled program runs on my pc or my raspberry (false -> 'debug mode')
    public static final boolean RELEASE = true;

    // I have my database, you should probably turn it to false
    public static final boolean WITH_DB = true;

    // Provided by discord
    public static final String discordToken = "";

    // Retrieved from osu_session cookie of the osu website (requires you to have supporter)
    public static final String osu_session = "";

    // Provided by osu
    public static final String osuAPIkey = "";

    // Provided by twitch
    public static final String twitchClientID = "";

    // Only important if WITH_DB is true
    public static final String dbUser = "";
    public static final String dbPw = "";

    // Can be replaced for any other dev's discord id
    public static final String badewanne3ID = "";

    // RoleLostListener sends a message to this channel whenever anyone lost a certain role
    public static final String welcomeMsgChannelID = "";

    // Server id where testing things takes place
    public static final String devGuildID = "";

    // Check lost roles, inactive members, ... on this server
    public static final String mainGuildID = "";

    // Path to the directory that contains all the .osu beatmap files
    public static final String mapPath = "";

    // Only required if WITH_DB is true, path to the database
    public static final String dbPath = "";

    // Path to the directory containing all flag images
    public static final String flagPath = "";
}
