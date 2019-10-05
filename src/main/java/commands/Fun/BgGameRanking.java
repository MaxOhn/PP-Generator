package main.java.commands.Fun;

public class BgGameRanking {
    private long discordUser;
    private int score;
    private double rating;

    public BgGameRanking(long discordUser, int score) {
        this.discordUser = discordUser;
        this.score = score;
        this.rating = 0;
    }

    public long getDiscordUser() {
        return discordUser;
    }

    public int getScore() {
        return score;
    }

    public double getRating() {
        return rating;
    }

    public void uptate(double rating) {
        this.rating += rating;
    }

    void incrementScore() {
        this.score++;
    }
}
