package main.java.commands.Fun;

public class cmdPretender extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "What if I say I'm not like the others?",
                "What if I say I'm not just another oooone of your plays?",
                "You're the pretender",
                "What if I say that I will never surrender?"
        };
    }

    @Override
    int getDelay() {
        return 3500;
    }
}
