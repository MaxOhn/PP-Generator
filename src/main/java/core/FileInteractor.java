package main.java.core;

import main.java.util.secrets;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileInteractor {

    private Logger logger = Logger.getLogger(this.getClass());

    public FileInteractor() {}

    public void deleteFile(String filePath) {
        File f = new File(filePath);
        if (!f.delete())
            logger.error("Something went wrong while deleting a file: " + f.getName());
    }

    public void downloadMapThumb(int mapID) {
        try {
            InputStream in = new URL("https://b.ppy.sh/thumb/" + mapID + ".jpg").openStream();
            Files.copy(in, Paths.get(secrets.thumbPath + mapID + ".jpg"), REPLACE_EXISTING);
            logger.info("Downloaded thumbnail of mapset " + mapID + " successfully");
        } catch (IOException e) {
            logger.error("Something went wrong while downloading the thumbnail of a mapset: " + e);
        }
    }

    public void downloadMap(int mapID) {
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
            logger.info("Downloaded map " + mapID + " successfully");
        } catch (IOException e) {
            logger.error("Something went wrong while downloading a map: " + e);
            deleteFile(file_name);
            return;
        }
        if (lines < 50000)
            return;
        deleteFile(file_name);
    }

    public int offsetOfNote(int noteIndex, int mapID) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(secrets.mapPath + mapID + ".osu"));
            int lineNum = 0;
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
            logger.error("Something went wrong while calculating the offset of a note: " + e);
            return 0;
        }
    }

    public void copyMapUntilOffset(String name, int mapID, int offsetLimit) {
        File submap = new File(name);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(submap));
            BufferedReader reader = new BufferedReader(new FileReader(secrets.mapPath + mapID + ".osu"));
            String line;
            boolean reachedTP = false;
            boolean reachedHO = false;
            int counter = 0;
            while ((line = reader.readLine()) != null) {
                if (!reachedTP && !reachedHO)
                    writer.write(line + System.getProperty("line.separator"));
                else if (reachedTP && !line.equals("")) {
                    if (++counter % 10 == 0)
                        counter = 0;
                    if (Integer.parseInt(line.split(",")[0]) <= offsetLimit)
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
            logger.error("Something went wrong while creating a sub-map: " + e);
        }
    }

}
