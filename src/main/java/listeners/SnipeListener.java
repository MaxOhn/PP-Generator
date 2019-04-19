package main.java.listeners;

import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.core.DBProvider;
import main.java.core.Main;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SnipeListener {

    private Logger logger = Logger.getLogger(this.getClass());
    private Map<TextChannel, Message> channels;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    {
        new Thread(() -> {
            try {
                while (Main.jda == null) {
                    try { Thread.sleep(500); }
                    catch (InterruptedException ignored) {}
                }
                channels = DBProvider.getSnipeChannels().stream()
                        .collect(HashMap::new, (m, v) -> m.put(Main.jda.getTextChannelById(v), null), HashMap::putAll);
            } catch (ClassNotFoundException | SQLException e) {
                logger.error("Could not retrieve snipe channels from database:");
                e.printStackTrace();
                channels = new HashMap<>();
            }
        }).start();
    }

    /*
        -1: currently not rebuilding
        -2: channel not added
    */
    public String getMessageId(TextChannel channel) {
        if (channels.keySet().contains(channel)) {
            Message msg = channels.get(channel);
            if (msg != null) return msg.getId();
            return "-1";
        }
        return "-2";
    }

    public boolean addChannel(TextChannel channel) {
        try {
            if (!channel.canTalk(channel.getGuild().getSelfMember())) return false;
            DBProvider.addSnipeChannel(channel.getId());
            channels.put(channel, null);
            if (Main.snipeManager.getIsUpdatingRankings()) {
                channel.sendMessage("Building in progress...")
                        .queue(message -> channels.put(channel, message));
            }
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void onSnipe(String mapID, String sniperID, String snipeeID) {
        String snipeeName = "User id " + snipeeID;
        String sniperName = "user id " + sniperID;
        String mapString = "https://osu.ppy.sh/b/" + mapID;
        try {
            sniperName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(sniperID)).build()).getUsername();
            snipeeName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(snipeeID)).build()).getUsername();
        } catch (Exception ignored) {}
        for (TextChannel channel : channels.keySet()) {
            if (!channel.canTalk(channel.getGuild().getSelfMember())) continue;
            channel.sendMessage(snipeeName + " was sniped by " + sniperName
                    + "\n" + mapString).queue();
        }
    }

    public void onClaim(String mapID, String claimerID) {
        String claimerName = "user id " + claimerID;
        String mapString = "https://osu.ppy.sh/b/" + mapID;
        try {
            claimerName = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(Integer.parseInt(claimerID)).build()).getUsername();
        } catch (Exception ignored) {}
        for (TextChannel channel : channels.keySet()) {
            if (!channel.canTalk(channel.getGuild().getSelfMember())) continue;
            channel.sendMessage("New first place is " + claimerName + "\n" + mapString).queue();
        }
    }

    public void onStartUpdateRanking() {
        for (TextChannel channel : channels.keySet()) {
            if (channels.get(channel) != null) channels.get(channel).delete().queue();
            if (!channel.canTalk(channel.getGuild().getSelfMember())) continue;
            channel.sendMessage("Initiate rebuild...")
                    .queue(message -> channels.put(channel, message));
        }
    }

    public void onUpdateRankingProgress(int currentIdx, int totalAmount, int amountFailed) {
        String editedMessage = "Building: " + df.format(100 * (double)currentIdx/totalAmount) + "% ("
                + currentIdx + " of " + totalAmount + ") | " + amountFailed + " failed";
        if (ThreadLocalRandom.current().nextBoolean()) {
            for (Message message : channels.values()) {
                if (message == null) continue;
                message.editMessage(editedMessage).queue();
            }
        }
    }

    public void onUpdateRankingStop(int currentIdx) {
        for (TextChannel channel : channels.keySet()) {
            if (channels.get(channel) != null) channels.get(channel).delete().queue();
            if (!channel.canTalk(channel.getGuild().getSelfMember())) continue;
            channel.sendMessage("Rebuilding stopped at id " + currentIdx)
                    .queue(message -> channels.put(channel, message));
        }
    }

    public void onUpdateRankingDone(int amountFailed) {
        for (TextChannel channel : channels.keySet()) {
            if (channels.get(channel) != null) channels.get(channel).delete().queue();
            if (!channel.canTalk(channel.getGuild().getSelfMember())) continue;
            channel.sendMessage("Rebuilding finished: " + amountFailed + " failed").queue();
        }
    }

}
