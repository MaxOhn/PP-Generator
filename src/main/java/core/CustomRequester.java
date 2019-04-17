package main.java.core;

import com.oopsjpeg.osu4j.backend.RateLimiter;
import main.java.util.secrets;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CustomRequester {

    private HttpClient client;
    private RateLimiter limiter;

    public CustomRequester() {
        this.limiter = Main.osuRateLimiter;
        //this.limiter = new RateLimiter(120);
        CookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("osu_session", secrets.osu_session);
        cookie.setDomain("osu.ppy.sh");
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
        client = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    public void close() {
        limiter.close();
    }

    public JSONArray getScores(String mapID) throws IOException {
        limiter.getOrWaitForTicket();
        HttpGet getRequst = new HttpGet("http://osu.ppy.sh/beatmaps/" + mapID + "/scores?type=country");
        HttpResponse response = client.execute(getRequst);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder responseStr = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            responseStr.append(line);
            responseStr.append('\r');
        }
        rd.close();
        return new JSONObject(responseStr.toString()).getJSONArray("scores");
    }
}
