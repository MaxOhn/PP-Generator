package main.java.core;

import com.oopsjpeg.osu4j.backend.Osu;
import main.java.commands.Fun.*;
import main.java.commands.Osu.Fruits.*;
import main.java.commands.Osu.Mania.*;
import main.java.commands.Osu.Standard.*;
import main.java.commands.Osu.Taiko.*;
import main.java.commands.Osu.*;
import main.java.commands.Streams.*;
import main.java.commands.Utility.*;
import main.java.listeners.*;
import main.java.util.secrets;
import main.java.util.statics;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;

import javax.security.auth.login.LoginException;

public class Main {

    private static JDABuilder builder;
    public static JDA jda;
    public static Osu osu;
    public static CustomOsu customOsu;
    public static StreamHook streamHook;
    public static DiscordLink discLink;
    public static MemberHandler memberHandler;
    public static ReactionHandler reactionHandler;

    public static void main(String[] args) throws LoginException, InterruptedException {
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
        builder.setGame(Game.playing(statics.gameName));
    }

    // Listeners for various events happening in discord
    private static void addListeners() {
        builder.addEventListener(new ReadyListener());
        builder.addEventListener(new ReconnectListener());
        builder.addEventListener(new CommandListener());
        builder.addEventListener(new ServerJoinListener());
        builder.addEventListener(new RoleLostListener());
        builder.addEventListener(new MemberJoinListener());
        builder.addEventListener(new ReactionListener());
    }

