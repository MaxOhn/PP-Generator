package main.java.core;

import de.maxikg.osuapi.model.*;
import main.java.util.secrets;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static main.java.util.utilOsu.abbrvModSet;
import static main.java.util.utilOsu.mods_str;


public class Performance {

    private int mapID;
    private int userID;
    private int nObjects;
    private int nPassedObjects;
    private int n300;
    private int n100;
    private int n50;
    private int nMisses;
    private int nGeki;
    private int nKatu;
    private int combo;
    private int maxCombo;
    private int score;
    private int completion;

    private double starRating;
    private double pp;
    private double ppMax;
    private double acc;
    private double od;
    private double ar;
    private double cs;
    private double hp;

    private String rank;

    private Set<Mod> mods;

    private GameMode mode;

    private Logger logger = Logger.getLogger(this.getClass());

    private static final Set<Mod> ratingModifier = new HashSet<>(Arrays.asList(Mod.EASY, Mod.HALF_TIME, Mod.NIGHTCORE,
            Mod.DOUBLE_TIME, Mod.HARD_ROCK));

    private static final DecimalFormat df = new DecimalFormat("0.00");

    public Performance() {}

    public Performance map(Beatmap map) {
        this.mapID =  map.getBeatmapId();
        this.maxCombo = map.getMaxCombo();

        if (this.mods == null || !new HashSet<>(this.mods).removeAll(ratingModifier))
            this.starRating = (double)map.getDifficultyRating();
        this.od =  map.getDifficultyOverall();
        this.ar = map.getDifficultyApproach();
        this.cs = map.getDifficultySize();
        this.hp = map.getDifficultyDrain();

        this.mode = map.getMode();

        return this;
    }

    public Performance userscore(UserScore score) {
        this.mapID = score.getBeatmapId();
        this.userID = score.getUserId();
        this.n300 = score.getCount300();
        this.n100 = score.getCount100();
        this.n50 = score.getCount50();
        this.nMisses = score.getCountMiss();
        this.nGeki = score.getCountGeki();
        this.nKatu = score.getCountKatu();
        this.score = score.getScore();
        this.combo = score.getMaxCombo();
        this.nObjects = 0;

        this.pp = (double)score.getPp();
        this.acc = 0;

        this.mods = score.getEnabledMods();
        if (new HashSet<>(this.mods).removeAll(ratingModifier))
            this.starRating = 0;

        this.rank = score.getRank();

        return this;
    }

    public Performance usergame(UserGame score) {
        this.mapID = score.getBeatmapId();
        this.userID = score.getUserId();
        this.n300 = score.getCount300();
        this.n100 = score.getCount100();
        this.n50 = score.getCount50();
        this.nMisses = score.getCountMiss();
        this.nGeki = score.getCountGeki();
        this.nKatu = score.getCountKatu();
        this.score = score.getScore();
        this.combo = score.getMaxCombo();
        this.nObjects = 0;

        this.pp = 0;
        this.acc = 0;

        this.mods = score.getEnabledMods();
        if (new HashSet<>(this.mods).removeAll(ratingModifier))
            this.starRating = 0;

        this.rank = score.getRank();

        return this;
    }

    public Performance beatmapscore(BeatmapScore score) {
        this.userID = score.getUserId();
        this.n300 = score.getCount300();
        this.n100 = score.getCount100();
        this.n50 = score.getCount50();
        this.nMisses = score.getCountMiss();
        this.nGeki = score.getCountGeki();
        this.nKatu = score.getCountKatu();
        this.score = score.getScore();
        this.combo = score.getMaxCombo();
        this.nObjects = 0;

        this.pp = (double)score.getPp();
        this.acc = 0;

        this.mods = score.getEnabledMods();
        if (new HashSet<>(this.mods).removeAll(ratingModifier))
            this.starRating = 0;

        this.rank = score.getRank();

        return this;
    }

    public Performance mode(GameMode mode) {
        this.mode = mode;
        return this;
    }

