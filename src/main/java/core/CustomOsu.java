package main.java.core;

import com.google.common.util.concurrent.RateLimiter;
import com.oopsjpeg.osu4j.GameMod;
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
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static main.java.util.utilOsu.mods_flag;

public class CustomOsu {

    private HttpClient client;
    private RateLimiter limiter;

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

    public List<String> getRankings() throws IOException {
        return getRankings("");
    }

    public List<String> getRankings(String countryShort) throws IOException {
        limiter.acquire();
        HttpGet getRequest = new HttpGet("http://osu.ppy.sh/rankings/osu/performance?country=" + countryShort);
        HttpResponse response = client.execute(getRequest);
        String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
        Document doc = Jsoup.parse(responseStr);
        return doc.select(".ranking-page-table").first()
                .getElementsByTag("tbody").first().children().stream()
                .map(e -> e.child(1).child(0).child(1).child(0).text())
                .collect(Collectors.toList());
    }

    public Collection<OsuScore> getScores(String mapID) throws IOException {
        return getScores(mapID, true);
    }

    public Collection<OsuScore> getScores(String mapID, boolean national) throws IOException {
        limiter.acquire();
        HttpGet getRequest = new HttpGet("http://osu.ppy.sh/beatmaps/" + mapID + "/scores" + (national ? "?type=country" : ""));
        HttpResponse response = client.execute(getRequest);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("No valid response from server:\n" + response.getEntity().toString());
        }
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
            s.setEnabledMods(GameMod.get(mods_flag(o.getJSONArray("mods").join("")
                    .replace("\"", ""))));
            s.setUser(Main.osu.users.getAsQuery(new EndpointUsers.ArgumentsBuilder(s.getUserID()).build()).asLazilyLoaded());
            s.setBeatmap(Main.osu.beatmaps.getAsQuery(new EndpointBeatmaps.ArgumentsBuilder()
                    .setBeatmapID(s.getBeatmapID()).build()).asLazilyLoaded().map(list -> list.get(0)));

            scores.add(s);
        }
        return scores;
    }
}
