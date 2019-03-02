package main.java.core;

import de.maxikg.osuapi.model.*;
import main.java.util.secrets;
import main.java.util.utilOsu;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import sun.misc.Perf;

import java.io.*;
import java.util.ArrayList;

import static de.maxikg.osuapi.model.Mod.createSum;
import static main.java.util.utilOsu.mods_str;


public class Performance {

    private PerfObj mapPerf = null;
    private PerfObj playPerf = null;
    private Beatmap map;
    private UserGame usergame = null;
    private BeatmapScore mapscore = null;
    private UserScore userscore = null;
    private Logger logger = Logger.getLogger(this.getClass());
    private int mode;

    public Performance(Beatmap map, Object score, int mode) {
        this.mode = mode;
        this.map = map;
        if (score instanceof BeatmapScore)
            this.mapscore = (BeatmapScore)score;
        else if (score instanceof UserScore)
            this.userscore = (UserScore)score;
        else if (score instanceof UserGame)
            this.usergame = (UserGame)score;
        prepareFiles();
        calculateMapPP();
        if (usergame != null)
            calculatePlayPP();
    }

    private int calculateCompletion() {
        int hits = utilOsu.passedObjects(usergame, mode);
        return (int)((double)hits*100/(double)mapPerf.getNobjects());
    }

    private void prepareFiles() {
        if (!new File(secrets.thumbPath + map.getBeatmapSetId() + ".jpg").isFile())
            Main.fileInteractor.downloadMapThumb(map.getBeatmapSetId());
        if (!new File(secrets.mapPath + map.getBeatmapId() + ".osu").isFile())
            Main.fileInteractor.downloadMap(map.getBeatmapId());
    }

    private void calculateMapPP() {
        try {
            String modeStr;
            switch (mode) {
                case 1: modeStr = "taiko"; break;
                case 3: modeStr = "mania"; break;
                default: modeStr = "osu"; break;
            }
            // E.g.: "PerformanceCalculator.dll simulate osu 171024.osu -m hd -m dt"
            String cmdLineString = secrets.execPrefix + "dotnet " +  secrets.perfCalcPath + " simulate " + modeStr +
                    " " + secrets.mapPath + map.getBeatmapId() + ".osu";
            if (usergame != null)
                for (Mod mod: usergame.getEnabledMods())
                    cmdLineString += " -m " + mods_str(mod.getFlag());
            else if (mapscore != null)
                for (Mod mod: mapscore.getEnabledMods())
                    cmdLineString += " -m " + mods_str(mod.getFlag());
            else if (userscore != null)
                for (Mod mod: userscore.getEnabledMods())
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
            if (mode == 3) {
                mapPerf.setNobjects(Main.fileInteractor.countTotalObjects(map.getBeatmapId()));
                mapPerf.setAcc(100);
            }
        } catch (Exception e) {
            logger.error("Something went wrong while calculating the pp of a map: " + e);
        }
    }

    private double calculateAcc() {
        double numerator = (double)usergame.getCount50() * 50.0D + (double)usergame.getCount100() * 100.0D +
                (double)usergame.getCount300() * 300.0D;
        if (mode == 3)
            numerator += (double)usergame.getCountKatu() * 200.0D + (double)usergame.getCountGeki() * 300.0D;
        else if (mode == 1)
            numerator = 0.5 * usergame.getCount100() + usergame.getCount300();
        double denominator;
        if (mode == 0)
            denominator = (double)(mapPerf.getNobjects()) * 300.0D;
        else // (mode == 3 || mode == 1)
            denominator = mapPerf.getNobjects();
        double res = numerator / denominator;
        return 100*Math.max(0.0D, Math.min(res, 1.0D));
    }

    private void calculatePlayPP() {
        boolean failedPlay = usergame.getRank().equals("F");
        if (failedPlay && mode > 0) return; // Don't calculate failed scores of non-standard plays
        String mapPath = failedPlay ?
                secrets.mapPath + "temp" + map.getBeatmapId() + usergame.getUserId() + ".osu" :
                secrets.mapPath + map.getBeatmapId() + ".osu";
        try {
            if (failedPlay) {
                int lastNoteTiming = Main.fileInteractor.offsetOfNote(utilOsu.passedObjects(usergame, mode), map.getBeatmapId());
                Main.fileInteractor.copyMapUntilOffset(mapPath, map.getBeatmapId(), lastNoteTiming);
            }
            String modeStr;
            switch (mode) {
                case 1: modeStr = "taiko"; break;
                case 3: modeStr = "mania"; break;
                default: modeStr = "osu"; break;
            }
            String cmdLineString = secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " simulate " + modeStr + " "
                    + secrets.mapPath + map.getBeatmapId() + ".osu";
            if (mode < 3) {
                cmdLineString += " -a " + calculateAcc()
                        + " -c " + usergame.getMaxCombo()
                        + " -X " + usergame.getCountMiss()
                        + (mode == 0 ? " -M " + usergame.getCount50() : "")
                        + " -G " + usergame.getCount100();
            } else { // mode == 3
                cmdLineString += " -s " + usergame.getScore();
            }
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
            if (mode == 3) {
                playPerf.setNobjects(mapPerf.getNobjects());
                playPerf.setAcc(calculateAcc());
            }
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
        return usergame == null ? 0 : playPerf.getAcc();
    }

    public int getCompletion() {
        return calculateCompletion();
    }

    private static class PerfObj {
        private double acc = -1;
        private int maxcombo = 0;
        private int nobjects = 0;
        private double pp;

        PerfObj(Iterable<String> lines) {
            for (String line: lines) {
                String[] splitLine = line.split(" ");
                switch (splitLine[0]) {
                    case "Accuracy":
                        if (this.acc > -1) break;
                        this.acc = Double.parseDouble(splitLine[splitLine.length-1].replace("%","")); break;
                    case "Greats":
                    case "Great":
                    case "Goods":
                    case "Good":
                    case "Meh":
                    case "Misses":
                    case "Miss":
                        this.nobjects += Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "Max":
                        this.maxcombo = Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "pp":
                        this.pp = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    default: break;
                }
            }
        }

        double getAcc() {
            return acc;
        }

        void setAcc(double acc) {
            this.acc = acc;
        }

        int getMaxCombo() {
            return maxcombo;
        }

        void setMaxCombo(int maxcombo) {
            this.maxcombo = maxcombo;
        }

        int getNobjects() {
            return nobjects;
        }

        void setNobjects(int nobjects) {
            this.nobjects = nobjects;
        }

        double getPp() {
            return pp;
        }
    }
}
