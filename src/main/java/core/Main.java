package main.java.core;

import de.maxikg.osuapi.client.DefaultOsuClient;
import main.java.commands.*;
import main.java.listeners.*;
import main.java.util.secrets;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import org.apache.log4j.PropertyConfigurator;

import javax.security.auth.login.LoginException;

public class Main {

    private static JDABuilder builder;
    private static JDA jda;
    public static DefaultOsuClient osu;
    public static TwitchHook twitch;
    public static DiscordLink discLink;
    public static FileInteractor fileInteractor;

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
    }

    private static void addCommands() {
        commandHandler.commands.put("t", new cmdTest());
        commandHandler.commands.put("ping", new cmdPing());
        commandHandler.commands.put("p", new cmdPing());
        commandHandler.commands.put("roll", new cmdRoll());
        commandHandler.commands.put("recent", new cmdRecent());
        commandHandler.commands.put("r", new cmdRecent());
        commandHandler.commands.put("recentmania", new cmdRecentMania());
        commandHandler.commands.put("rmania", new cmdRecentMania());
        commandHandler.commands.put("rm", new cmdRecentMania());
        commandHandler.commands.put("addstream", new cmdAddStream());
        commandHandler.commands.put("adds", new cmdAddStream());
        commandHandler.commands.put("trackedstreams", new cmdTrackedStreams());
        commandHandler.commands.put("trackeds", new cmdTrackedStreams());
        commandHandler.commands.put("removestream", new cmdRemoveStream());
        commandHandler.commands.put("removes", new cmdRemoveStream());
        commandHandler.commands.put("compare", new cmdCompare());
        commandHandler.commands.put("c", new cmdCompare());
        commandHandler.commands.put("help", new cmdHelp());
        commandHandler.commands.put("h", new cmdHelp());
        commandHandler.commands.put("link", new cmdLink());
        commandHandler.commands.put("dance", new cmdDance());
    }

    static void streamerOnline(String test, String id) {
        jda.getTextChannelById(Long.parseLong(id)).sendMessage(test).queue();
    }
}
