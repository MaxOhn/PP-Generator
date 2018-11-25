package main.java.listeners;

import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.log4j.Logger;


public class reconnectListener extends ListenerAdapter {

    public void onReconnect(ReconnectedEvent event) {
        Logger logger = Logger.getLogger(this.getClass());
        logger.info("Reconnect event occurred.");
    }
}