    public int getNPassedObjects() {
        if (nPassedObjects != 0) return nPassedObjects;
        switch (mode) {
            case STANDARD: return (nPassedObjects = n300 + n100 + n50 + nMisses);
            case TAIKO: return (nPassedObjects = n300 + n100 + nMisses);
            case OSU_MANIA: return (nPassedObjects = nGeki + nKatu + n300 + n100 + n50 + nMisses);
            default: return 0;
        }
    }

    public int getNObjects() {
        return nObjects == 0
                ? (nObjects = Main.fileInteractor.countTotalObjects(mapID))
                : nObjects;
    }

    public String getRank() {
        return rank;
    }

    public double getAccDouble() {
        if (acc != 0) return acc;
        double numerator = (double)n50 * 50.0D + (double)n100 * 100.0D + (double)n300 * 300.0D;
        if (mode == GameMode.OSU_MANIA)
            numerator += (double)nKatu * 200.0D + (double)nGeki * 300.0D;
        else if (mode == GameMode.TAIKO)
            numerator = 0.5 * n100 + n300;
        double denominator;
        if (mode == GameMode.STANDARD)
            denominator = (double)(getNPassedObjects()) * 300.0D;
        else // taiko, mania
            denominator = getNPassedObjects();
        double res = numerator / denominator;
        return (acc = 100*Math.max(0.0D, Math.min(res, 1.0D)));
    }

    public String getAcc() {
        return df.format(getAccDouble());
    }

