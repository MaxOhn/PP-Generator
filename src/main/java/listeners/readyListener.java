package main.java.listeners;

import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.backend.RateLimiter;
import main.java.core.DiscordLink;
import main.java.core.FileInteractor;
import main.java.core.Main;
import main.java.core.TwitchHook;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import java.util.HashSet;

public class readyListener extends ListenerAdapter {

    public void onReady(ReadyEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        logger.info("API is ready!");
        Main.osuRateLimiter = new RateLimiter(Main.OSU_REQUESTS_PER_MINUTE);
        Main.osu = Osu.getAPI(secrets.osuAPIkey);
        Main.osu.setRateLimiter(Main.osuRateLimiter);
        if (secrets.RELEASE) Main.twitch = new TwitchHook();
        Main.discLink = new DiscordLink();
        Main.fileInteractor = new FileInteractor();
        Main.runningLyrics = new HashSet<>();
    }
}
