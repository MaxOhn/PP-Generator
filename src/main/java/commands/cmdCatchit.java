package main.java.commands;

public class cmdCatchit extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "This song is one you won't forget",
                "It will get stuck -- in your head",
                "If it does, then you can't blame me",
                "Just like I said - too catchy"
        };
    }

    @Override
    int getDelay() {
        return 3500;
    }
}
