package main.java.core;

import de.maxikg.osuapi.model.Beatmap;
import de.maxikg.osuapi.model.Mod;
import de.maxikg.osuapi.model.UserGame;
import main.java.util.secrets;
import main.java.util.utilOsu;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;

import static de.maxikg.osuapi.model.Mod.createSum;


public class Performance {

    private PerfObj mapPerf = null;
    private PerfObj playPerf = null;
    private Beatmap map;
    private UserGame usergame = null;
    private Logger logger = Logger.getLogger(this.getClass());
    private ManiaPerformanceCalculator maniaPerf = null;
    private int mode;

    public Performance(Beatmap map) {
        this.mode = 0;
        this.map = map;
        prepareFiles();
        calculateMapPP();
    }

    public Performance(Beatmap map, UserGame usergame) {
        this(map, usergame, 0);
    }

    public Performance(Beatmap map, UserGame usergame, int mode) {
        this.mode = mode;
        this.map = map;
        this.usergame = usergame;
        prepareFiles();
        if (mode == 0) {
            calculateMapPP();
            calculatePlayPP();
        } else if (mode == 3) {
            double stars = calculateManiaStars();
            maniaPerf = new ManiaPerformanceCalculator(createSum(usergame.getEnabledMods()),
                    Main.fileInteractor.countTotalObjects(map.getBeatmapId()), (float)map.getDifficultyOverall(),
                    usergame.getScore(), stars);
            if (usergame.getRank().equals("F"))
                maniaPerf.setPlayPP(0);
        }
    }

    private int calculateCompletion() {
        int hits = utilOsu.passedObjects(usergame, mode);
        if (mode == 0)
            return (int)((double)hits*100/(double)mapPerf.getNobjects());
        else // (mode == 3)
            return (int)((double)hits*100/(double)maniaPerf.getNoteCount());
    }

    private void prepareFiles() {
        if (!new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg").isFile())
            Main.fileInteractor.downloadMapThumb(map.getBeatmapSetId());
        if (!new File(secrets.mapPath + map.getBeatmapId() + ".osu").isFile())
            Main.fileInteractor.downloadMap(map.getBeatmapId());
    }

    // TODO
    private double calculateManiaStars() {
        return map.getDifficultyRating();
    }

    private void calculateMapPP() {
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
            while ((line = errors.readLine()) != null)
                logger.error(line);
            pr.waitFor();
        } catch (Exception e) {
            logger.error("Something went wrong while calculating the pp of a map: " + e);
        }
    }

    private double calculateAcc() {
        double numerator = (double)usergame.getCount50() * 50.0D + (double)usergame.getCount100() * 100.0D +
                (double)usergame.getCount300() * 300.0D;
        if (mode == 3)
            numerator += (double)usergame.getCountKatu() * 200.0D + (double)usergame.getCountGeki() * 300.0D;
        double denominator;
        if (mode == 0)
            denominator = (double)(mapPerf.getNobjects()) * 300.0D;
        else // (mode == 3)
            denominator = maniaPerf.getNoteCount();
        double res = numerator / denominator;
        return 100*Math.max(0.0D, Math.min(res, 1.0D));
    }

