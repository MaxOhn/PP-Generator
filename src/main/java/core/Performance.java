package main.java.core;

import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuScore;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilOsu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.oopsjpeg.osu4j.GameMode.*;
import static main.java.util.utilOsu.mods_intToStr;

/*
    Provides various info about a map, its pp value, its star rating, ...
 */
public class Performance {

    private OsuBeatmap map;
    private OsuScore score;

    private int nObjects;
    private int nPassedObjects;
    private int completion;

    private double starRating;
    private double ppMax;
    private double acc;
    private Set<GameMod> mods;
    private GameMode mode;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    // Mods that modify the star rating
    private static final Set<GameMod> starModifier = new HashSet<>(Arrays.asList(GameMod.EASY, GameMod.HALF_TIME,
            GameMod.NIGHTCORE, GameMod.DOUBLE_TIME, GameMod.HARD_ROCK));

    private static final DecimalFormat df = new DecimalFormat("0.00");

    public Performance() {}

    // Set a new map
    public Performance map(OsuBeatmap map) {
        this.map = map;
        this.mode = map.getMode();
        this.mods = new HashSet<>();
        this.ppMax = 0;
        return this;
    }

     // Set a new score
    public Performance osuscore(OsuScore score) {
        this.score = score;
        this.nObjects = 0;
        this.nPassedObjects = 0;

        this.acc = 0;
        this.starRating = 0;

        Set<GameMod> scoreMods = new HashSet<>(Arrays.asList(score.getEnabledMods()));
        if (this.mods != scoreMods)
            this.ppMax = 0;
        this.mods = scoreMods;

        return this;
    }

    // Set a new mode
    public void mode(GameMode mode) {
        this.mode = mode;
    }

    // Set the current state as a no-choke i.e. remove misses and make it fullcombo
    public void noChoke(int transferMissesTo) {
        if (mode != STANDARD) return;
        utilOsu.unchokeScore(score, getMaxCombo(), mode, getNObjects(), transferMissesTo);
        this.acc = 0;
    }

    // Return the amount of passed hit objects (important for fails)
    public int getNPassedObjects() {
        if (nPassedObjects != 0) return nPassedObjects;
        switch (mode) {
            case STANDARD:
                return (nPassedObjects = score.getHit300() + score.getHit100() + score.getHit50() + score.getMisses());
            case TAIKO:
                return (nPassedObjects = score.getHit300() + score.getHit100() + score.getMisses());
            case MANIA:
                return (nPassedObjects = score.getGekis() + score.getKatus() + score.getHit300() + score.getHit100()
                        + score.getHit50() + score.getMisses());
            default: return 0;
        }
    }

    // Return the total amount of hitobjects
    public int getNObjects() {
        return nObjects == 0
                ? (nObjects = FileInteractor.countTotalObjects(map.getID()))
                : nObjects;
    }

    // Return the rating of the score i.e. SS, XS, A, D, ...
    public String getRank() {
        if (score.getRank().equals(""))
            score.setRank(utilOsu.getRank(mode, score, getNObjects()));
        return score.getRank();
    }

    // Return accuracy as a double
    public double getAccDouble() {
        if (acc != 0) return acc;
        return (acc = utilOsu.getAcc(score, mode, getNPassedObjects()));
    }

    // Return accuracy as a string
    public String getAcc() {
        return df.format(getAccDouble());
    }

    // Return the max pp of the current map as a double
    public double getPpMaxDouble() {
        // If already calculated, return the precalculated value
        if (ppMax != 0) return ppMax;
        // Retrieve pp from database
        try {
            if (!secrets.WITH_DB)
                throw new IllegalArgumentException();
            this.ppMax = DBProvider.getPpRating(map.getID(), utilOsu.mods_setToStr(mods));
        } catch (IllegalAccessException e) {    // pp rating not yet calculated
            calculateMaxPp();   // calculate it, then save it
            try {
                if (secrets.WITH_DB)
                    DBProvider.addModsPp(map.getID(), utilOsu.mods_setToStr(mods), this.ppMax);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with ppRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                calculateMaxPp();   // calculate it, then save it
                if (secrets.WITH_DB) {
                    DBProvider.addMapPp(map.getID());
                    DBProvider.addModsPp(map.getID(), utilOsu.mods_setToStr(mods), this.ppMax);
                }
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with ppRating database: ");
                e1.printStackTrace();
            }
        } catch (IllegalArgumentException e) {  // mod combination not stored
            calculateMaxPp();   // calculate it without saving
        } catch (ClassNotFoundException e) {    // won't happen
            logger.error("Something went wrong while setting the pp rating: ");
            e.printStackTrace();
        }
        return ppMax;
    }

