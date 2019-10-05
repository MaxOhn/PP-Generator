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
import java.util.*;
import java.util.List;

public class utilGeneral {

    public static String secondsToTimeFormat(long secs) {
        return String.format("%02d:%02d", secs/60, secs%60);
    }

    public static String secondsToTimeFormat(int secs) {
        return String.format("%02d:%02d", secs/60, secs%60);
    }

    public static String howLongAgo(ZonedDateTime d) {
        OffsetDateTime date = OffsetDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        long factor = 60;
        long diffSeconds = OffsetDateTime.now().toEpochSecond() - date.toEpochSecond();
        long diffMinutes = diffSeconds / factor;
        if (diffMinutes < 1)
            return diffSeconds + " second" + (diffSeconds == 1 ? "" : "s") + " ago";
        long diffHours = diffSeconds / (factor *= 60);
        if (diffHours < 1)
            return diffMinutes + " minute" + (diffMinutes == 1 ? "" : "s") + " ago";
        long diffDays = diffSeconds / (factor *= 24);
        if (diffDays < 1)
            return diffHours + " hour" + (diffHours == 1 ? "" : "s") + " ago";
        long diffWeeks = diffSeconds / (factor * 7);
        if (diffWeeks < 1)
            return diffDays + " day" + (diffDays == 1 ? "" : "s") + " ago";
        long diffMonths = diffSeconds / (long)(factor * 30.41666666);
        if (diffMonths < 1)
            return diffWeeks + " week" + (diffWeeks == 1 ? "" : "s") + " ago";
        long diffYears = diffSeconds / (factor * 365);
        if (diffYears < 1) {
            if (diffDays - (diffMonths * 30.41666666) > 20)
                diffMonths++;
            return diffMonths + " month" + (diffMonths == 1 ? "" : "s") + " ago";
        }
        if (diffMonths - (diffYears * 12) > 6)
            diffYears++;
        return diffYears + " year" + (diffYears == 1 ? "" : "s") + " ago";
    }

    public static boolean isAuthority(Member author, String serverID) throws SQLException, ClassNotFoundException {
        List<String> authorityRoles = secrets.WITH_DB
                ? Arrays.asList(DBProvider.getAuthorityRoles(serverID))
                : Arrays.asList(statics.authorities);
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
        if (smaller.length == 0) return true;
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

    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        return longerLength == 0 ? 1 : (longerLength - editDistance(longer, shorter)) / (double)longerLength;
    }

    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase(); s2 = s2.toLowerCase();
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue((a, b) -> b.compareTo(a)));
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());
        return result;
    }
}
