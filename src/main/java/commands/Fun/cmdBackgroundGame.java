package main.java.commands.Fun;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import com.oopsjpeg.osu4j.OsuBeatmapSet;
import com.oopsjpeg.osu4j.backend.EndpointBeatmapSet;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class cmdBackgroundGame implements ICommand {

    private Logger logger = Logger.getLogger(cmdBackgroundGame.class);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private HashMap<Long, BackgroundGame> runningGames = new HashMap<>();
    private HashMap<Long, HashMap<Long, PlayerInfo>> activePlayers = new HashMap<>();
    private Queue<Integer> previous = new LinkedList<>();
    private File[] files = new File(getSourcePath()).listFiles();
    private int TIMEOUT_MINUTES = 2;
    private int TIME_TILL_INACTIVE = 60_000;
    private int ROUNDS_TILL_INACTIVE = 5;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        if (args.length != 1) {
            event.getChannel().sendMessage(help(2)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        switch (args[0].toLowerCase()) {
            case "start":
            case "s":
                startGame(event.getChannel());
                break;
            case "enhance":
            case "bigger":
            case "b":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    event.getChannel().sendMessage(help(1)).queue();
                    return;
                }
                runningGames.get(event.getChannel().getIdLong()).resetTimeout();
                runningGames.get(event.getChannel().getIdLong()).increaseRadius();
                event.getChannel().sendFile(
                        runningGames.get(event.getChannel().getIdLong()).getResult(),
                        "Guess the background.png"
                ).queue();
                break;
            case "resolve":
            case "solve":
            case "r":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    event.getChannel().sendMessage(help(1)).queue();
                    return;
                }
                resolveGame(event.getChannel(), false, true);
                break;
            case "hint":
            case "h":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    event.getChannel().sendMessage(help(1)).queue();
                    return;
                }
                runningGames.get(event.getChannel().getIdLong()).resetTimeout();
                event.getChannel().sendMessage(runningGames.get(event.getChannel().getIdLong()).getHint()).queue();
                break;
            case "stop":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    event.getChannel().sendMessage(help(1)).queue();
                    return;
                }
                runningGames.get(event.getChannel().getIdLong()).dispose();
                activePlayers.get(event.getChannel().getIdLong()).values().forEach(player -> player.roundsInactive = ROUNDS_TILL_INACTIVE);
                runningGames.get(event.getChannel().getIdLong()).players.clear();
                if (secrets.WITH_DB)
                    updateRankingOfInactive(event.getChannel().getIdLong());
                runningGames.remove(event.getChannel().getIdLong());
                event.getChannel().sendMessage("End of game, see you next time o/").queue();
                break;
            case "-stats":
                HashMap<String, Double> stats;
                try {
                    stats = DBProvider.getBgPlayerStats(event.getAuthor().getIdLong());
                } catch (ClassNotFoundException | SQLException e) {
                    logger.error("Could not retrieve stats for id " + event.getAuthor().getId(), e);
                    event.getChannel().sendMessage("Something went wrong, blame bade").queue();
                    return;
                }
                event.getChannel().sendMessage("`" + event.getAuthor().getName() + "` has guessed `" + stats.get("Score").intValue()
                + "` correctly and has a rating of `" + df.format(stats.get("Rating")) + "`").queue();
                break;
            case "-score":
            case "-rank":
            case "-ranking":
            case "-rating":
                if (event.getChannelType() == ChannelType.PRIVATE) {
                    event.getChannel().sendMessage("No highscore list in private messages").queue();
                    return;
                }
                boolean wantScore = args[0].toLowerCase().equals("-score");
                HashMap<Long, Double> topScores;
                try {
                    topScores = wantScore
                            ? DBProvider.getBgTopScores()
                            : DBProvider.getBgTopRatings();
                } catch (ClassNotFoundException | SQLException e) {
                    logger.error("Could not retrieve top " + (wantScore ? "scores" : "ratings"), e);
                    event.getChannel().sendMessage("Something went wrong, blame bade").queue();
                    return;
                }
                topScores.keySet().removeIf(id -> event.getGuild().getMemberById(id) == null || topScores.get(id) == 0);
                if (topScores.size() > 15)
                    topScores.keySet().retainAll(topScores.keySet().stream().limit(15).collect(Collectors.toCollection(HashSet::new)));
                if (!wantScore) {
                    double minRating;
                    try {
                        minRating = DBProvider.getMinRating();
                    } catch (ClassNotFoundException | SQLException e) {
                        logger.error("Could not retrieve min rating:", e);
                        event.getChannel().sendMessage("Something went wrong, blame bade").queue();
                        return;
                    }
                    if (minRating < 0)
                        topScores.replaceAll((k, v) -> v - minRating);
                }
                EmbedBuilder eb = new EmbedBuilder()
                        .setColor(Color.green)
                        .setAuthor("Top " + (wantScore ? "scores" : "ratings") + " in the background game:");
                eb.setDescription(buildMessage(event, utilGeneral.sortByValue(topScores)));
                event.getChannel().sendMessage(eb.build()).queue();
                break;
            default:
                event.getChannel().sendMessage(help(2)).queue();
        }
    }

    private String buildMessage(MessageReceivedEvent event, Map<Long, Double> ranking) {
        HashMap<Long, String> names = ranking.keySet().stream()
                .collect(HashMap::new, (m, id) -> m.put(id, event.getGuild().getMemberById(id).getEffectiveName()), HashMap::putAll);
        int longestNameLength = names.values().stream()
                .reduce("", (longest, next) -> next.length() > longest.length() ? next : longest).length();
        StringBuilder msg = new StringBuilder("```\n");
        String[] symbols = new String[] { "♔", "♕", "♖", "♗", "♘", "♙"};
        int idx = 0;
        for (Map.Entry<Long, Double> entry : ranking.entrySet()) {
            msg.append(++idx);
            msg.append(" ");
            if (idx <= symbols.length)
                msg.append(symbols[idx - 1]);
            else
                msg.append("  ");
            if (ranking.size() > 10 && idx < 10)
                msg.append(" ");
            msg.append(" # ");
            msg.append(names.get(entry.getKey()));
            msg.append(StringUtils.repeat(" ", longestNameLength - names.get(entry.getKey()).length() + 2));
            msg.append("=> ");
            msg.append(df.format(entry.getValue()));
            msg.append("\n");
        }
        return msg.append("```").toString();
    }

    private void startGame(MessageChannel channel) {
        runningGames.remove(channel.getIdLong());
        if (!activePlayers.containsKey(channel.getIdLong()))
            activePlayers.put(channel.getIdLong(), new HashMap<>());
        File image;
        BufferedImage origin;
        while (true) {
            image = files[ThreadLocalRandom.current().nextInt(files.length)];
            try {
                if (!previous.contains(image.hashCode()) && image.isFile()) {
                    origin = ImageIO.read(image);
                    break;
                }
            } catch (IOException e) {
                logger.warn("Error while selecting file: " + image.getName(), e);
            }
        }
        previous.add(image.hashCode());
        if (previous.size() > files.length / 2)
            previous.remove();
        OsuBeatmapSet mapset;
        try {
            mapset = Main.osu.beatmapSets.query(new EndpointBeatmapSet
                    .Arguments(Integer.parseInt(image.getName().substring(0, image.getName().indexOf('.')))));
        } catch (OsuAPIException e) {
            logger.error("Error while retrieving the mapset", e);
            startGame(channel);
            return;
        }
        BackgroundGame bgGame = new BackgroundGame(channel, origin, mapset);
        channel.sendMessage("Here's the next one:").addFile(bgGame.getResult(), "Guess the background.png").queue();
        runningGames.put(channel.getIdLong(), bgGame);
    }

    private void resolveGame(MessageChannel channel, boolean exact, boolean autostart) {
        BackgroundGame game = runningGames.get(channel.getIdLong());
        if (game == null) return;
        game.dispose();
        String text = "Full background: https://osu.ppy.sh/beatmapsets/";
        if (!game.winnerName.isEmpty()) {
            text = (exact
                    ? "Gratz `" + game.winnerName + "`, you guessed it"
                    : "You were close enough `" + game.winnerName + "`, gratz")
                    + " :)\nMapset: https://osu.ppy.sh/beatmapsets/";
            if (secrets.WITH_DB) {
                updateRankingOfInactive(channel.getIdLong());
                updateRankingOfActive(channel.getIdLong());
            }
        }
        channel.sendMessage(text + game.mapsetid).addFile(game.getReveal(), "Guess the background.png").queue();
        runningGames.remove(channel.getIdLong());
        if (autostart)
            startGame(channel);
    }

    private void updateRankingOfInactive(long channel) {
        BackgroundGame game = runningGames.get(channel);
        for (PlayerInfo player : activePlayers.get(channel).values()) {
            if (game.players.contains(player.rating.getDiscordUser()))
                player.roundsInactive = 0;
            else
                player.roundsInactive++;
        }
        HashSet<Long> inactivePlayers = activePlayers.get(channel).values().stream()
                .filter(player -> (System.currentTimeMillis() - player.lastSeen > TIME_TILL_INACTIVE || player.roundsInactive >= ROUNDS_TILL_INACTIVE)
                        && !runningGames.get(channel).players.contains(player.rating.getDiscordUser()))
                .map(pair -> pair.rating.getDiscordUser())
                .collect(Collectors.toCollection(HashSet::new));
        if (inactivePlayers.size() > 0) {
            try {
                DBProvider.updateBgPlayerRanking(inactivePlayers.stream()
                        .map(playerID -> activePlayers.get(channel).get(playerID).rating)
                        .collect(Collectors.toCollection(HashSet::new))
                );
            } catch (ClassNotFoundException | SQLException e) {
                logger.error("Could not update player rankings", e);
            }
            activePlayers.get(channel).keySet().removeIf(inactivePlayers::contains);
        }
    }

    private void updateRankingOfActive(long channel) {
        BackgroundGame game = runningGames.get(channel);
        int numPlayers = activePlayers.get(channel).size();
        if (numPlayers > 1) {
            double ratingValue = getRatingValue(numPlayers) / (1 - numPlayers);
            activePlayers.get(channel).get(game.winner).rating.uptate(ratingValue * (1 - numPlayers));
            for (PlayerInfo player : activePlayers.get(channel).values()) {
                if (player.rating.getDiscordUser() == game.winner) continue;
                player.rating.uptate(ratingValue);
            }
            for (long player : game.correctButTooLate)
                activePlayers.get(channel).get(player).rating.uptate(ratingValue * (1 - numPlayers) / 10);
        }
        activePlayers.get(channel).get(game.winner).rating.incrementScore();
    }

    private double getRatingValue(int numPlayers) {
        return 1D/(Math.pow(0.95, numPlayers * 5)) - 1;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + getName() + " -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + getName() + " [start/bigger/hint/resolve/stop] [-score] [-rating] [-stats <user mention>]` to play the background-guessing game." +
                        "\nWith `start` I will select and show part of a new background for you to guess." +
                        "\nWith `bigger` I will slightly enlargen the currently shown part of the background to make it easier." +
                        "\nWith `hint` I will provide you some clues for the map title." +
                        "\nWith `resolve` I will show you the entire background and its mapset" +
                        "\nWith `stop` I will stop the game" +
                        "\nIf the first argument is `-score`, I will display the score leaderboard for this server i.e. who's most addicted :^)" +
                        "\nIf the first argument is `-rating`, I will display the \"elo\" leaderboard for this server i.e. who guesses fastest in 2+ player rounds" +
                        "\nIf the first argument is `-stats`, I will show your current score and rating";
            case 1:
                return "You must first start a new round via `" + statics.prefix + getName() + " start`" + help;
            case 2:
                return "This command requires exactly one argument which must either be `start`, `bigger`, `hint`, `resolve`, `-score`, `-rating`, or `-stats`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.FUN;
    }

    public String getSourcePath() {
        return statics.bgGamePath;
    }

    public String getName() {
        return "bg";
    }

    private static String removeParenthesis(String str) {
        String newStr = str;
        if (str.contains("(") && str.contains(")")) {
            newStr = str.substring(0, str.indexOf("(")) + str.substring(str.indexOf(")") + 1);
        }
        int idx = newStr.indexOf("feat.");
        if (idx == -1)
            idx = newStr.indexOf("ft.");
        return (idx != -1 ? newStr.substring(0, idx) : newStr).trim();
    }

    private class BackgroundGame {
        private BufferedImage origin;
        private MessageChannel channel;
        private int x;
        private int y;
        private ScheduledFuture<?> timeLeft;
        private String artist;
        private String title;
        private long mapsetid;
        private String[] titleSplit;
        private String hintTitle;
        private ChatReader chatReader;

        private boolean artistGuessed = false;
        private int hintDepth = 0;
        private int radius = 100;
        private HashSet<Long> players = new HashSet<>();
        private List<Integer> hintIndices = new ArrayList<>();
        private String winnerName = "";
        private long winner = 0;
        private HashSet<Long> correctButTooLate = new HashSet<>();

        BackgroundGame(MessageChannel channel, BufferedImage origin, OsuBeatmapSet mapset) {
            this.channel = channel;
            this.origin = origin;
            x = ThreadLocalRandom.current().nextInt(radius, origin.getWidth() - radius);
            y = ThreadLocalRandom.current().nextInt(radius, origin.getHeight() - radius);
            artist = removeParenthesis(mapset.getArtist().toLowerCase());
            title = removeParenthesis(mapset.getTitle().toLowerCase());
            char[] titleArray = title.toCharArray();
            for (int i = 1; i < titleArray.length; i++) {
                if (titleArray[i] != ' ') {
                    hintIndices.add(i);
                }
            }
            titleSplit = title.split(" ");
            String[] titleCpy = new String[titleSplit.length];
            titleCpy[0] = title.substring(0, 1) + StringUtils.repeat("▢", titleSplit[0].length() - 1);
            for (int i = 1; i < titleSplit.length; i++)
                titleCpy[i] = StringUtils.repeat("▢", titleSplit[i].length());
            hintTitle = String.join(" ", titleCpy);
            mapsetid = mapset.getBeatmapSetID();
            timeLeft = scheduler.schedule(() -> resolveGame(channel, false, false), TIMEOUT_MINUTES, TimeUnit.MINUTES);
            chatReader = new ChatReader(channel.getIdLong(), mapsetid);
            Main.jda.addEventListener(chatReader);
        }

        String getHint() {
            String hint;
            switch (hintDepth++) {
                case 0:
                    hint = "Let me give you a hint: The title has " + titleSplit.length + " word";
                    if (titleSplit.length != 1)
                        hint += "s";
                    hint += " and the starting letter is `" + title.substring(0, 1) + "`";
                    return hint;
                case 1:
                    if (!artistGuessed) {
                        hint = "Here's my second hint: The artist looks like `";
                        String[] artistSplit = artist.split(" ");
                        String[] artistCpy = new String[artistSplit.length];
                        artistCpy[0] = artist.substring(0, 1) + StringUtils.repeat("▢", artistSplit[0].length() - 1);
                        for (int i = 1; i < artistSplit.length; i++)
                            artistCpy[i] = StringUtils.repeat("▢", artistSplit[i].length());
                        artistGuessed = true;
                        return hint + String.join(" ", artistCpy) + "`";
                    }
                case 2:
                    return "Slowly constructing the title: `" + hintTitle + "`";
                default:
                    if (hintIndices.size() > 0) {
                        hint = "Slowly constructing the title: `";
                        int rndIdx = ThreadLocalRandom.current().nextInt(hintIndices.size());
                        int titleIdx = hintIndices.get(rndIdx);
                        hintTitle = hintTitle.substring(0, titleIdx)
                                + title.substring(titleIdx, titleIdx + 1)
                                + (titleIdx < title.length() - 1 ? hintTitle.substring(titleIdx + 1) : "");
                        hintIndices.remove(rndIdx);
                        return hint + hintTitle + "`";
                    } else return "Bruh the title is literally `" + title + "` xd";
            }
        }

        BufferedImage getSubimage() {
            int cx = Math.max(0, x - radius), cy = Math.max(0, y - radius);
            return origin.getSubimage(cx, cy,
                    Math.min(origin.getWidth(), x + radius) - cx,
                    Math.min(origin.getHeight(), y + radius) - cy
            );
        }

        byte[] getResult() {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            while (true) {
                try {
                    ImageIO.write(getSubimage(), "png", result);
                    return result.toByteArray();
                } catch (IOException e) {
                    logger.error("Error while writing result", e);
                }
            }
        }

        byte[] getReveal() {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            while (true) {
                try {
                    ImageIO.write(origin, "png", result);
                    return result.toByteArray();
                } catch (IOException e) {
                    logger.error("Error while revealing", e);
                }
            }
        }

        void increaseRadius() {
            radius += 75;
        }

        void resetTimeout() {
            timeLeft.cancel(false);
            timeLeft = scheduler.schedule(() -> resolveGame(channel, false, false), TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }

        void dispose() {
            timeLeft.cancel(false);
            Main.jda.removeEventListener(chatReader);
        }
    }

    private static class PlayerInfo {

        private Long lastSeen;
        private int roundsInactive;
        private BgGameRanking rating;

        PlayerInfo(Long lastSeen, BgGameRanking rating) {
            this.lastSeen = lastSeen;
            this.rating = rating;
            this.roundsInactive = 0;
        }
    }

    private class ChatReader extends ListenerAdapter {

        long channelID;
        long mapsetID;
        boolean foundWinner;

        private ChatReader(long channelID, long mapsetID) {
            super();
            this.channelID = channelID;
            this.mapsetID = mapsetID;
            this.foundWinner = false;
        }

        public void onMessageReceived(MessageReceivedEvent event) {
            long channel = event.getChannel().getIdLong();
            BackgroundGame game = runningGames.get(channel);
            if (channel != channelID || event.getAuthor().isBot() || game == null || game.mapsetid != mapsetID || (foundWinner && event.getMessage().getAuthor().getIdLong() == game.winner))
                return;
            Message msg = event.getMessage();
            if (secrets.WITH_DB) {
                game.players.add(msg.getAuthor().getIdLong());
                if (!activePlayers.get(channel).containsKey(msg.getAuthor().getIdLong())) {
                    activePlayers.get(channel).put(
                            msg.getAuthor().getIdLong(),
                            new PlayerInfo(System.currentTimeMillis(), new BgGameRanking(msg.getAuthor().getIdLong(), 0)));
                } else
                    activePlayers.get(channel).get(msg.getAuthor().getIdLong()).lastSeen = System.currentTimeMillis();
            }
            String content = msg.getContentRaw().toLowerCase();
            if (game.title.equals(content)) {
                if (!foundWinner) {
                    foundWinner = true;
                    game.winnerName = msg.getAuthor().getName();
                    game.winner = msg.getAuthor().getIdLong();
                    scheduler.schedule(() -> resolveGame(game.channel, true, true), 500, TimeUnit.MILLISECONDS);
                    return;
                } else game.correctButTooLate.add(msg.getAuthor().getIdLong());
                return;
            }
            if (game.titleSplit.length > 0) {
                String[] contentSplit = content.split(" ");
                int hit = 0;
                for (String c : contentSplit) {
                    for (String t : game.titleSplit) {
                        if (c.equals(t)) {
                            if ((hit += c.length()) > 8) {
                                if (!foundWinner) {
                                    foundWinner = true;
                                    game.winnerName = msg.getAuthor().getName();
                                    game.winner = msg.getAuthor().getIdLong();
                                    scheduler.schedule(() -> resolveGame(game.channel, false, true), 500, TimeUnit.MILLISECONDS);
                                    return;
                                } else game.correctButTooLate.add(msg.getAuthor().getIdLong());
                            }
                        }
                    }
                }
            }
            double similarity = utilGeneral.similarity(game.title, content);
            if (similarity > 0.5) {
                if (!foundWinner) {
                    foundWinner = true;
                    game.winnerName = msg.getAuthor().getName();
                    game.winner = msg.getAuthor().getIdLong();
                    scheduler.schedule(() -> resolveGame(game.channel, false, true), 500, TimeUnit.MILLISECONDS);
                    return;
                } else game.correctButTooLate.add(msg.getAuthor().getIdLong());
                return;
            }
            if (!game.artistGuessed) {
                if (game.artist.equals(content)) {
                    game.channel.sendMessage("That's the correct artist `" + msg.getAuthor().getName() + "`, can you get the title too?").queue();
                    game.artistGuessed = true;
                } else if (similarity < 0.3 && utilGeneral.similarity(game.artist, content) > 0.5) {
                    game.channel.sendMessage("`" + msg.getAuthor().getName() + "` got the artist almost correct, it's actually `"
                            + game.artist + "` but can you get the title?").queue();
                    game.artistGuessed = true;
                }
            }
        }
    }
}