    // Calculate the max pp of the current map via osu-tool's PerformanceCalculator (no support for CtB)
    private void calculateMaxPp() {
        try {
            // Prepare mode
            String modeStr;
            switch (mode) {
                case STANDARD: modeStr = "osu"; break;
                case TAIKO: modeStr = "taiko"; break;
                case MANIA: modeStr = "mania"; break;
                default: modeStr = ""; break;
            }
            // E.g.: "PerformanceCalculator.dll simulate osu 171024.osu -m hd -m dt"
            StringBuilder cmdLineString = new StringBuilder(statics.execPrefix + "dotnet " + statics.perfCalcPath + " simulate " + modeStr +
                    " " + secrets.mapPath + map.getID() + ".osu");
            // Add mods
            for (GameMod mod: mods)
                cmdLineString.append(" -m ").append(mods_intToStr((int) mod.getBit()));
            // Modify score if mode is mania
            if (mode == MANIA) {
                int max = 1_000_000;
                if (mods.contains(GameMod.NO_FAIL)) max *= 0.5;
                if (mods.contains(GameMod.HALF_TIME)) max *= 0.5;
                if (mods.contains(GameMod.EASY)) max *= 0.5;
                if (max < 1_000_000) {
                    max = Math.max(max, 130_000);
                    cmdLineString.append(" -s ").append(max);
                }
            }
            // Run command on terminal
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString.toString());
            // Parse response
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

    // Return the max pp of the current map as a string
    public String getPpMax() {
        return df.format(getPpMaxDouble());
    }

    // Return the pp of the current score as a double
    public double getPpDouble() {
        // If pp available for the score, return them
        if (score.getPp() > 0) return score.getPp();
        boolean failedPlay = score.getRank().equals("F");
        if (failedPlay && mode.getID() > 0) return score.getPp(); // Don't calculate failed scores of non-standard plays
        String mapPath = failedPlay ?
                secrets.mapPath + "temp" + map.getID() + score.getUserID() + ".osu" :
                secrets.mapPath + map.getID() + ".osu";
        try {
            if (failedPlay) {
                // Create new map file up to the failed note
                int lastNoteTiming = FileInteractor.offsetOfNote(getNPassedObjects(), map.getID());
                FileInteractor.copyMapUntilOffset(mapPath, map.getID(), lastNoteTiming);
            }
            // Prepare mode
            String modeStr;
            switch (mode) {
                case TAIKO: modeStr = "taiko"; break;
                case MANIA: modeStr = "mania"; break;
                default: modeStr = "osu"; break;
            }
            StringBuilder cmdLineString = new StringBuilder(statics.execPrefix + "dotnet " + statics.perfCalcPath
                    + " simulate " + modeStr + " " + secrets.mapPath + map.getID() + ".osu");
            // Set score results
            if (mode.getID() < 3) {
                cmdLineString.append(" -a ").append(getAccDouble())
                        .append(" -c ").append(score.getMaxCombo())
                        .append(" -X ").append(score.getMisses())
                        .append(mode == STANDARD ? " -M " + score.getHit50() : "")
                        .append(" -G ").append(score.getHit100());
            } else { // mode == 3
                cmdLineString.append(" -s ").append(score.getScore());
            }
            // Set mods
            for (GameMod mod: mods)
                cmdLineString.append(" -m ").append(mods_intToStr((int) mod.getBit()));
            // Run command on terminal
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString.toString());
            // Parse response
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            //BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line;
            while ((line = input.readLine()) != null) {
                String[] splitLine = line.split(" ");
                if ("pp".equals(splitLine[0])) {
                    score.setPp(Float.parseFloat(splitLine[splitLine.length - 1]));
                    break;
                }
            }
            //while ((line = errors.readLine()) != null)
            //    logger.error(line);
            input.close();
            //errors.close();
            pr.waitFor();
            // Remove the temporarily created map file for failed scores
            if (failedPlay)
                FileInteractor.deleteFile(mapPath);
        } catch(Exception e) {
            logger.error("Something went wrong while calculating the pp of a play: ");
            e.printStackTrace();
        }
        return score.getPp();
    }

