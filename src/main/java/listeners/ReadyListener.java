package main.java.listeners;

import com.oopsjpeg.osu4j.backend.Osu;
import main.java.core.*;
import main.java.util.secrets;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

public class ReadyListener extends ListenerAdapter {

    public void onReady(ReadyEvent event) {
        Main.osu = Osu.getAPI(secrets.osuAPIkey);
        Main.customOsu = new CustomOsu();
        Main.discLink = new DiscordLink();
        Main.memberHandler = new MemberHandler();
        Main.reactionHandler = new ReactionHandler();
        if (secrets.RELEASE) {
            while (true) {
                try {
                    Main.streamHook = new StreamHook();
                    break;
                } catch (Exception ignored) {
                } finally {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) { }
                }
            }
        }
        LoggerFactory.getLogger(this.getClass()).info("API is ready!");
    }
}
