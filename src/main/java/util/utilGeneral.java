package main.java.util;

import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

public class utilGeneral {

    public static String secondsToTimeFormat(long secs) {
        return String.format("%02d:%02d", secs/60, secs%60);
    }

    public static String secondsToTimeFormat(int secs) {
        return String.format("%02d:%02d", secs/60, secs%60);
    }

    public static String howLongAgo(Date d) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime date = OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        int temp = now.getYear() - date.getYear();
        if (temp > 0)
            return temp + " year" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getMonthValue() - date.getMonthValue();
        if (temp > 0)
            return temp + " month" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getDayOfMonth() - date.getDayOfMonth();
        if (temp > 0)
            return temp + " day" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getHour() - date.getHour();
        if (temp > 0)
            return temp + " hour" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getMinute() - date.getMinute();
        if (temp > 0)
            return temp + " minute" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getSecond() - date.getSecond();
        if (temp > 0)
            return temp + " second" + (temp == 1 ? "" : "s") + " ago";
        return "";
    }

    public static boolean isAuthority(MessageReceivedEvent event) {
        for(Role r : event.getGuild().getMember(event.getAuthor()).getRoles())
            if(Arrays.asList(statics.authorities).contains(r.getName().toLowerCase()))
                return true;
        return false;
    }

    public static boolean isDev(MessageReceivedEvent event) {
        return event.getAuthor().getId().equals(secrets.badewanne3ID);
    }
}
