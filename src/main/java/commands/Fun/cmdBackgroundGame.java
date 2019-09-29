package main.java.commands.Fun;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import com.oopsjpeg.osu4j.OsuBeatmapSet;
import com.oopsjpeg.osu4j.backend.EndpointBeatmapSet;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import de.gesundkrank.jskills.*;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.DBProvider;
import main.java.core.Main;
import main.java.util.Pair;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class cmdBackgroundGame implements ICommand {

    private Logger logger = Logger.getLogger(cmdBackgroundGame.class);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private HashMap<Long, BackgroundGame> runningGames = new HashMap<>();
    private HashMap<Long, HashMap<Long, Pair<Long, BgGameRanking>>> activePlayers = new HashMap<>();
    private Queue<Integer> previous = new LinkedList<>();
    private File[] files = new File(getSourcePath()).listFiles();
    private final int checkInterval = 3000;
    private GameInfo gameInfo = new GameInfo(1500, 500, 240, 10, 0.1);

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(0));
            return false;
        }
        if (args.length != 1) {
            new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(2));
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
            case "bigger":
            case "b":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                runningGames.get(event.getChannel().getIdLong()).increaseRadius();
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT)
                        .send(runningGames.get(event.getChannel().getIdLong()).getResult(), "Guess the background.png");
                break;
            case "resolve":
            case "solve":
            case "r":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                resolveGame(event.getChannel(), "", 0, true, 0);
                break;
            case "hint":
            case "h":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT)
                        .send(runningGames.get(event.getChannel().getIdLong()).getHint());
                break;
            default:
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(2));
        }
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
        new BotMessage(channel, BotMessage.MessageType.TEXT)
                .send("Here's the next one:", bgGame.getResult(), "Guess the background.png");
        runningGames.put(channel.getIdLong(), bgGame);
    }

    private void resolveGame(MessageChannel channel, String winner, double similarity, boolean autostart, long winnerID) {
        BackgroundGame game = runningGames.get(channel.getIdLong());
        if (game == null) return;
        String text = "Full background: https://osu.ppy.sh/beatmapsets/";
        if (!winner.isEmpty()) {
            text = (similarity == 1
                    ? "Gratz `" + winner + "`, you guessed it"
                    : "You were close enough `" + winner + "`, gratz")
                    + " :)\nMapset: https://osu.ppy.sh/beatmapsets/";
            updateRankingOfInactive(channel.getIdLong());
            updateRankingOfActive(channel.getIdLong(), winnerID);
        }
        new BotMessage(channel, BotMessage.MessageType.TEXT).send(
                text + game.mapsetid,
                game.getReveal(),
                "Guess the background.png"
        );
        game.dispose();
        runningGames.remove(channel.getIdLong());
        if (autostart)
            startGame(channel);
    }

    private void updateRankingOfInactive(long channel) {
        HashSet<Long> inactivePlayers = activePlayers.get(channel).values().stream()
                .filter(pair -> System.currentTimeMillis() - pair.left > 15000
                        && !runningGames.get(channel).players.contains(pair.right.getDiscordUser()))
                .map(pair -> pair.right.getDiscordUser())
                .collect(Collectors.toCollection(HashSet::new));
        if (inactivePlayers.size() > 0)
            activePlayers.get(channel).keySet().removeIf(inactivePlayers::contains);
        try {
            DBProvider.updateBgPlayerRanking(inactivePlayers.stream()
                    .map(playerID -> activePlayers.get(channel).get(playerID).right)
                    .collect(Collectors.toCollection(HashSet::new))
            );
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Could not update player rankings", e);
        }
    }

    private void updateRankingOfActive(long channel, long winner) {
        if (activePlayers.get(channel).size() > 1) {
            List<ITeam> teams = activePlayers.get(channel).values().stream()
                    .map(pair -> new Team(new Player<>(pair.getRight().getDiscordUser()), pair.getRight().getRating()))
                    .collect(Collectors.toList());
            int[] teamRanks = activePlayers.get(channel).values().stream()
                    .map(pair -> pair.right.getDiscordUser() == winner ? 1 : 2)
                    .mapToInt(x -> x).toArray();
            Map<IPlayer, Rating> newRatings = TrueSkillCalculator.calculateNewRatings(gameInfo, teams, teamRanks);
            for (IPlayer player : newRatings.keySet()) {
                long playerID = Long.parseLong(player.toString());
                activePlayers.get(channel).get(playerID).right.uptate(newRatings.get(player));
            }
        }
        activePlayers.get(channel).get(winner).right.incrementScore();
    }

    private void checkChat(long channel, long mapsetid) {
        BackgroundGame game = runningGames.get(channel);
        //logger.info("Checking chat (bgGame: " + (game != null ? game.mapsetid : "null") + ", mapset: " + mapsetid + ")");
        if (game == null || game.mapsetid != mapsetid) {
            //logger.info("Stop checking because new map");
            return;
        }
        game.channel.getHistoryAfter(game.lastMsgChecked, 10).queue(messageHistory -> {
            if (messageHistory.isEmpty()) {
                //logger.info("No new messages -> schedule in " + (checkInterval / 1000d) + " seconds");
                scheduler.schedule(() -> checkChat(channel, mapsetid), checkInterval, TimeUnit.MILLISECONDS);
                return;
            }
            ListIterator<Message> it = messageHistory.getRetrievedHistory().listIterator(messageHistory.size());
            while (it.hasPrevious()) {
                Message msg = it.previous();
                if (msg.getAuthor() == Main.jda.getSelfUser()) continue;
                if (secrets.WITH_DB) {
                    game.players.add(msg.getAuthor().getIdLong());
                    if (!activePlayers.get(channel).containsKey(msg.getAuthor().getIdLong())) {
                        try {
                            activePlayers.get(channel).put(
                                    msg.getAuthor().getIdLong(),
                                    new Pair<>(System.currentTimeMillis(), DBProvider.getBgPlayerRanking(msg.getAuthor().getIdLong()))
                            );
                        } catch (SQLException | ClassNotFoundException e) {
                            //logger.error("Could not retrieve player rating", e);
                        }
                    } else
                        activePlayers.get(channel).get(msg.getAuthor().getIdLong()).left = System.currentTimeMillis();
                }
                String content = msg.getContentRaw().toLowerCase();
                if (game.title.equals(content)) {
                    //logger.info("Resolved title " + game.title + " == " + content);
                    resolveGame(game.channel, msg.getAuthor().getName(), 1, true, msg.getAuthor().getIdLong());
                    return;
                }
                if (game.titleSplit.length > 0) {
                    String[] contentSplit = content.split(" ");
                    int hit = 0;
                    for (String c : contentSplit) {
                        for (String t : game.titleSplit) {
                            if (c.equals(t)) {
                                if ((hit += c.length()) > 8) {
                                    //logger.info("Resolved title " + game.title + " ~= " + content + " (" + hit + ")");
                                    resolveGame(game.channel, msg.getAuthor().getName(), 0.9, true, msg.getAuthor().getIdLong());
                                    return;
                                }
                            }
                        }
                    }
                }
                double similarity = utilGeneral.similarity(game.title, content);
                if (similarity > 0.5) {
                    //logger.info("Resolved title " + game.title + " ~= " + content + " (" + similarity + ")");
                    resolveGame(game.channel, msg.getAuthor().getName(), similarity, true, msg.getAuthor().getIdLong());
                    return;
                }
                if (!game.artistGuessed) {
                    if (game.artist.equals(content)) {
                        new BotMessage(game.channel, BotMessage.MessageType.TEXT)
                                .send("That's the correct artist `" + msg.getAuthor().getName() + "`, can you get the title too?");
                        game.artistGuessed = true;
                    } else if (similarity < 0.3 && utilGeneral.similarity(game.artist, content) > 0.5) {
                        new BotMessage(game.channel, BotMessage.MessageType.TEXT)
                                .send("`" + msg.getAuthor().getName() + "` got the artist almost correct, it's actually `" +
                                        game.artist + "` but can you get the title?");
                        game.artistGuessed = true;
                    }
                }
            }
            game.lastMsgChecked = it.next().getIdLong();
            //logger.info("Done checking -> immediate restart");
            checkChat(channel, mapsetid);
        });
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "background -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + getName() + " <start/bigger/hint/resolve>` to play the background-guessing game." +
                        "\nWith `start` I will select and show part of a new background for you to guess." +
                        "\nWith `bigger` I will slightly enlargen the currently shown part of the background to make it easier." +
                        "\nWith `hint` I will provide you some clues for the map title." +
                        "\nWith `resolve` I will show you the entire background and its mapset";
            case 1:
                return "You must first start a new round via `" + statics.prefix + getName() + " start`" + help;
            case 2:
                return "This command requires exactly one argument which must either be `start`, `bigger`, `hint`, or `resolve`" + help;
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
        return "background";
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
        private int x;
        private int y;
        private int radius;
        private ScheduledFuture<?> timeLeft;
        private long lastMsgChecked;
        private MessageChannel channel;
        private String artist;
        private String title;
        private long mapsetid;
        private String[] titleSplit;
        private boolean artistGuessed;
        private int hintDepth;
        private HashSet<Long> players;

        BackgroundGame(MessageChannel channel, BufferedImage origin, OsuBeatmapSet mapset) {
            this.channel = channel;
            this.origin = origin;
            artistGuessed = false;
            hintDepth = 0;
            lastMsgChecked = channel.getLatestMessageIdLong();
            radius = 100;
            x = ThreadLocalRandom.current().nextInt(radius, origin.getWidth() - radius);
            y = ThreadLocalRandom.current().nextInt(radius, origin.getHeight() - radius);
            artist = removeParenthesis(mapset.getArtist().toLowerCase());
            title = removeParenthesis(mapset.getTitle().toLowerCase());
            mapsetid = mapset.getBeatmapSetID();
            titleSplit = title.split(" ");
            players = new HashSet<>();
            timeLeft = scheduler.schedule(() -> resolveGame(channel, "", 0, false, 0), 5, TimeUnit.MINUTES);
            scheduler.schedule(() -> checkChat(channel.getIdLong(), mapsetid), checkInterval + 500, TimeUnit.MILLISECONDS);
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
                        hint = "Here's my second hint: The artist is called `" + artist + "`";
                        artistGuessed = true;
                        return hint;
                    }
                case 2: {
                    hint = "My last hint for you: The title looks like this `";
                    String[] titleCpy = new String[titleSplit.length];
                    titleCpy[0] = title.substring(0, 1) + StringUtils.repeat("▢", titleSplit[0].length() - 1);
                    for (int i = 1; i < titleSplit.length; i++)
                        titleCpy[i] = StringUtils.repeat("▢", titleSplit[i].length());
                    return hint + String.join(" ", titleCpy) + "`";
                }
                default: {
                    hint = "All the hints I give you: The artist is `" + artist + "` and the title looks like `";
                    String[] titleCpy = new String[titleSplit.length];
                    titleCpy[0] = title.substring(0, 1) + StringUtils.repeat("▢", titleSplit[0].length() - 1);
                    for (int i = 1; i < titleSplit.length; i++)
                        titleCpy[i] = StringUtils.repeat("▢", titleSplit[i].length());
                    return hint + String.join(" ", titleCpy) + "`";
                }
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

        void dispose() {
            timeLeft.cancel(false);
        }
    }
}