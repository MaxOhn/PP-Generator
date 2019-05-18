package main.java.commands.Fun;

public class cmdSayGoodbye extends cmdSong {
    @Override
    String[] getLyrics() {
        return new String[] {
                "It still kills meeee",
                "(it - still - kills - me)",
                "That I can't change thiiiings",
                "(that I - can't - change - things)",
                "But I'm still dreaming",
                "I'll rewrite the ending",
                "So you'll take back the lies",
                "Before we say our goodbyes",
                "\\~\\~\\~ say our goodbyyeees \\~\\~\\~",
        };
    }

    @Override
    int getDelay() {
        return 2500;
    }
}
