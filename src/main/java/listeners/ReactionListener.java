package main.java.listeners;

import main.java.core.Main;
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ReactionListener extends ListenerAdapter {

    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        Main.reactionHandler.addedReaction(event, getHash(event));
    }

    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        Main.reactionHandler.removedReaction(event, getHash(event));
    }

    private int getHash(GenericGuildMessageReactionEvent event) {
        return (event.getGuild().getId() + event.getChannel().getId() + event.getMessageId()).hashCode();
    }
}
