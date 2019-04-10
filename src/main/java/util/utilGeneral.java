package main.java.util;

import main.java.core.DBProvider;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        if (temp > 1 || (temp == 1 && now.getMonthValue() > date.getMonthValue()))
            return temp + " year" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getMonthValue() - date.getMonthValue();
        if (temp < 0)
            temp += 12;
        if (temp > 1 || temp == 1 && now.getDayOfMonth() > date.getDayOfMonth())
            return temp + " month" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getDayOfMonth() - date.getDayOfMonth();
        if (temp < 0)
            temp += date.getMonth().length(false);
        if (temp > 1 || temp == 1 && now.getHour() > date.getHour())
            return temp + " day" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getHour() - date.getHour();
        if (temp < 0)
            temp += 24;
        if (temp > 1 || temp == 1 && now.getMinute() > date.getMinute())
            return temp + " hour" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getMinute() - date.getMinute();
        if (temp < 0)
            temp += 60;
        if (temp > 1 || temp == 1 && now.getSecond() > date.getSecond())
            return temp + " minute" + (temp == 1 ? "" : "s") + " ago";
        temp = now.getSecond() - date.getSecond();
        if (temp > 0)
            return temp + " second" + (temp == 1 ? "" : "s") + " ago";
        return "";
    }

    public static boolean isAuthority(Member author, String serverID) throws SQLException, ClassNotFoundException {
        List<String> authorityRoles = Arrays.asList(DBProvider.getAuthorityRoles(serverID));
        for(Role r : author.getRoles())
            if(r.hasPermission(Permission.ADMINISTRATOR) ||
                    (authorityRoles.contains(r.getName().toLowerCase())))
                return true;
        return false;
    }

    public enum Category {
        FUN("Fun"),
        OSU("osu!"),
        TWITCH("Twitch"),
        UTILITY("Utility");
        String name;
        Category(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    public static boolean isDev(User author) {
        return author.getId().equals(secrets.badewanne3ID);
    }
}