    public double getPpMaxDouble() {
        if (ppMax != 0) return ppMax;
        try {
            String modeStr;
            switch (mode) {
                case STANDARD: modeStr = "osu"; break;
                case TAIKO: modeStr = "taiko"; break;
                case OSU_MANIA: modeStr = "mania"; break;
                default: modeStr = ""; break;
            }
            // E.g.: "PerformanceCalculator.dll simulate osu 171024.osu -m hd -m dt"
            StringBuilder cmdLineString = new StringBuilder(secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " simulate " + modeStr +
                    " " + secrets.mapPath + mapID + ".osu");
            for (Mod mod: mods)
                cmdLineString.append(" -m ").append(mods_str(mod.getFlag()));
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString.toString());
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            //BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line;
            while ((line = input.readLine()) != null) {
                String[] splitLine = line.split(" ");
                switch (splitLine[0]) {
                    case "Greats":
                    case "Great":
                    case "Goods":
                    case "Good":
                    case "Meh":
                    case "Misses":
                    case "Miss": this.nObjects += Integer.parseInt(splitLine[splitLine.length-1]); break;
                    case "OD": this.od = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    case "AR": this.od = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    case "pp": this.ppMax = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    default: break;
                }
            }
            //while ((line = errors.readLine()) != null)
            //    logger.error(line);
            input.close();
            //errors.close();
            pr.waitFor();
        } catch (Exception e) {
            logger.error("Something went wrong while calculating the pp of a map: ");
            e.printStackTrace();
        }
        return this.ppMax;
    }

    public String getPpMax() {
        return df.format(getPpMaxDouble());
    }

    public double getPpDouble() {
        if (pp != 0) return pp;
        boolean failedPlay = rank.equals("F");
        if (failedPlay && mode.getValue() > 0) return pp; // Don't calculate failed scores of non-standard plays
        String mapPath = failedPlay ?
                secrets.mapPath + "temp" + mapID + userID + ".osu" :
                secrets.mapPath + mapID + ".osu";
        try {
            if (failedPlay) {
                int lastNoteTiming = Main.fileInteractor.offsetOfNote(getNPassedObjects(), mapID);
                Main.fileInteractor.copyMapUntilOffset(mapPath, mapID, lastNoteTiming);
            }
            String modeStr;
            switch (mode) {
                case TAIKO: modeStr = "taiko"; break;
                case OSU_MANIA: modeStr = "mania"; break;
                default: modeStr = "osu"; break;
            }
            StringBuilder cmdLineString = new StringBuilder(secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " simulate " + modeStr + " "
                    + secrets.mapPath + mapID + ".osu");
            if (mode.getValue() < 3) {
                cmdLineString.append(" -a ").append(getAccDouble())
                        .append(" -c ").append(combo)
                        .append(" -X ").append(nMisses)
                        .append(mode == GameMode.STANDARD ? " -M " + n50 : "").append(" -G ").append(n100);
            } else { // mode == 3
                cmdLineString.append(" -s ").append(score);
            }
            for (Mod mod: mods)
                cmdLineString.append(" -m ").append(mods_str(mod.getFlag()));
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString.toString());
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            //BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line;
            while ((line = input.readLine()) != null) {
                String[] splitLine = line.split(" ");
                switch (splitLine[0]) {
                    case "pp": this.pp = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    default: break;
                }
            }
            //while ((line = errors.readLine()) != null)
            //    logger.error(line);
            input.close();
            //errors.close();
            pr.waitFor();
            if (failedPlay)
                Main.fileInteractor.deleteFile(mapPath);
        } catch(Exception e) {
            logger.error("Something went wrong while calculating the pp of a play: ");
            e.printStackTrace();
        }
        return pp;
    }

    public String getPp() {
        return df.format(getPpDouble());
    }

    public int getN300() {
        return n300;
    }

    public int getN100() {
        return n100;
    }

    public int getN50() {
        return n50;
    }

    public int getNGeki() {
        return nGeki;
    }

    public int getNKatu() {
        return nKatu;
    }

    public int getNMisses() {
        return nMisses;
    }

    public int getCombo() {
        return combo;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public int getScore() {
        return score;
    }

    public int getCompletion() {
        if (completion != 0) return completion;
        int hits = getNPassedObjects();
        return (completion = (int)((double)hits*100/(double)getNObjects()));
    }

    public double getStarRatingDouble() {
        if (starRating != 0) return starRating;
        HashSet<Mod> modsImportant = new HashSet<>(mods);
        modsImportant.retainAll(ratingModifier);
        if (modsImportant.isEmpty())
            return starRating;
        if (modsImportant.contains(Mod.NIGHTCORE)) {
            modsImportant.remove(Mod.NIGHTCORE);
            modsImportant.add(Mod.DOUBLE_TIME);
        }
        try {
            this.starRating = DBProvider.getStarRating(mapID, abbrvModSet(modsImportant));
        } catch (IllegalAccessException e) {    // star rating not yet calculated
            calculateStarRating(modsImportant);
            try {
                DBProvider.addMods(mapID, abbrvModSet(modsImportant), this.starRating);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                DBProvider.addMap(mapID);
                calculateStarRating(modsImportant);
                DBProvider.addMods(mapID, abbrvModSet(modsImportant), this.starRating);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (ClassNotFoundException e) {    // won't happen
            logger.error("Something went wrong while setting the star rating: ");
            e.printStackTrace();
        }
        return starRating;
    }

    public String getStarRating() {
        return df.format(getStarRatingDouble());
    }

    private void calculateStarRating(Set<Mod> m) {
        StringBuilder cmdLineString = new StringBuilder(secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " difficulty "
                + secrets.mapPath + mapID + ".osu");
        for (Mod mod: m)
            cmdLineString.append(" -m ").append(mods_str(mod.getFlag()));
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString.toString());
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            //BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            /* // debugging
            String line;
            while ((line = input.readLine()) != null)
                logger.error(line);
            while ((line = errors.readLine()) != null)
                logger.error(line);
            //*/
            starRating = Double.parseDouble(input.readLine());
            input.close();
            //errors.close();
            pr.waitFor();
        } catch (InterruptedException | IOException e) {
            logger.error("Something went wrong while calculating the star rating: ");
            e.printStackTrace();
        }
    }
}
