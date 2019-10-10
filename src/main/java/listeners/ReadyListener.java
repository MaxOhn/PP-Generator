package main.java.listeners;

import com.oopsjpeg.osu4j.backend.Osu;
import main.java.core.*;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

public class ReadyListener extends ListenerAdapter {

    public void onReady(ReadyEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        logger.info("API is ready!");
        Main.osu = Osu.getAPI(secrets.osuAPIkey);
        Main.customOsu = new CustomOsu();
        if (secrets.RELEASE)
            Main.streamHook = new StreamHook();
        Main.discLink = new DiscordLink();
        Main.memberHandler = new MemberHandler();
        Main.reactionHandler = new ReactionHandler();
    }
}
