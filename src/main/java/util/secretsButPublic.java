package main.java.util;

/*
 *  !!!
 *  Copy this file, then rename it and the class to "secrets" instead of "secretsButPublic"
 *  Then fill in the variables
 *  !!!
 */
public class secretsButPublic {

    // false -> 'debug mode', paths like those in statics.java probably require you to keep this false for the start
    // Feel free to check all usages of this variable to customize its use yourself
    public static final boolean RELEASE = false;

    // Unless you've set up your own database to store various data like discord-osu links, beatmap info,
    // tracked twitch streamers, ... this should stay false
    public static final boolean WITH_DB = false;

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
    public static final String dbPath = "";

    // Can be replaced for any other dev's discord id
    public static final String badewanne3ID = "";

    // RoleLostListener sends a message to this channel whenever anyone lost a certain role
    public static final String welcomeMsgChannelID = "";

    // Server id where testing things takes place
    public static final String devGuildID = "";

    // Check lost roles, inactive members, ... on this server
    public static final String mainGuildID = "";

    // Path to the directory that contains all the .osu beatmap files
    // Can be an empty folder in which all requested maps will be saved
    public static final String mapPath = "";
}
