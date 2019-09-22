package main.java.commands.Twitch;

import main.java.commands.ICommand;
import main.java.core.BotMessage;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Map;
import java.util.stream.Collectors;

public class cmdAllStreamers implements ICommand {
    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            event.getChannel().sendMessage("This command is not usable in private chat").queue();
            return false;
        }
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getTextChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        Map<String, Game> streamMap = event.getGuild().getMembers().stream()
                .filter(m -> m.getGame() != null && m.getGame().getType() == Game.GameType.STREAMING)
                .collect(Collectors.toMap(Member::getNickname, Member::getGame));
        StringBuilder msg = new StringBuilder();
        if (streamMap.size() > 0) {
            msg.append("__Current streamers on this server:__");
            for (String name : streamMap.keySet())
                msg.append("- `").append(name).append("`: `")
                        .append(streamMap.get(name).getName()).append("` on `")
                        .append(streamMap.get(name).getUrl()).append("`\n");
        } else msg.append("No current streamers on this server");
        new BotMessage(event, BotMessage.MessageType.TEXT).send(msg.toString());
    }

    @Override
    public String help(int hCode) {
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "allstreamers` to make me list all members of this server who are currently streaming";
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.TWITCH;
    }
}
