package main.java.commands.Fun;

import com.oopsjpeg.osu4j.OsuBeatmapSet;
import com.oopsjpeg.osu4j.backend.EndpointBeatmapSet;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class cmdBackgroundGame implements ICommand {

    private Logger logger = Logger.getLogger(cmdBackgroundGame.class);
    private File image = null;
    private BufferedImage origin = null;
    private int rX;
    private int rY;
    private int radius;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> timeLeft = null;
    private ScheduledFuture<?> chatChecker = null;
    private String lastMsgChecked;
    private MessageReceivedEvent originEvent;
    private String artist = "";
    private String title = "";
    private Boolean artistGuessed;

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(0));
            return false;
        }
        if (args.length != 1) {
            new BotMessage(event, BotMessage.MessageType.TEXT).send(help(2));
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        switch (args[0]) {
            case "Start":
            case "start":
            case "s":
                File[] files = new File(statics.bgGamePath).listFiles();
                assert files != null;
                while (true) {
                    image = files[ThreadLocalRandom.current().nextInt(files.length)];
                    try {
                        origin = ImageIO.read(image);
                        break;
                    } catch (IOException ignored) {
                        logger.warn("Error while selecting file: " + image.getName());
                    }
                }
                artistGuessed = false;
                originEvent = event;
                lastMsgChecked = event.getMessage().getId();
                radius = 100;
                rX = ThreadLocalRandom.current().nextInt(radius, origin.getWidth() - radius);
                rY = ThreadLocalRandom.current().nextInt(radius, origin.getHeight() - radius);
                try {
                    ImageIO.write(origin.getSubimage(rX - radius, rY - radius, 2 * radius, 2 * radius), "png", result);
                } catch (IOException e) {
                    logger.error("Error while writing result", e);
                }
                OsuBeatmapSet mapset;
                try {
                    mapset = Main.osu.beatmapSets.query(new EndpointBeatmapSet
                            .Arguments(Integer.parseInt(image.getName().substring(0, image.getName().indexOf('.')))));
                } catch (OsuAPIException e) {
                    logger.error("Error while retrieving the mapset", e);
                    return;
                }
                artist = mapset.getArtist().toLowerCase();
                title = mapset.getTitle().toLowerCase();
                event.getTextChannel().sendFile(result.toByteArray(), "Guess the background.png").queue(msg -> {
                    if (timeLeft != null)
                        timeLeft.cancel(false);
                    timeLeft = scheduler.schedule(() -> action(new String[] {"r"}, event), 5, TimeUnit.MINUTES);
                    if (chatChecker == null)
                        chatChecker = scheduler.scheduleAtFixedRate(this::checkChat, 0, 2, TimeUnit.SECONDS);
                });
                break;
            case "Bigger":
            case "bigger":
            case "b":
                if (origin == null) {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                radius += 75;
                int x = rX - radius;
                int y = rY - radius;
                int xEnd = Math.min(origin.getWidth(), x < 0 ? rX + radius - x : rX + radius);
                int yEnd = Math.min(origin.getHeight(), y < 0 ? rY + radius - y : rY + radius);
                if (x < 0) x = 0;
                if (y < 0) y = 0;
                try {
                    ImageIO.write(origin.getSubimage(x, y, xEnd - x, yEnd - y), "png", result);
                } catch (IOException e) {
                    logger.error("Error while writing result", e);
                }
                event.getTextChannel().sendFile(result.toByteArray(), "Guess the background.png").queue();
                break;
            case "Resolve":
            case "resolve":
            case "Solve":
            case "solve":
            case "r":
                if (origin == null) {
                    new BotMessage(event, BotMessage.MessageType.TEXT).send(help(1));
                    return;
                }
                try {
                    ImageIO.write(origin, "png", result);
                } catch (IOException e) {
                    logger.error("Error while writing result", e);
                }
                timeLeft.cancel(false);
                chatChecker.cancel(false);
                chatChecker = null;
                String text = "Resolving background: https://osu.ppy.sh/beatmapsets/";
                if (args.length > 1) {
                    String name = args[1];
                    double similarity = Double.parseDouble(args[2]);
                        text = similarity == 1
                                ? "Gratz `" + name + "`, you guessed it :)\nMapset: https://osu.ppy.sh/beatmapsets/"
                                : "You we're close enough `" + name + "`, gratz :)\nMapset: https://osu.ppy.sh/beatmapsets/";
                }
                event.getTextChannel().sendMessage(text)
                        .append(image.getName().substring(0, image.getName().indexOf('.')))
                        .addFile(result.toByteArray(), "Guess the background.png")
                        .queue();
                image = null;
                origin = null;
                radius = 100;
                originEvent = null;
                lastMsgChecked = "";
                artist = "";
                title = "";
                break;
            default:
                new BotMessage(event, BotMessage.MessageType.TEXT).send(help(2));
        }
    }

    private void checkChat() {
        int counter = 50;
        String newLast = originEvent.getTextChannel().getLatestMessageId();
        for (Message msg : originEvent.getTextChannel().getIterableHistory()) {
            if (--counter == 0 || msg.getId().equals(lastMsgChecked)) break;
            String content = msg.getContentRaw().toLowerCase();
            if (title.equals(content)) {
                action(new String[] { "r", msg.getAuthor().getName(), "1"}, originEvent);
                return;
            }
            double similarity = utilGeneral.similarity(title, content);
            if (similarity > 0.5) {
                action(new String[] { "r", msg.getAuthor().getName(), "" + similarity }, originEvent);
                return;
            }
            if (!artistGuessed) {
                if (artist.equals(content)) {
                    new BotMessage(originEvent, BotMessage.MessageType.TEXT)
                            .send("That's the correct artist `" + msg.getAuthor().getName() + "`, can you get the title too?");
                    artistGuessed = true;
                } else if (similarity < 0.3 && utilGeneral.similarity(artist, content) > 0.5) {
                    new BotMessage(originEvent, BotMessage.MessageType.TEXT)
                            .send("`" + msg.getAuthor().getName() + "` got the artist almost correct, it's actually `" +
                                    artist + "` but can you get the title?");
                    artistGuessed = true;
                }
            }
        }
        lastMsgChecked = newLast;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "background -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "background <start/bigger/resolve>` to play the background-guessing game." +
                        "\nWith `start` I will select and show part of a new background for you to guess." +
                        "\nWith `bigger` I will slightly enlargen the currently shown part of the background to make it easier." +
                        "\nWith `resolve` I will show you the entire background and its mapset";
            case 1:
                return "You must first start a new round via `" + statics.prefix + "background start`" + help;
            case 2:
                return "This command requires exactly one argument which must either be `start`, `bigger`, or `resolve`" + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.FUN;
    }
}
