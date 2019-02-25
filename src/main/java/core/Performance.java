package main.java.core;

import de.maxikg.osuapi.model.Beatmap;
import de.maxikg.osuapi.model.BeatmapScore;
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
import static main.java.util.utilOsu.mods_str;


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

    public Performance(Beatmap map, BeatmapScore score) {
        this.mode = 3;
        this.map = map;
        maniaPerf = new ManiaPerformanceCalculator(createSum(score.getEnabledMods()),
                Main.fileInteractor.countTotalObjects(map.getBeatmapId()), (float)map.getDifficultyOverall(),
                score.getScore(), calculateManiaStars());
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
            maniaPerf = new ManiaPerformanceCalculator(createSum(usergame.getEnabledMods()),
                    Main.fileInteractor.countTotalObjects(map.getBeatmapId()), (float)map.getDifficultyOverall(),
                    usergame.getScore(), calculateManiaStars());
            if (usergame.getRank().equals("F"))
                maniaPerf.setPlayPP(0);
        }
    }

    private int calculateCompletion() {
        int hits = utilOsu.passedObjects(usergame, mode);
        if (mapPerf == null && mode == 0) return 0;
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
            // E.g.: "PerformanceCalculator.dll simulate osu 171024.osu -m hd -m dt"
            String cmdLineString = secrets.execPrefix + "dotnet " +  secrets.perfCalcPath + " simulate osu " + secrets.mapPath +
                    map.getBeatmapId() + ".osu";
            if (usergame != null)
                for (Mod mod: usergame.getEnabledMods())
                    cmdLineString += " -m " + mods_str(mod.getFlag());
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            ArrayList<String> lines = new ArrayList<>();
            String line = input.readLine();     // Skip artist, song title, difficulty
            while ((line = input.readLine()) != null)
                lines.add(line);
            mapPerf = new PerfObj(lines);
            while ((line = errors.readLine()) != null)
                logger.error(line);
            input.close();
            errors.close();
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
            String cmdLineString = secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " simulate osu " + secrets.mapPath +
                    map.getBeatmapId() + ".osu"
                    + " -a " + calculateAcc()
                    + " -c " + usergame.getMaxCombo()
                    + " -X " + usergame.getCountMiss()
                    + " -M " + usergame.getCount50()
                    + " -G " + usergame.getCount100();
            for (Mod mod: usergame.getEnabledMods())
                cmdLineString += " -m " + mods_str(mod.getFlag());
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            ArrayList<String> lines = new ArrayList<>();
            String line = input.readLine();     // Skip artist, song title, difficulty
            while ((line = input.readLine()) != null)
                lines.add(line);
            playPerf = new PerfObj(lines);
            while ((line = errors.readLine()) != null)
                logger.error(line);
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
        return usergame == null ? 0 : playPerf.getAcc();
    }

    public int getCompletion() {
        return calculateCompletion();
    }

    private static class PerfObj {
        private double acc = -1;
        private int combo;
        private int maxcombo;
        private int nobjects = 0;
        private int nmiss;
        private double pp;

        PerfObj(Iterable<String> lines) {
            for (String line: lines) {
                String[] splitLine = line.split(" ");
                switch (splitLine[0]) {
                    case "Accuracy":
                        if (this.acc > -1) break;
                        this.acc = Double.parseDouble(splitLine[splitLine.length-1].replace("%","")); break;
                    case "Combo":
                        this.combo = Integer.parseInt(splitLine[splitLine.length-2]); break;
                    case "Great":
                        this.nobjects += Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "Good":
                        this.nobjects += Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "Meh":
                        this.nobjects += Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "Miss":
                        this.nobjects += (this.nmiss = Integer.parseInt(splitLine[splitLine.length-1])); break;
                    case "Max":
                        this.maxcombo = Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "pp":
                        this.pp = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    default: break;
                }
            }
        }

        public double getAcc() {
            return acc;
        }

        public int getCombo() {
            return combo;
        }

        public int getMaxCombo() {
            return maxcombo;
        }

        public int getNobjects() {
            return nobjects;
        }

        public int getNmiss() {
            return nmiss;
        }

        public double getPp() {
            return pp;
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
