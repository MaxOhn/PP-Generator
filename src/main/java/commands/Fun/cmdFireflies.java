package main.java.commands.Fun;

public class cmdFireflies extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "You would not believe your eyes",
                "If ten million fireflies",
                "Lit up the world as I fell asleep",
                "'Cause they'd fill the open air",
                "And leave teardrops everywhere",
                "You'd think me rude, but I would just stand and stare"
        };
    }

    @Override
    int getDelay() {
        return 3500;
    }
}
