package main.java.commands.Fun;

public class cmdRockefeller extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "1 - 2 - 7 - 3,",
                "down the Rockerfeller street.",
                "Life is marchin' on, do you feel that?",
                "1 - 2 - 7 - 3,",
                "down the Rockerfeller street.",
                "Everything is more than surreal"
        };
    }

    @Override
    int getDelay() {
        return 2500;
    }
}
