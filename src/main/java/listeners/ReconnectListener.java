package main.java.listeners;

import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;


public class ReconnectListener extends ListenerAdapter {

    public void onReconnect(ReconnectedEvent event) {
        LoggerFactory.getLogger(this.getClass()).info("Reconnect event occurred.");
    }
}
