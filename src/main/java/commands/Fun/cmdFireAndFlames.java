package main.java.commands.Fun;

public class cmdFireAndFlames extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "So far away we wait for the day-yay",
                "For the lives all so wasted and gooone",
                "We feel the pain of a lifetime lost in a thousand days",
                "Through the fire and the flames we carry ooooooon"
        };
    }

    @Override
    int getDelay() {
        return 3500;
    }
}