    public OsuBeatmap getMap() {
        return map;
    }

    public OsuScore getOsuScore() {
        return score;
    }

    // Return the pp of the current score as a string
    public String getPp() {
        return df.format(getPpDouble());
    }

    public int getN300() {
        return score.getHit300();
    }

    public int getN100() {
        return score.getHit100();
    }

    public int getN50() {
        return score.getHit50();
    }

    public int getNGeki() {
        return score.getGekis();
    }

    public int getNKatu() {
        return score.getKatus();
    }

    public int getNMisses() {
        return score.getMisses();
    }

    public int getCombo() {
        return score.getMaxCombo();
    }

    public int getMaxCombo() {
        if (mode != STANDARD) return 0;
        return map.getMaxCombo();
    }

    public GameMode getMode() {
        return this.mode;
    }

    public int getScore() {
        return score.getScore();
    }

    // For failed scores, return the % of passed hitobjects
    public int getCompletion() {
        if (completion != 0) return completion;
        int hits = getNPassedObjects();
        return (completion = (int)((double)hits*100/(double)getNObjects()));
    }

    // Return the star rating of the current map as a double
    public double getStarRatingDouble() {
        // If already calculated, return that value
        if (starRating != 0) return starRating;
        // Consider only those mods that modify the star rating
        HashSet<GameMod> modsImportant = new HashSet<>(mods);
        modsImportant.retainAll(starModifier);
        if (modsImportant.isEmpty() && map.getDifficulty() != 0)
            return map.getDifficulty();
        // Retrieve value from database
        try {
            if (!secrets.WITH_DB)
                throw new IllegalArgumentException();
            this.starRating = DBProvider.getStarRating(map.getID(), utilOsu.mods_setToStr(modsImportant));
        } catch (IllegalAccessException e) {    // star rating not yet calculated
            calculateStarRating(modsImportant); // calculate it and save it
            try {
                if (secrets.WITH_DB)
                    DBProvider.addModsStars(map.getID(), utilOsu.mods_setToStr(modsImportant), this.starRating);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                calculateStarRating(modsImportant); // calculate it and save it
                if (secrets.WITH_DB) {
                    DBProvider.addMapStars(map.getID());
                    DBProvider.addModsStars(map.getID(), utilOsu.mods_setToStr(modsImportant), this.starRating);
                }
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (IllegalArgumentException e) {  // mod combination not stored
            calculateStarRating(modsImportant); // calculate it without saving
        } catch (ClassNotFoundException e) {    // won't happen
            logger.error("Something went wrong while setting the star rating: ");
            e.printStackTrace();
        }
        return starRating;
    }

    // Return the star rating of the current map as a string
    public String getStarRating() {
        return df.format(getStarRatingDouble());
    }

    // Calculate the star rating of the current map via osu-tool's PerformanceCalculator (no support for CtB)
    private void calculateStarRating(Set<GameMod> m) {
        StringBuilder cmdLineString = new StringBuilder(statics.execPrefix + "dotnet " + statics.perfCalcPath + " difficulty "
                + secrets.mapPath + map.getID() + ".osu");
        for (GameMod mod: m)
            cmdLineString.append(" -m ").append(mods_intToStr((int)mod.getBit()));
        try {
            // Run command on terminal
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(cmdLineString.toString());
            // Parse response
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            /* // debugging
            BufferedReader errors = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line;
            while ((line = input.readLine()) != null)
                logger.warn(line);
            while ((line = errors.readLine()) != null)
                logger.error(line);
            errors.close();
            //*/
            starRating = Double.parseDouble(input.readLine().replace(",", "."));
            input.close();
            pr.waitFor();
        } catch (Exception e) {
            logger.error("Something went wrong while calculating the star rating: ");
            e.printStackTrace();
        }
    }
}
