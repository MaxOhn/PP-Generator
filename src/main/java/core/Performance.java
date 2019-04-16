package main.java.core;

import com.oopsjpeg.osu4j.*;
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
import java.util.concurrent.ThreadLocalRandom;

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

    private double baseStarRating;
    private double starRating;
    private double pp;
    private double ppMax;
    private double acc;
    //private double od;
    //private double ar;
    //private double cs;
    //private double hp;

    private String rank;

    private Set<GameMod> mods;

    private GameMode mode;

    private Logger logger = Logger.getLogger(this.getClass());

    private static final Set<GameMod> starModifier = new HashSet<>(Arrays.asList(GameMod.EASY, GameMod.HALF_TIME,
            GameMod.NIGHTCORE, GameMod.DOUBLE_TIME, GameMod.HARD_ROCK));

    private static final DecimalFormat df = new DecimalFormat("0.00");

    public Performance() {}

    public Performance map(OsuBeatmap map) {
        this.mapID =  map.getID();
        this.maxCombo = map.getMaxCombo();

        this.ppMax = 0;
        this.baseStarRating = (double)map.getDifficulty();
        //this.od =  map.getDifficultyOverall();
        //this.ar = map.getDifficultyApproach();
        //this.cs = map.getDifficultySize();
        //this.hp = map.getDifficultyDrain();

        this.mode = map.getMode();

        return this;
    }

    public Performance osuscore(OsuScore score) {
        if (score.getBeatmapID() != -1) this.mapID = score.getBeatmapID();
        if (score.getUserID() != -1) this.userID = score.getUserID();
        if (score.getHit300() != -1) this.n300 = score.getHit300();
        if (score.getHit100() != -1) this.n100 = score.getHit100();
        if (score.getHit50() != -1) this.n50 = score.getHit50();
        if (score.getMisses() != -1) this.nMisses = score.getMisses();
        if (score.getGekis() != -1) this.nGeki = score.getGekis();
        if (score.getKatus() != -1) this.nKatu = score.getKatus();
        if (score.getScore() != -1) this.score = score.getScore();
        if (score.getMaxCombo() != -1) this.combo = score.getMaxCombo();
        this.nObjects = 0;
        this.nPassedObjects = 0;

        if (score.getPp() != -1) this.pp = score.getPp();
        this.acc = 0;
        this.starRating = 0;

        Set<GameMod> scoreMods = new HashSet<>(Arrays.asList(score.getEnabledMods()));
        if (this.mods != scoreMods)
            this.ppMax = 0;
        this.mods = scoreMods;

        if (!score.getRank().equals("")) this.rank = score.getRank();

        return this;
    }

    public void mode(GameMode mode) {
        this.mode = mode;
    }

    public void noChoke() {
        if (mode != GameMode.STANDARD) return;
        this.combo = this.maxCombo;
        double ratio = (double)n300/(n300 + n100 + n50);
        for (; this.nMisses > 0; this.nMisses--) {
            if (ThreadLocalRandom.current().nextDouble(1) < ratio) n100++;
            else n300++;
        }
        this.nMisses = 0;
        this.acc = 0;
        this.pp = 0;
        if (n300 == getNObjects())
            this.rank = mods.contains(GameMod.HIDDEN) ? "XH" : "X";
        else if ((double)n300/getNObjects() > 0.9 && (double)n50/getNObjects() < 0.01 && nMisses == 0)
            this.rank = mods.contains(GameMod.HIDDEN) ? "SH" : "S";
        else if (((double)n300/getNObjects() > 0.8 && nMisses == 0) || (double)n300/getNObjects() > 0.9)
            this.rank = "A";
        else if (((double)n300/getNObjects() > 0.7 && nMisses == 0) || (double)n300/getNObjects() > 0.8)
            this.rank = "B";
        else if ((double)n300/getNObjects() > 0.6)
            this.rank = "C";
        else
            this.rank = "D";
    }

    public int getNPassedObjects() {
        if (nPassedObjects != 0) return nPassedObjects;
        switch (mode) {
            case STANDARD: return (nPassedObjects = n300 + n100 + n50 + nMisses);
            case TAIKO: return (nPassedObjects = n300 + n100 + nMisses);
            case MANIA: return (nPassedObjects = nGeki + nKatu + n300 + n100 + n50 + nMisses);
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
        if (mode == GameMode.MANIA)
            numerator += (double)nKatu * 200.0D + (double)nGeki * 300.0D;
        else if (mode == GameMode.TAIKO)
            numerator = 0.5 * n100 + n300;
        double denominator;
        if (mode == GameMode.STANDARD)
            denominator = (double)(getNPassedObjects()) * 300.0D;
        else // taiko, mania
            denominator = getNPassedObjects();
        if (mode == GameMode.MANIA) denominator *= 300;
        double res = numerator / denominator;
        return (acc = 100*Math.max(0.0D, Math.min(res, 1.0D)));
    }

    public String getAcc() {
        return df.format(getAccDouble());
    }

    public double getPpMaxDouble() {
        if (ppMax != 0) return ppMax;
        try {
            this.ppMax = DBProvider.getPpRating(mapID, abbrvModSet(mods));
        } catch (IllegalAccessException e) {    // pp rating not yet calculated
            calculateMaxPp();
            try {
                DBProvider.addModsPp(mapID, abbrvModSet(mods), this.ppMax);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with ppRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                DBProvider.addMapPp(mapID);
                calculateMaxPp();
                DBProvider.addModsPp(mapID, abbrvModSet(mods), this.ppMax);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with ppRating database: ");
                e1.printStackTrace();
            }
        } catch (IllegalArgumentException e) {  // mod combination not stored
            calculateMaxPp();
        } catch (ClassNotFoundException e) {    // won't happen
            logger.error("Something went wrong while setting the pp rating: ");
            e.printStackTrace();
        }
        return ppMax;
    }

    private void calculateMaxPp() {
        try {
            String modeStr;
            switch (mode) {
                case STANDARD: modeStr = "osu"; break;
                case TAIKO: modeStr = "taiko"; break;
                case MANIA: modeStr = "mania"; break;
                default: modeStr = ""; break;
            }
            // E.g.: "PerformanceCalculator.dll simulate osu 171024.osu -m hd -m dt"
            StringBuilder cmdLineString = new StringBuilder(secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " simulate " + modeStr +
                    " " + secrets.mapPath + mapID + ".osu");
            for (GameMod mod: mods)
                cmdLineString.append(" -m ").append(mods_str((int)mod.getBit()));
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
                    //case "OD": this.od = Double.parseDouble(splitLine[splitLine.length-1]); break;
                    //case "AR": this.od = Double.parseDouble(splitLine[splitLine.length-1]); break;
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
    }

    public String getPpMax() {
        return df.format(getPpMaxDouble());
    }

    public double getPpDouble() {
        if (pp != 0) return pp;
        boolean failedPlay = rank.equals("F");
        if (failedPlay && mode.getID() > 0) return pp; // Don't calculate failed scores of non-standard plays
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
                case MANIA: modeStr = "mania"; break;
                default: modeStr = "osu"; break;
            }
            StringBuilder cmdLineString = new StringBuilder(secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " simulate " + modeStr + " "
                    + secrets.mapPath + mapID + ".osu");
            if (mode.getID() < 3) {
                cmdLineString.append(" -a ").append(getAccDouble())
                        .append(" -c ").append(combo)
                        .append(" -X ").append(nMisses)
                        .append(mode == GameMode.STANDARD ? " -M " + n50 : "").append(" -G ").append(n100);
            } else { // mode == 3
                cmdLineString.append(" -s ").append(score);
            }
            for (GameMod mod: mods)
                cmdLineString.append(" -m ").append(mods_str((int)mod.getBit()));
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
        if (mode != GameMode.STANDARD) return 0;
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
        HashSet<GameMod> modsImportant = new HashSet<>(mods);
        modsImportant.retainAll(starModifier);
        if (modsImportant.isEmpty())
            return baseStarRating;
        try {
            this.starRating = DBProvider.getStarRating(mapID, abbrvModSet(modsImportant));
        } catch (IllegalAccessException e) {    // star rating not yet calculated
            calculateStarRating(modsImportant);
            try {
                DBProvider.addModsStars(mapID, abbrvModSet(modsImportant), this.starRating);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                DBProvider.addMapStars(mapID);
                calculateStarRating(modsImportant);
                DBProvider.addModsStars(mapID, abbrvModSet(modsImportant), this.starRating);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (IllegalArgumentException e) {  // mod combination not stored
            calculateStarRating(modsImportant);
        } catch (ClassNotFoundException e) {    // won't happen
            logger.error("Something went wrong while setting the star rating: ");
            e.printStackTrace();
        }
        return starRating;
    }

    public String getStarRating() {
        return df.format(getStarRatingDouble());
    }

    private void calculateStarRating(Set<GameMod> m) {
        StringBuilder cmdLineString = new StringBuilder(secrets.execPrefix + "dotnet " + secrets.perfCalcPath + " difficulty "
                + secrets.mapPath + mapID + ".osu");
        for (GameMod mod: m)
            cmdLineString.append(" -m ").append(mods_str((int)mod.getBit()));
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
