package main.java.core;

import com.oopsjpeg.osu4j.*;
import main.java.util.secrets;
import main.java.util.statics;
import main.java.util.utilOsu;
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

    private Logger logger = Logger.getLogger(this.getClass());

    private static final Set<GameMod> starModifier = new HashSet<>(Arrays.asList(GameMod.EASY, GameMod.HALF_TIME,
            GameMod.NIGHTCORE, GameMod.DOUBLE_TIME, GameMod.HARD_ROCK));

    private static final DecimalFormat df = new DecimalFormat("0.00");

    public Performance() {}

    public Performance map(OsuBeatmap map) {
        this.map = map;
        this.mode = map.getMode();
        this.mods = new HashSet<>();
        this.ppMax = 0;
        return this;
    }

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

    public void mode(GameMode mode) {
        this.mode = mode;
    }

    public void noChoke() {
        if (mode != GameMode.STANDARD) return;
        if (getCombo() == getMaxCombo()) return;
        score.setMaxcombo(map.getMaxCombo());
        double ratio = (double)score.getHit300()/(score.getHit300() + score.getHit100() + score.getHit50());
        for (; score.getMisses() > 0; score.setCountmiss(score.getMisses()-1)) {
            if (ThreadLocalRandom.current().nextDouble(1) < ratio)
                score.setCount100(score.getHit100() + 1);
            else
                score.setCount300(score.getHit300() + 1);
        }
        score.setCountmiss(0);
        this.acc = 0;
        score.setPp(0);
        double ratio300 = (double)score.getHit300()/getNObjects();
        if (score.getHit300() == getNObjects())
            score.setRank(mods.contains(GameMod.HIDDEN) ? "XH" : "X");
        else if (ratio300 > 0.9 && (double)score.getHit50()/getNObjects() < 0.01 && score.getMisses() == 0)
            score.setRank(mods.contains(GameMod.HIDDEN) ? "SH" : "S");
        else if ((ratio300 > 0.8 && score.getMisses() == 0) || ratio300 > 0.9)
            score.setRank("A");
        else if ((ratio300 > 0.7 && score.getMisses() == 0) || ratio300 > 0.8)
            score.setRank("B");
        else if (ratio300 > 0.6)
            score.setRank("C");
        else
            score.setRank("D");
    }

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

    public int getNObjects() {
        return nObjects == 0
                ? (nObjects = FileInteractor.countTotalObjects(map.getID()))
                : nObjects;
    }

    public String getRank() {
        return score.getRank();
    }

    public double getAccDouble() {
        if (acc != 0) return acc;
        return (acc = utilOsu.getAcc(score, mode, getNPassedObjects()));
    }

    public String getAcc() {
        return df.format(getAccDouble());
    }

    public double getPpMaxDouble() {
        if (ppMax != 0) return ppMax;
        try {
            this.ppMax = DBProvider.getPpRating(map.getID(), abbrvModSet(mods));
        } catch (IllegalAccessException e) {    // pp rating not yet calculated
            calculateMaxPp();
            try {
                DBProvider.addModsPp(map.getID(), abbrvModSet(mods), this.ppMax);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with ppRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                DBProvider.addMapPp(map.getID());
                calculateMaxPp();
                DBProvider.addModsPp(map.getID(), abbrvModSet(mods), this.ppMax);
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
            StringBuilder cmdLineString = new StringBuilder(statics.execPrefix + "dotnet " + statics.perfCalcPath + " simulate " + modeStr +
                    " " + secrets.mapPath + map.getID() + ".osu");
            for (GameMod mod: mods)
                cmdLineString.append(" -m ").append(mods_str((int)mod.getBit()));
            if (mode == GameMode.MANIA) {
                int max = 1000000;
                if (mods.contains(GameMod.NO_FAIL)) max *= 0.5;
                if (mods.contains(GameMod.HALF_TIME)) max *= 0.5;
                if (mods.contains(GameMod.EASY)) max *= 0.5;
                if (max < 1000000) {
                    max = Math.max(max, 130000);
                    cmdLineString.append(" -s ").append(max);
                }
            }
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
        if (score.getPp() > 0) return score.getPp();
        boolean failedPlay = score.getRank().equals("F");
        if (failedPlay && mode.getID() > 0) return score.getPp(); // Don't calculate failed scores of non-standard plays
        String mapPath = failedPlay ?
                secrets.mapPath + "temp" + map.getID() + score.getUserID() + ".osu" :
                secrets.mapPath + map.getID() + ".osu";
        try {
            if (failedPlay) {
                int lastNoteTiming = FileInteractor.offsetOfNote(getNPassedObjects(), map.getID());
                FileInteractor.copyMapUntilOffset(mapPath, map.getID(), lastNoteTiming);
            }
            String modeStr;
            switch (mode) {
                case TAIKO: modeStr = "taiko"; break;
                case MANIA: modeStr = "mania"; break;
                default: modeStr = "osu"; break;
            }
            StringBuilder cmdLineString = new StringBuilder(statics.execPrefix + "dotnet " + statics.perfCalcPath
                    + " simulate " + modeStr + " " + secrets.mapPath + map.getID() + ".osu");
            if (mode.getID() < 3) {
                cmdLineString.append(" -a ").append(getAccDouble())
                        .append(" -c ").append(score.getMaxCombo())
                        .append(" -X ").append(score.getMisses())
                        .append(mode == GameMode.STANDARD ? " -M " + score.getHit50() : "")
                        .append(" -G ").append(score.getHit100());
            } else { // mode == 3
                cmdLineString.append(" -s ").append(score.getScore());
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
                    case "pp": score.setPp(Float.parseFloat(splitLine[splitLine.length-1])); break;
                    default: break;
                }
            }
            //while ((line = errors.readLine()) != null)
            //    logger.error(line);
            input.close();
            //errors.close();
            pr.waitFor();
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
        if (mode != GameMode.STANDARD) return 0;
        return map.getMaxCombo();
    }

    public GameMode getMode() {
        return this.mode;
    }

    public int getScore() {
        return score.getScore();
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
        if (modsImportant.isEmpty() && map.getDifficulty() != 0)
            return map.getDifficulty();
        try {
            this.starRating = DBProvider.getStarRating(map.getID(), abbrvModSet(modsImportant));
        } catch (IllegalAccessException e) {    // star rating not yet calculated
            calculateStarRating(modsImportant);
            try {
                DBProvider.addModsStars(map.getID(), abbrvModSet(modsImportant), this.starRating);
            } catch (ClassNotFoundException | SQLException e1) {
                logger.error("Something went wrong while interacting with starRating database: ");
                e1.printStackTrace();
            }
        } catch (SQLException e) {              // map not in database
            try {
                DBProvider.addMapStars(map.getID());
                calculateStarRating(modsImportant);
                DBProvider.addModsStars(map.getID(), abbrvModSet(modsImportant), this.starRating);
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
        StringBuilder cmdLineString = new StringBuilder(statics.execPrefix + "dotnet " + statics.perfCalcPath + " difficulty "
                + secrets.mapPath + map.getID() + ".osu");
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
