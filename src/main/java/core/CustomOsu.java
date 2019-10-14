package main.java.core;

import com.google.common.util.concurrent.RateLimiter;
import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import main.java.util.secrets;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_intToStr;
import static main.java.util.utilOsu.mods_strToInt;

/*
    Providing some functionality that the regular osu client does not provide
 */
public class CustomOsu {

    private HttpClient client;
    private RateLimiter limiter;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public CustomOsu() {
        this.limiter = RateLimiter.create(0.60);
        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("osu_session", secrets.osu_session);
        cookie.setDomain("osu.ppy.sh");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
        client = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    // Return all osu usernames in the top 50 pp leaderboard of the given mode and country
    public List<String> getRankings(GameMode mode, String countryShort) throws IOException {
        limiter.acquire();
        // Prepare url and request data
        String modeStr;
        switch (mode) {
            case TAIKO: modeStr = "taiko"; break;
            case MANIA: modeStr = "mania"; break;
            case CATCH_THE_BEAT: modeStr = "fruits"; break;
            default: modeStr = "osu"; break;
        }
        HttpGet getRequest = new HttpGet("http://osu.ppy.sh/rankings/" + modeStr + "/performance?country=" + countryShort);
        HttpResponse response = client.execute(getRequest);
        // Parse response
        String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
        Document doc = Jsoup.parse(responseStr);
        try {
            return doc.select(".ranking-page-table").first()
                    .getElementsByTag("tbody").first().children().stream()
                    .map(e -> e.child(1).child(0).child(1).textNodes().get(0).text())
                    .map(e -> e.substring(1, e.length() - 1))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Error while scraping rankings page, is osu_session still up to date?");
            throw e;
        }
    }

    // Return all scores on the given map with the given mods, on either national or global leaderboard
    public Collection<OsuScore> getScores(String mapID, boolean national, Set<GameMod> mods) throws IOException {
        limiter.acquire();
        // Prepare url and request data
        StringBuilder url = new StringBuilder("http://osu.ppy.sh/beatmaps/" + mapID + "/scores?");
        if (national)
            url.append("type=country");
        if (mods != null) {
            if (mods.isEmpty())
                url.append("&mods[]=NM");
            else
                for (GameMod mod : mods)
                    url.append("&mods[]=").append(mods_intToStr((int) mod.getBit()));
        }
        HttpGet getRequest = new HttpGet(url.toString());
        HttpResponse response = client.execute(getRequest);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("No valid response from server:\n" + response.getEntity().toString());
        }
        // Parse response
        String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
        Collection<OsuScore> scores = new ArrayList<>();
        JSONArray rawScores = new JSONObject(responseStr).getJSONArray("scores");
        for (int i = 0; i < rawScores.length(); i++) {
            JSONObject o = (JSONObject)rawScores.get(i);
            JSONObject m = o.getJSONObject("beatmap");
            JSONObject stats = o.getJSONObject("statistics");
            OsuScore s = new OsuScore(Main.osu);

            s.setBeatmapID(m.getInt("id"));
            s.setScore(o.getInt("score"));
            s.setMaxcombo(o.getInt("max_combo"));
            s.setCount300(stats.getInt("count_300"));
            s.setCount100(stats.getInt("count_100"));
            s.setCount50(stats.getInt("count_50"));
            s.setCountmiss(stats.getInt("count_miss"));
            s.setCountgeki(stats.getInt("count_geki"));
            s.setCountkatu(stats.getInt("count_katu"));
            s.setPerfect(o.getBoolean("perfect"));
            s.setUserID(o.getInt("user_id"));
            s.setUsername(o.getJSONObject("user").getString("username"));
            s.setDate(ZonedDateTime.parse(o.getString("created_at")));
            s.setRank(o.getString("rank"));
            s.setPp(o.isNull("pp") ? 0 : (float)o.getDouble("pp"));
            s.setEnabledMods(GameMod.get(mods_strToInt(o.getJSONArray("mods").join("")
                    .replace("\"", ""))));
            s.setUser(Main.osu.users.getAsQuery(new EndpointUsers.ArgumentsBuilder(s.getUserID()).build()).asLazilyLoaded());
            s.setBeatmap(Main.osu.beatmaps.getAsQuery(new EndpointBeatmaps.ArgumentsBuilder()
                    .setBeatmapID(s.getBeatmapID()).build()).asLazilyLoaded().map(list -> list.get(0)));

            scores.add(s);
        }
        return scores;
    }

    // Retrieve the pp of the user with the given global rank (works only if rank is <=10000)
    public double getPpOfRank(int rank, GameMode mode) throws IOException {
        if (rank < 1 || rank > 10000)
            throw new IllegalArgumentException("Rank must be between 1 and 10000");
        limiter.acquire();
        // Prepare url and request data
        String modeString = "";
        switch (mode) {
            case STANDARD: modeString = "osu"; break;
            case MANIA: modeString = "mania"; break;
            case TAIKO: modeString = "taiko"; break;
            case CATCH_THE_BEAT: modeString = "fruits"; break;
        }
        String url = "https://osu.ppy.sh/rankings/" + modeString + "/performance?page=" + (1 - (rank % 50 == 0 ? 1 : 0) + rank / 50) + "#scores";
        HttpGet getRequest = new HttpGet(url);
        HttpResponse response = client.execute(getRequest);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("No valid response from server:\n" + response.getEntity().toString());
        }
        // Parse response
        String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
        Document doc = Jsoup.parse(responseStr);
        try {
            Optional<Element> elem = doc.select(".ranking-page-table").first()
                    .getElementsByTag("tbody").first().children().stream()
                    .skip(rank % 50 == 0 ? 49 : (rank % 50) - 1).findFirst();
            if (elem.isPresent()) {
                return Double.parseDouble(elem.get().child(4).text().replace(",", ""));
            } else
                throw new IllegalStateException("Could not find row " + ((rank % 50) - 1) + " of response html");
        } catch (Exception e) {
            logger.warn("Error while scraping rankings page to get pp for rank");
            throw e;
        }
    }
}
