package main.java.commands.Fun;

public class cmdBrainpower extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "**O-oooooooooo AAAAE-A-A-I-A-U- JO-oooooooooooo AAE-O-A-A-U-U-A- E-eee-ee-eee AAAAE-A-E-I-E-A- JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA**"
        };
    }

    @Override
    int getDelay() {
        return 2500;
    }
}
