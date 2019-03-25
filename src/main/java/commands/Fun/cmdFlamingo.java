package main.java.commands.Fun;

public class cmdFlamingo extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "Black, white, green or blue,",
                "show off your natural hue",
                "Flamingo, oh oh owoah",
                "If you're multicouloured thats cool too",
                "You, dont, need to change,",
                "it's boring bein' the same",
                "Flamingo, oh oh oh",
                "You're pretty either way!"
        };
    }

    @Override
    int getDelay() {
        return 3000;
    }
}
