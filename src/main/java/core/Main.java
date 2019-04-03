package main.java.core;

import de.maxikg.osuapi.client.DefaultOsuClient;
import main.java.commands.Fun.*;
import main.java.commands.Osu.*;
import main.java.commands.Twitch.cmdAddStream;
import main.java.commands.Twitch.cmdRemoveStream;
import main.java.commands.Twitch.cmdTrackedStreams;
import main.java.commands.Utility.*;
import main.java.listeners.*;
import main.java.util.secrets;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import org.apache.log4j.PropertyConfigurator;

import javax.security.auth.login.LoginException;
import java.util.HashSet;

public class Main {

    private static JDABuilder builder;
    private static JDA jda;
    public static DefaultOsuClient osu;
    public static TwitchHook twitch;
    public static DiscordLink discLink;
    public static FileInteractor fileInteractor;
    public static HashSet<String> runningLyrics;

    public static void main(String[] args) throws LoginException, InterruptedException {

        String log4jConfPath = secrets.log4jPath;
        PropertyConfigurator.configure(log4jConfPath);
        builder = new JDABuilder((AccountType.BOT));
        login();
        addListeners();
        addCommands();
        jda = builder.build().awaitReady();
    }

    private static void login() {
        builder.setToken(secrets.discordToken);
        builder.setAutoReconnect(true);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setGame(Game.playing(secrets.gameName));
    }

    private static void addListeners() {
        builder.addEventListener(new readyListener());
        builder.addEventListener(new reconnectListener());
        builder.addEventListener(new commandListener());
        builder.addEventListener(new serverJoinListener());
        builder.addEventListener(new roleLostListener());
    }

    private static void addCommands() {

        // osu! standard
        commandHandler.commands.put("recent", new cmdRecent());
        commandHandler.commands.put("r", new cmdRecent());
        commandHandler.commands.put("compare", new cmdCompare());
        commandHandler.commands.put("c", new cmdCompare());

        // osu! taiko
        commandHandler.commands.put("recenttaiko", new cmdRecentTaiko());
        commandHandler.commands.put("rt", new cmdRecentTaiko());
        commandHandler.commands.put("comparetaiko", new cmdCompareTaiko());
        commandHandler.commands.put("ct", new cmdCompareTaiko());

        // osu! mania
        commandHandler.commands.put("recentmania", new cmdRecentMania());
        commandHandler.commands.put("rm", new cmdRecentMania());
        commandHandler.commands.put("comparemania", new cmdCompareMania());
        commandHandler.commands.put("cm", new cmdCompareMania());

        // osu! general
        commandHandler.commands.put("link", new cmdLink());
        commandHandler.commands.put("recentbest", new cmdRecentBest());
        commandHandler.commands.put("rb", new cmdRecentBest());
        commandHandler.commands.put("top", new cmdTop());
        commandHandler.commands.put("scores", new cmdScores());
        commandHandler.commands.put("topscores", new cmdTopScores());
        commandHandler.commands.put("osutop", new cmdTopScores());
        commandHandler.commands.put("nochoke", new cmdNoChoke());
        commandHandler.commands.put("nochokes", new cmdNoChoke());
        commandHandler.commands.put("nc", new cmdNoChoke());

        // twitch
        commandHandler.commands.put("addstream", new cmdAddStream());
        commandHandler.commands.put("adds", new cmdAddStream());
        commandHandler.commands.put("trackedstreams", new cmdTrackedStreams());
        commandHandler.commands.put("trackeds", new cmdTrackedStreams());
        commandHandler.commands.put("removestream", new cmdRemoveStream());
        commandHandler.commands.put("removes", new cmdRemoveStream());

        // fun
        commandHandler.commands.put("dance", new cmdDance());
        commandHandler.commands.put("lyrics", new cmdLyrics());
        commandHandler.commands.put("fireflies", new cmdFireflies());
        commandHandler.commands.put("pretender", new cmdPretender());
        commandHandler.commands.put("fireandflames", new cmdFireAndFlames());
        commandHandler.commands.put("catchit", new cmdCatchit());
        commandHandler.commands.put("flamingo", new cmdFlamingo());
        commandHandler.commands.put("ding", new cmdDing());
        commandHandler.commands.put("brainpower", new cmdBrainpower());

        // utility
        commandHandler.commands.put("devtool", new cmdDevTool());
        commandHandler.commands.put("dt", new cmdDevTool());
        commandHandler.commands.put("ping", new cmdPing());
        commandHandler.commands.put("p", new cmdPing());
        commandHandler.commands.put("roll", new cmdRoll());
        commandHandler.commands.put("help", new cmdHelp());
        commandHandler.commands.put("h", new cmdHelp());
        commandHandler.commands.put("prune", new cmdPrune());
        commandHandler.commands.put("purge", new cmdPrune());
        commandHandler.commands.put("setauthorityroles", new cmdSetAuthorityRoles());
        commandHandler.commands.put("authorityroles", new cmdSetAuthorityRoles());
        commandHandler.commands.put("authorities", new cmdSetAuthorityRoles());
    }

    public static void sendCustomMessage(String text, String id) {
        if (secrets.RELEASE)
            jda.getTextChannelById(id).sendMessage(text).queue();
        else
            jda.getTextChannelById(secrets.devChannelID).sendMessage(text).queue();
    }
}
