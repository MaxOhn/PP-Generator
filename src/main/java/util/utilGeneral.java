package main.java.util;

import main.java.core.DBProvider;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class utilGeneral {

    public static String secondsToTimeFormat(long secs) {
        return String.format("%02d:%02d", secs/60, secs%60);
    }

    public static String secondsToTimeFormat(int secs) {
        return String.format("%02d:%02d", secs/60, secs%60);
    }

    public static String howLongAgo(ZonedDateTime d) {
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
        return isDev(author.getUser());
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

    public static BufferedImage combineImages(List<String> urls) {
        try {
            int w = 400, h = 400;
            List<BufferedImage> imgs = new ArrayList<>();
            for (String url : urls) {
                BufferedImage img = ImageIO.read(new URL(url));
                imgs.add(img);
                w = Math.min(w, img.getWidth());
                h = Math.min(h, img.getHeight());
            }
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = combined.createGraphics();
            for (BufferedImage img : imgs) {
                g2.drawImage(img.getSubimage(
                        imgs.indexOf(img)*img.getWidth()/imgs.size(),
                        0,
                        img.getWidth()/imgs.size(),
                        img.getHeight()
                        ), imgs.indexOf(img)*w/imgs.size(), 0, w/imgs.size(), h, null);
            }
            g2.dispose();
            return combined;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public  static boolean isSubarray(Object[] smaller, Object[] larger) {
        int i = 0, j = 0;
        while (i < smaller.length && j < larger.length) {
            if (smaller[i] == larger[j]) {
                i++; j++;
                if (i == smaller.length) return true;
            } else {
                i = 0; j++;
            }
        }
        return false;
    }

    public static boolean isDev(User author) {
        return author.getId().equals(secrets.badewanne3ID);
    }
}
