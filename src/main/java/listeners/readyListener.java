package main.java.listeners;

import de.maxikg.osuapi.client.DefaultOsuClient;
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
        Main.osu = new DefaultOsuClient(secrets.osuAPIkey);
        Main.twitch = new TwitchHook();
        Main.discLink = new DiscordLink();
        Main.fileInteractor = new FileInteractor();
        Main.runningLyrics = new HashSet<>();
    }
}
