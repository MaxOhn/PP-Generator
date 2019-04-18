package main.java.commands.Fun;

public class cmdBombsAway extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "Tick tick tock and it's bombs awayyyy",
                "Come ooon, it's the only way",
                "Save your-self for a better dayyyy",
                "No, no, we are falling dooo-ooo-ooo-ooown",
                "I know, you know - this is over",
                "Tick tick tock and it's bombs awayyyy",
                "Now we're falling -- now we're falling doooown"
        };
    }

    @Override
    int getDelay() {
        return 3500;
    }
}