    private void calculatePlayPP() {
        boolean failedPlay = usergame.getRank().equals("F");
        String mapPath = failedPlay ?
                secrets.mapPath + "temp" + map.getBeatmapId() + usergame.getUserId() + ".osu" :
                secrets.mapPath + map.getBeatmapId() + ".osu";
        try {
            if (failedPlay) {
                int lastNoteTiming = Main.fileInteractor.offsetOfNote(utilOsu.passedObjects(usergame, mode), map.getBeatmapId());
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
        if (mode == 0)
            return mapPerf == null ? 0 : mapPerf.getPp();
        else //(mode == 3)
            return maniaPerf.getMapPP();
    }

    public double getTotalPlayPP() {
        if (mode == 0)
            return playPerf == null ? 0 : playPerf.getPp();
        else // (mode == 3)
            return maniaPerf.getPlayPP();
    }

    public int getObjectAmount() {
        if (mode == 0)
            return mapPerf == null ? 0 : mapPerf.getNobjects();
        else // (mode == 3)
            return maniaPerf.noteCount;
    }

    public double getAcc() {
        return usergame == null ? 0 : calculateAcc();
    }

    public int getCompletion() {
        return calculateCompletion();
    }

    private static class PerfObj {
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

    /*
     * ---------------
     *      Mania
     * ---------------
     */

    // thanks yentis <3
    private class ManiaPerformanceCalculator {
        private int score = 0;
        private double realScore = 0;
        private double scoreMultiplier = 1;
        private double stars = 0;
        private float overallDifficulty = 0;
        private int noteCount = 0;
        private ArrayList<Double> ppValues = new ArrayList<>();
        private int mods = 0;
        private double MAX_SCORE = 1000000;

        public int getNoteCount() {
            return noteCount;
        }

        double getPlayPP() {
            return ppValues.get(0);
        }

        public void setPlayPP(double v) {
            ppValues.set(0, v);
        }

        double getMapPP() {
            return ppValues.get(1);
        }

        public double getStars() {
            return stars;
        }

        ManiaPerformanceCalculator(int mods, int noteCount, float overallDifficulty, int score, double stars) {
            this.mods = mods;
            this.noteCount = noteCount;
            this.overallDifficulty = overallDifficulty;
            this.score = score;
            this.stars = stars;
            if ((mods & Mod.NO_FAIL.getFlag()) > 0)
                scoreMultiplier *= 0.5;
            if((mods & Mod.EASY.getFlag()) > 0)
                scoreMultiplier *= 0.5;
            if((mods & Mod.HALF_TIME.getFlag()) > 0)
                scoreMultiplier *= 0.5;
            ComputePPValues();
        }

        private void ComputePPValues() {
            double multiplier = ComputeMultiplier();
            double strainValue = ComputeStrainValue(score);
            double accValue = ComputeAccValue(strainValue);
            ppValues.add(ComputeTotalPP(strainValue, accValue, multiplier));
            strainValue = ComputeStrainValue(MAX_SCORE * scoreMultiplier);
            accValue = ComputeAccValue(strainValue);
            ppValues.add(ComputeTotalPP(strainValue, accValue, multiplier));
        }

        private double ComputeMultiplier() {
            double multiplier = 0.8;
            if ((mods & Mod.NO_FAIL.getFlag()) > 0)
                multiplier *= 0.90;
            if ((mods & Mod.SPUN_OUT.getFlag()) > 0)
                multiplier *= 0.95;
            if ((mods & Mod.EASY.getFlag()) > 0)
                multiplier *= 0.5;
            return multiplier;
        }

        private double ComputeStrainValue(double score) {
            if(scoreMultiplier <= 0)
                return 0;
            realScore = score * (1 / scoreMultiplier);
            double strainValue = Math.pow(5 * Math.max(1, stars / 0.2) - 4, 2.2) / 135;
            strainValue *= 1 + 0.1 * Math.min(1, noteCount / 1500);
            if (realScore <= 500000)
                strainValue = 0;
            else if (realScore <= 600000)
                strainValue *= 0.3 * ((realScore - 500000.0) / 100000);
            else if (realScore <= 700000)
                strainValue *= 0.3 + 0.25 * ((realScore - 600000.0) / 100000);
            else if (realScore <= 800000)
                strainValue *= 0.55 + 0.2 * ((realScore - 700000.0) / 100000);
            else if (realScore <= 900000)
                strainValue *= 0.75 + 0.15 * ((realScore - 800000.0) / 100000);
            else
                strainValue *= 0.9 + 0.1 * ((realScore - 900000.0) / 100000);
            return strainValue;
        }

        private double ComputeAccValue(double strain) {
            double hitWindow300 = 34 + 3 * Math.min(10, Math.max(0, 10 - overallDifficulty));
            if(hitWindow300 <= 0)
                return 0;
            return Math.max(0, 0.2 - ((hitWindow300 - 34) * 0.006667)) * strain * Math.pow(Math.max(0, realScore - 960000) / 40000, 1.1);
        }

        private double ComputeTotalPP(double strainValue, double accValue, double multiplier) {
            return Math.pow(Math.pow(strainValue, 1.1) + Math.pow(accValue, 1.1), 1 / 1.1) * multiplier;
        }
    }
}
