package main.java.listeners;

import com.oopsjpeg.osu4j.backend.Osu;
import main.java.core.*;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class ReadyListener extends ListenerAdapter {

    public void onReady(ReadyEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        logger.info("API is ready!");
        Main.osu = Osu.getAPI(secrets.osuAPIkey);
        if (secrets.RELEASE) Main.twitch = new TwitchHook();
        Main.discLink = new DiscordLink();
        Main.fileInteractor = new FileInteractor();
        Main.runningLyrics = new HashSet<>();
    }
}
