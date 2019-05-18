package main.java.commands.Fun;

public class cmdTijdmachine extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
            "Als ik denk aan al die dagen,",
            "dat ik mij zo heb misdragen.",
            "Dan denk ik, - had ik maar een tijdmachine -- tijdmachine",
            "Maar die heb ik niet,",
            "dus zal ik mij gedragen,",
            "en zal ik blijven sparen,",
            "sparen voor een tijjjdmaaachine."
        };
    }

    @Override
    int getDelay() {
        return 2500;
    }
}
