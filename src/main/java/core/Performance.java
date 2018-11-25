package main.java.core;

import de.maxikg.osuapi.model.Beatmap;
import de.maxikg.osuapi.model.UserGame;
import main.java.util.secrets;
import main.java.util.utilOsu;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;


public class Performance {

    private PerfObj mapPerf = null;
    private PerfObj playPerf = null;
    private Beatmap map;
    private UserGame usergame = null;
    private int completion = 0;
    private Logger logger = Logger.getLogger(this.getClass());

    public Performance(Beatmap map) {
        this.map = map;
        calculateMapPP();
    }

    public Performance(Beatmap map, UserGame usergame) {
        this.map = map;
        this.usergame = usergame;
        calculateMapPP();
        calculatePlayPP();
        calculateCompletion();
    }

    private void calculateCompletion() {
        int hits = utilOsu.passedObjects(usergame);
        completion = (int)((double)hits*100/(double)mapPerf.getNobjects());
    }

    private void calculateMapPP() {
        if (!new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg").isFile())
            Main.fileInteractor.downloadMapThumb(map.getBeatmapSetId());
        if (!new File(secrets.mapPath + map.getBeatmapId() + ".osu").isFile())
            Main.fileInteractor.downloadMap(map.getBeatmapId());
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(secrets.execPrefix + "oppai " + secrets.mapPath + map.getBeatmapId() + ".osu "
                    + (usergame == null ? "" : ("+" + utilOsu.abbrvModSet(usergame.getEnabledMods())) + " ")
                    + "-ojson");
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line;
            line = input.readLine();
            mapPerf = new PerfObj(new JSONObject(line));
            while((line=errors.readLine()) != null) {
                logger.error(line);
            }
            pr.waitFor();
        } catch(Exception e) {
            logger.error("Something went wrong while calculating the pp of a map: " + e);
        }
    }

    private double calculateAcc() {
        double res = ((double)usergame.getCount50() * 50.0D + (double)usergame.getCount100() * 100.0D +
                (double)usergame.getCount300() * 300.0D) / ((double)(mapPerf.getNobjects()) * 300.0D);
        return 100*Math.max(0.0D, Math.min(res, 1.0D));
    }

    private void calculatePlayPP() {
        boolean failedPlay = usergame.getRank().equals("F");
        String mapPath = failedPlay ?
                secrets.mapPath + "temp" + map.getBeatmapId() + usergame.getUserId() + ".osu" :
                secrets.mapPath + map.getBeatmapId() + ".osu";
        try {
            if (failedPlay) {
                int lastNoteTiming = Main.fileInteractor.offsetOfNote(utilOsu.passedObjects(usergame), map.getBeatmapId());
                Main.fileInteractor.copyMapUntilOffset(mapPath, map.getBeatmapId(), lastNoteTiming);
            }
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(secrets.execPrefix + "oppai " + mapPath + " "
                    + "+" + utilOsu.abbrvModSet(usergame.getEnabledMods()) + " "
                    + calculateAcc() + "% "
                    + usergame.getCount50() + "x50 "
                    + usergame.getCount100() + "x100 "
                    + usergame.getCountMiss() + "m "
                    + usergame.getMaxCombo() + "x "
                    + "-ojson");
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line = input.readLine();
            playPerf = new PerfObj(new JSONObject(line));
            while((line=errors.readLine()) != null) {
                System.out.println(line);
            }
            input.close();
            errors.close();
            pr.waitFor();
            if (failedPlay)
                Main.fileInteractor.deleteFile(mapPath);
        } catch(Exception e) {
            logger.error("Something went wrong while calculating the pp of a play: " + e);
        }
    }

    public double getTotalMapPP() {
        return mapPerf == null ? 0 : mapPerf.getPp();
    }

    public double getTotalPlayPP() {
        return playPerf == null ? 0 : playPerf.getPp();
    }

    public int getObjectAmount() {
        return mapPerf == null ? 0 : mapPerf.getNobjects();
    }

    public double getAcc() {
        return usergame == null ? 0 : calculateAcc();
    }

    public int getCompletion() {
        return completion;
    }

    public static class PerfObj {
        private String artist;
        private String title;
        private String creator;
        private String version;
        private String modsStr;
        private int mods;
        private double od;
        private double ar;
        private double cs;
        private double hp;
        private int combo;
        private int maxCombo;
        private int ncircles;
        private int nsliders;
        private int nspinners;
        private int nmisses;
        private int scoreVersion;
        private double stars;
        private double speedStars;
        private double aimStars;
        private int nsingles;
        private int nsinglesThreshold;
        private double aimPP;
        private double speedPP;
        private double accPP;
        private double pp;

        PerfObj(JSONObject json) throws JSONException {
            this.artist = json.getString("artist");
            this.title = json.getString("title");
            this.creator = json.getString("creator");
            this.version = json.getString("version");
            this.modsStr = json.getString("mods_str");
            this.mods = json.getInt("mods");
            this.od = json.getDouble("od");
            this.ar = json.getDouble("ar");
            this.cs = json.getDouble("cs");
            this.hp = json.getDouble("hp");
            this.combo = json.getInt("combo");
            this.maxCombo = json.getInt("max_combo");
            this.ncircles = json.getInt("num_circles");
            this.nsliders = json.getInt("num_sliders");
            this.nspinners = json.getInt("num_spinners");
            this.nmisses = json.getInt("misses");
            this.scoreVersion = json.getInt("score_version");
            this.stars = json.getDouble("stars");
            this.speedStars = json.getDouble("speed_stars");
            this.aimStars = json.getDouble("aim_stars");
            this.nsingles = json.getInt("nsingles");
            this.nsinglesThreshold = json.getInt("nsingles_threshold");
            this.aimPP = json.getDouble("aim_pp");
            this.speedPP = json.getDouble("speed_pp");
            this.accPP = json.getDouble("acc_pp");
            this.pp = json.getDouble("pp");
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getCreator() {
            return creator;
        }

        public String getVersion() {
            return version;
        }

        public String getModsStr() {
            return modsStr;
        }

        public int getMods() {
            return mods;
        }

        public double getOd() {
            return od;
        }

        public double getAr() {
            return ar;
        }

        public double getCs() {
            return cs;
        }

        public double getHp() {
            return hp;
        }

        public int getCombo() {
            return combo;
        }

        public int getMaxCombo() {
            return maxCombo;
        }

        public int getNcircles() {
            return ncircles;
        }

        public int getNsliders() {
            return nsliders;
        }

        public int getNspinners() {
            return nspinners;
        }

        public int getNmisses() {
            return nmisses;
        }

        public int getScoreVersion() {
            return scoreVersion;
        }

        public double getStars() {
            return stars;
        }

        public double getSpeedStars() {
            return speedStars;
        }

        public double getAimStars() {
            return aimStars;
        }

        public int getNsingles() {
            return nsingles;
        }

        public int getNsinglesThreshold() {
            return nsinglesThreshold;
        }

        public double getAimPP() {
            return aimPP;
        }

        public double getSpeedPP() {
            return speedPP;
        }

        public double getAccPP() {
            return accPP;
        }

        double getPp() {
            return pp;
        }

        int getNobjects() {
            return ncircles + nsliders + nspinners;
        }
    }
}
