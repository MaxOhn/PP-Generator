package main.java.core;

import com.oopsjpeg.osu4j.OsuBeatmap;
import main.java.util.secrets;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileInteractor {

    public static void deleteFile(String filePath) {
        File f = new File(filePath);
        if (!f.delete())
            Logger.getLogger(FileInteractor.class).error("Something went wrong while deleting a file: " + f.getName());
    }

    public static boolean downloadMapThumb(int mapID) {
        try {
            InputStream in = new URL("https://b.ppy.sh/thumb/" + mapID + "l.jpg").openStream();
            Files.copy(in, Paths.get(secrets.thumbPath + mapID + "l.jpg"), REPLACE_EXISTING);
            Logger.getLogger(FileInteractor.class).info("Downloaded thumbnail of mapset " + mapID + " successfully");
            return true;
        } catch (IOException e) {
            Logger.getLogger(FileInteractor.class).error("Something went wrong while downloading the thumbnail of a mapset: ");
            //e.printStackTrace();
            return false;
        }
    }

    public static File saveImage(BufferedImage img, String name) {
        try {
            File imgFile = new File(secrets.thumbPath + name);
            ImageIO.write(img, "png", imgFile);
            return imgFile;
        } catch (IOException e) {
            Logger.getLogger(FileInteractor.class).error("Something went wrong while saving an image:");
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteImage(String name) {
        deleteFile(secrets.thumbPath + name);
    }

    public static boolean downloadMap(int mapID) {
        String urlStr = "https://osu.ppy.sh/web/maps/" + mapID;
        String file_name = secrets.mapPath + mapID + ".osu";
        int lines = 0;
        try {
            URL url = new URL(urlStr);
            File map = new File(file_name);
            InputStream in = url.openStream();
            Files.copy(in, map.toPath(), REPLACE_EXISTING);
            BufferedReader bReader = new BufferedReader(new FileReader(map));
            while (bReader.readLine() != null) lines++;
            bReader.close();
            Logger.getLogger(FileInteractor.class).info("Downloaded map " + mapID + " successfully");
        } catch (IOException e) {
            Logger.getLogger(FileInteractor.class).error("Something went wrong while downloading a map: ");
            e.printStackTrace();
            deleteFile(file_name);
            return false;
        }
        if (lines < 50000)
            return true;
        deleteFile(file_name);
        return true;
    }

    public static int offsetOfNote(int noteIndex, int mapID) {
        int lineNum = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(secrets.mapPath + mapID + ".osu"));
            String line;
            boolean reachedHO = false;
            while ((line = reader.readLine()) != null && lineNum < noteIndex - 1) {
                if (reachedHO) lineNum++;
                reachedHO |= line.equals("[HitObjects]");
            }
            line = reader.readLine();
            String[] splitted = line.split(",");
            return Integer.parseInt(splitted[2]);
        } catch (IOException e) {
            Logger.getLogger(FileInteractor.class).error("Something went wrong while calculating the offset of a note: ");
            e.printStackTrace();
            return 0;
        } catch (Exception e) {
            Logger.getLogger(FileInteractor.class).error("Unexpected error while calculating offset of note");
            System.out.println("mapID: " + mapID + "\nnoteIndex: " + noteIndex + "\nlineNum: " + lineNum);
            e.printStackTrace();
            return 0;
        }
    }

    public static int countTotalObjects(int mapID) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(secrets.mapPath + mapID + ".osu"));
            int nobjects = 0;
            String line;
            boolean reachedHO = false;
            while ((line = reader.readLine()) != null) {
                if (reachedHO) nobjects++;
                reachedHO |= line.equals("[HitObjects]");
            }
            return nobjects;
        } catch (IOException e) {
            Logger.getLogger(FileInteractor.class).error("Something went wrong while counting the objects of a map: ");
            e.printStackTrace();
            return 0;
        }
    }

    public static void copyMapUntilOffset(String name, int mapID, int offsetLimit) {
        File submap = new File(name);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(submap));
            BufferedReader reader = new BufferedReader(new FileReader(secrets.mapPath + mapID + ".osu"));
            String line;
            boolean reachedTP = false;
            boolean reachedHO = false;
            while ((line = reader.readLine()) != null) {
                if (!reachedTP && !reachedHO)
                    writer.write(line + System.getProperty("line.separator"));
                else if (reachedTP && !line.equals("")) {
                    if (Float.parseFloat(line.split(",")[0]) <= offsetLimit)
                        writer.write(line + System.getProperty("line.separator"));
                } else if (reachedHO)
                    if (Integer.parseInt(line.split(",")[2]) <= offsetLimit)
                        writer.write(line + System.getProperty("line.separator"));
                reachedHO |= line.equals("[HitObjects]");
                reachedTP = (reachedTP || line.equals("[TimingPoints]")) && !line.equals("");
            }
            writer.close();
            reader.close();
        } catch (IOException e) {
            Logger.getLogger(FileInteractor.class).error("Something went wrong while creating a sub-map: ");
            e.printStackTrace();
        }
    }

    public static boolean prepareFiles(OsuBeatmap map) {
        boolean success = true;
        if (!new File(secrets.thumbPath + map.getBeatmapSetID() + "l.jpg").isFile())
            success = downloadMapThumb(map.getBeatmapSetID());
        if (!new File(secrets.mapPath + map.getID() + ".osu").isFile())
            success &= downloadMap(map.getID());
        return success;
    }
}