    private static void addCommands() {

        // osu! general
        commandHandler.commands.put("link", new cmdLink());
        commandHandler.commands.put("compare", new cmdScores());
        commandHandler.commands.put("c", new cmdScores());
        commandHandler.commands.put("scores", new cmdScores());
        commandHandler.commands.put("leaderboard", new cmdMapLeaderboard());
        commandHandler.commands.put("lb", new cmdMapLeaderboard());
        commandHandler.commands.put("globalleaderboard", new cmdGlobalLeaderboard());
        commandHandler.commands.put("glb", new cmdGlobalLeaderboard());
        commandHandler.commands.put("simulate", new cmdSimulateMap());
        commandHandler.commands.put("s", new cmdSimulateMap());

        // osu! standard
        commandHandler.commands.put("recent", new cmdRecent());
        commandHandler.commands.put("r", new cmdRecent());
        commandHandler.commands.put("rs", new cmdRecent());
        commandHandler.commands.put("recentbest", new cmdRecentBest());
        commandHandler.commands.put("rb", new cmdRecentBest());
        commandHandler.commands.put("top", new cmdTop());
        commandHandler.commands.put("topscores", new cmdTop());
        commandHandler.commands.put("osutop", new cmdTop());
        commandHandler.commands.put("nochoke", new cmdNoChoke());
        commandHandler.commands.put("nochokes", new cmdNoChoke());
        commandHandler.commands.put("nc", new cmdNoChoke());
        commandHandler.commands.put("sotarks", new cmdSotarks());
        commandHandler.commands.put("ss", new cmdSS());
        commandHandler.commands.put("recentleaderboard", new cmdRecentLeaderboard());
        commandHandler.commands.put("rlb", new cmdRecentLeaderboard());
        commandHandler.commands.put("recentgloballeaderboard", new cmdRecentGlobalLeaderboard());
        commandHandler.commands.put("rglb", new cmdRecentGlobalLeaderboard());
        commandHandler.commands.put("common", new cmdCommonScores());
        commandHandler.commands.put("simulaterecent", new cmdSimulateRecent());
        commandHandler.commands.put("sr", new cmdSimulateRecent());
        commandHandler.commands.put("wi", new cmdWhatIf());
        commandHandler.commands.put("whatif", new cmdWhatIf());
        commandHandler.commands.put("pp", new cmdPP());
        commandHandler.commands.put("rank", new cmdRank());
        commandHandler.commands.put("reach", new cmdReach());
        commandHandler.commands.put("osu", new cmdProfile());
        commandHandler.commands.put("osuprofile", new cmdProfile());
        commandHandler.commands.put("profileosu", new cmdProfile());

        // osu! mania
        commandHandler.commands.put("recentmania", new cmdRecentMania());
        commandHandler.commands.put("rm", new cmdRecentMania());
        commandHandler.commands.put("recentbestmania", new cmdRecentBestMania());
        commandHandler.commands.put("rbm", new cmdRecentBestMania());
        commandHandler.commands.put("topmania", new cmdTopMania());
        commandHandler.commands.put("topm", new cmdTopMania());
        commandHandler.commands.put("recentmanialeaderboard", new cmdRecentManiaLeaderboard());
        commandHandler.commands.put("rmlb", new cmdRecentManiaLeaderboard());
        commandHandler.commands.put("recentmaniagloballeaderboard", new cmdRecentManiaGlobalLeaderboard());
        commandHandler.commands.put("rmglb", new cmdRecentManiaGlobalLeaderboard());
        commandHandler.commands.put("commonmania", new cmdCommonScoresMania());
        commandHandler.commands.put("commonm", new cmdCommonScoresMania());
        commandHandler.commands.put("simulaterecentmania", new cmdSimulateRecentMania());
        commandHandler.commands.put("srm", new cmdSimulateRecentMania());
        commandHandler.commands.put("wim", new cmdWhatIfMania());
        commandHandler.commands.put("whatifmania", new cmdWhatIfMania());
        commandHandler.commands.put("ppm", new cmdPPMania());
        commandHandler.commands.put("ppmania", new cmdPPMania());
        commandHandler.commands.put("rankm", new cmdRankMania());
        commandHandler.commands.put("rankmania", new cmdRankMania());
        commandHandler.commands.put("ratio", new cmdRatio());
        commandHandler.commands.put("ratios", new cmdRatio());
        commandHandler.commands.put("reachmania", new cmdReachMania());
        commandHandler.commands.put("reachm", new cmdReachMania());
        commandHandler.commands.put("mania", new cmdProfileMania());
        commandHandler.commands.put("maniaprofile", new cmdProfileMania());
        commandHandler.commands.put("profilemania", new cmdProfileMania());

        // osu! taiko
        commandHandler.commands.put("recenttaiko", new cmdRecentTaiko());
        commandHandler.commands.put("rt", new cmdRecentTaiko());
        commandHandler.commands.put("recentbesttaiko", new cmdRecentBestTaiko());
        commandHandler.commands.put("rbt", new cmdRecentBestTaiko());
        commandHandler.commands.put("toptaiko", new cmdTopTaiko());
        commandHandler.commands.put("topt", new cmdTopTaiko());
        commandHandler.commands.put("recenttaikoleaderboard", new cmdRecentTaikoLeaderboard());
        commandHandler.commands.put("rtlb", new cmdRecentTaikoLeaderboard());
        commandHandler.commands.put("recenttaikogloballeaderboard", new cmdRecentTaikoGlobalLeaderboard());
        commandHandler.commands.put("rtglb", new cmdRecentTaikoGlobalLeaderboard());
        commandHandler.commands.put("commontaiko", new cmdCommonScoresTaiko());
        commandHandler.commands.put("commont", new cmdCommonScoresTaiko());
        commandHandler.commands.put("wit", new cmdWhatIfTaiko());
        commandHandler.commands.put("whatiftaiko", new cmdWhatIfTaiko());
        commandHandler.commands.put("ppt", new cmdPPTaiko());
        commandHandler.commands.put("pptaiko", new cmdPPTaiko());
        commandHandler.commands.put("rankt", new cmdRankTaiko());
        commandHandler.commands.put("ranktaiko", new cmdRankTaiko());
        commandHandler.commands.put("reachtaiko", new cmdReachTaiko());
        commandHandler.commands.put("reacht", new cmdReachTaiko());
        commandHandler.commands.put("taiko", new cmdProfileTaiko());
        commandHandler.commands.put("taikoprofile", new cmdProfileTaiko());
        commandHandler.commands.put("profiletaiko", new cmdProfileTaiko());

        // osu! ctb
        commandHandler.commands.put("recentctb", new cmdRecentFruits());
        commandHandler.commands.put("rc", new cmdRecentFruits());
        commandHandler.commands.put("recentbestctb", new cmdRecentBestFruits());
        commandHandler.commands.put("rbc", new cmdRecentBestFruits());
        commandHandler.commands.put("topctb", new cmdTopFruits());
        commandHandler.commands.put("topc", new cmdTopFruits());
        commandHandler.commands.put("recentctbleaderboard", new cmdRecentFruitsLeaderboard());
        commandHandler.commands.put("rclb", new cmdRecentFruitsLeaderboard());
        commandHandler.commands.put("recentctbgloballeaderboard", new cmdRecentFruitsGlobalLeaderboard());
        commandHandler.commands.put("rcglb", new cmdRecentFruitsGlobalLeaderboard());
        commandHandler.commands.put("commonctb", new cmdCommonScoresFruits());
        commandHandler.commands.put("commonc", new cmdCommonScoresFruits());
        commandHandler.commands.put("whatifctb", new cmdWhatIfFruits());
        commandHandler.commands.put("wic", new cmdWhatIfFruits());
        commandHandler.commands.put("ppctb", new cmdPPFruits());
        commandHandler.commands.put("ppc", new cmdPPFruits());
        commandHandler.commands.put("rankctb", new cmdRankFruits());
        commandHandler.commands.put("rankc", new cmdRankFruits());
        commandHandler.commands.put("reachctb", new cmdReachFruits());
        commandHandler.commands.put("reachc", new cmdReachFruits());
        commandHandler.commands.put("ctb", new cmdProfileFruits());
        commandHandler.commands.put("ctbprofile", new cmdProfileFruits());
        commandHandler.commands.put("profilectb", new cmdProfileFruits());

        // twitch
        commandHandler.commands.put("addstream", new cmdAddStream());
        commandHandler.commands.put("removestream", new cmdRemoveStream());
        commandHandler.commands.put("trackedstreams", new cmdTrackedStreams());
        commandHandler.commands.put("allstreamers", new cmdAllStreamers());
        commandHandler.commands.put("allstreams", new cmdAllStreamers());

        // fun
        commandHandler.commands.put("ding", new cmdDing());
        commandHandler.commands.put("dance", new cmdDance());
        commandHandler.commands.put("lyrics", new cmdLyrics());
        commandHandler.commands.put("catchit", new cmdCatchit());
        commandHandler.commands.put("1273", new cmdRockefeller());
        commandHandler.commands.put("flamingo", new cmdFlamingo());
        commandHandler.commands.put("bombsaway", new cmdBombsAway());
        commandHandler.commands.put("fireflies", new cmdFireflies());
        commandHandler.commands.put("pretender", new cmdPretender());
        commandHandler.commands.put("brainpower", new cmdBrainpower());
        commandHandler.commands.put("saygoodbye", new cmdSayGoodbye());
        commandHandler.commands.put("tijdmachine", new cmdTijdmachine());
        commandHandler.commands.put("rockefeller", new cmdRockefeller());
        commandHandler.commands.put("fireandflames", new cmdFireAndFlames());
        commandHandler.commands.put("bg", new cmdBackgroundGame());
        commandHandler.commands.put("bgm", new cmdBackgroundGameMania());

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
        commandHandler.commands.put("authorities", new cmdSetAuthorityRoles());
        commandHandler.commands.put("roleassign", new cmdRoleAssign());
    }
}
