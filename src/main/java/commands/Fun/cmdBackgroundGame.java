package main.java.commands.Fun;

import com.auth0.jwt.internal.org.apache.commons.lang3.StringUtils;
import com.oopsjpeg.osu4j.OsuBeatmapSet;
import com.oopsjpeg.osu4j.backend.EndpointBeatmapSet;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

public class cmdBackgroundGame implements ICommand {

    private Logger logger = Logger.getLogger(cmdBackgroundGame.class);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private HashMap<Long, BackgroundGame> runningGames = new HashMap<>();
    private Queue<String> previous = new LinkedList<>();

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
        action(args, event, true);
    }

    public void action(String[] args, MessageReceivedEvent event, boolean autostart) {
        switch (args[0].toLowerCase()) {
            case "start":
            case "s":
                runningGames.remove(event.getChannel().getIdLong());
                File[] files = new File(getSourcePath()).listFiles();
                assert files != null;
                File image;
                BufferedImage origin;
                while (true) {
                    image = files[ThreadLocalRandom.current().nextInt(files.length)];
                    try {
                        if (!previous.contains(image.getName()) && image.isFile()) {
                            origin = ImageIO.read(image);
                            break;
                        }
                    } catch (IOException ignored) {
                        logger.warn("Error while selecting file: " + image.getName());
                    }
                }
                previous.add(image.getName());
                if (previous.size() > files.length / 2)
                    previous.remove();
                OsuBeatmapSet mapset;
                try {
                    mapset = Main.osu.beatmapSets.query(new EndpointBeatmapSet
                            .Arguments(Integer.parseInt(image.getName().substring(0, image.getName().indexOf('.')))));
                } catch (OsuAPIException e) {
                    logger.error("Error while retrieving the mapset", e);
                    action(args, event);
                    return;
                }
                BackgroundGame bgGame = new BackgroundGame(event, image, origin, mapset, scheduler);
                if (args.length > 1)
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send("Here's the next one:", bgGame.getResult(), "Guess the background.png");
                else
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(bgGame.getResult(), "Guess the background.png");
                runningGames.put(event.getChannel().getIdLong(), bgGame);
                break;
            case "bigger":
            case "b":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                runningGames.get(event.getChannel().getIdLong()).increaseRadius();
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(runningGames.get(event.getChannel().getIdLong()).getResult(), "Guess the background.png");
                break;
            case "resolve":
            case "solve":
            case "r":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                String text = "Full background: https://osu.ppy.sh/beatmapsets/";
                if (args.length > 1) {
                    String name = args[1];
                    double similarity = Double.parseDouble(args[2]);
                        text = (similarity == 1
                                ? "Gratz `" + name + "`, you guessed it"
                                : "You were close enough `" + name + "`, gratz")
                        + " :)\nMapset: https://osu.ppy.sh/beatmapsets/";
                }
                String imgName = runningGames.get(event.getChannel().getIdLong()).image.getName();
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT)
                        .send(text + imgName.substring(0, imgName.indexOf('.')), runningGames.get(event.getChannel().getIdLong()).getReveal(), "Guess the background.png");
                runningGames.get(event.getChannel().getIdLong()).dispose();
                runningGames.remove(event.getChannel().getIdLong());
                if (autostart) {
                    String[] newArgs = Arrays.copyOf(args, args.length + 1);
                    newArgs[0] = "s";
                    action(newArgs, event, true);
                }
                break;
            case "hint":
            case "h":
                if (!runningGames.containsKey(event.getChannel().getIdLong())) {
                    new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(runningGames.get(event.getChannel().getIdLong()).getHint());
                break;
            default:
                new BotMessage(event.getChannel(), BotMessage.MessageType.TEXT).send(help(2));
        }
    }

    private void checkChat(BackgroundGame bgGame) {
        int counter = 20;
        long newLast = bgGame.originEvent.getChannelType() == ChannelType.PRIVATE
                ? Main.jda.getPrivateChannelById(bgGame.originEvent.getChannel().getIdLong()).getLatestMessageIdLong()
                : bgGame.originEvent.getChannel().getLatestMessageIdLong();
        if (bgGame.lastMsgChecked != newLast) {
            for (Message msg : bgGame.originEvent.getChannel().getIterableHistory()) {
                if (--counter == 0 || msg.getIdLong() == bgGame.lastMsgChecked) break;
                if (msg.getAuthor() == Main.jda.getSelfUser()) continue;
                String content = msg.getContentRaw().toLowerCase();
                if (bgGame.title.equals(content)) {
                    action(new String[]{"r", msg.getAuthor().getName(), "1"}, bgGame.originEvent);
                    return;
                }
                if (bgGame.titleSplit.length > 0) {
                    String[] contentSplit = content.split(" ");
                    int hit = 0;
                    for (String c : contentSplit) {
                        for (String t : bgGame.titleSplit) {
                            if (c.equals(t)) {
                                if ((hit += c.length()) > 8) {
                                    action(new String[]{"r", msg.getAuthor().getName(), "0.9"}, bgGame.originEvent);
                                    return;
                                }
                            }
                        }
                    }
                }
                double similarity = utilGeneral.similarity(bgGame.title, content);
                if (similarity > 0.5) {
                    action(new String[]{"r", msg.getAuthor().getName(), "" + similarity}, bgGame.originEvent);
                    return;
                }
                if (!bgGame.artistGuessed) {
                    if (bgGame.artist.equals(content)) {
                        new BotMessage(bgGame.originEvent.getChannel(), BotMessage.MessageType.TEXT)
                                .send("That's the correct artist `" + msg.getAuthor().getName() + "`, can you get the title too?");
                        bgGame.artistGuessed = true;
                    } else if (similarity < 0.3 && utilGeneral.similarity(bgGame.artist, content) > 0.5) {
                        new BotMessage(bgGame.originEvent.getChannel(), BotMessage.MessageType.TEXT)
                                .send("`" + msg.getAuthor().getName() + "` got the artist almost correct, it's actually `" +
                                        bgGame.artist + "` but can you get the title?");
                        bgGame.artistGuessed = true;
                    }
                }
            }
            bgGame.lastMsgChecked = newLast;
        }
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

    private static String removeParanthesis(String str) {
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
        private File image;
        private BufferedImage origin;
        private int x;
        private int y;
        private int radius;
        private ScheduledFuture<?> timeLeft;
        private ScheduledFuture<?> chatChecker;
        private long lastMsgChecked;
        private MessageReceivedEvent originEvent;
        private String artist;
        private String title;
        private String[] titleSplit;
        private boolean artistGuessed;
        private int hintDepth;

        BackgroundGame(MessageReceivedEvent event, File image, BufferedImage origin, OsuBeatmapSet mapset, ScheduledExecutorService scheduler) {
            originEvent = event;
            this.image = image;
            this.origin = origin;
            artistGuessed = false;
            hintDepth = 0;
            lastMsgChecked = event.getMessage().getIdLong();
            radius = 100;
            x = ThreadLocalRandom.current().nextInt(radius, origin.getWidth() - radius);
            y = ThreadLocalRandom.current().nextInt(radius, origin.getHeight() - radius);
            artist = removeParanthesis(mapset.getArtist().toLowerCase());
            title = removeParanthesis(mapset.getTitle().toLowerCase());
            titleSplit = title.split(" ");
            timeLeft = scheduler.schedule(() -> action(new String[] {"r"}, event, false), 5, TimeUnit.MINUTES);
            chatChecker = scheduler.scheduleAtFixedRate(() -> checkChat(this), 0, 1500, TimeUnit.MILLISECONDS);
        }

        String getHint() {
            String hint;
            String[] titleCpy;
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
                case 2:
                    hint = "My last hint for you: The title looks like this `";
                    titleCpy = new String[titleSplit.length];
                    for (int i = 0; i < titleSplit.length; i++)
                        titleCpy[i] = StringUtils.repeat("▢", titleSplit[i].length());
                    return hint + String.join(" ", titleCpy) + "`";
                default:
                    hint = "All the hints I give you: The artist is `" + artist + "` and the title looks like `";
                    titleCpy = new String[titleSplit.length];
                    titleCpy[0] = title.substring(0, 1) + StringUtils.repeat("▢", titleSplit[0].length() - 1);
                    for (int i = 1; i < titleSplit.length; i++)
                        titleCpy[i] = StringUtils.repeat("▢", titleSplit[i].length());
                    return hint + String.join(" ", titleCpy) + "`";
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
            chatChecker.cancel(false);
        }
    }

}
