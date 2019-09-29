package main.java.commands.Fun;

import de.gesundkrank.jskills.Rating;

public class BgGameRanking {
    private long discordUser;
    private int score;
    private Rating rating;

    public BgGameRanking(long discordUser, int score, double mu, double sigma) {
        this.discordUser = discordUser;
        this.score = score;
        this.rating = new Rating(mu, sigma);
    }

    public long getDiscordUser() {
        return discordUser;
    }

    public int getScore() {
        return score;
    }

    public Rating getRating() {
        return rating;
    }

    public void update(int score, Rating rating) {
        this.score = score;
        this.rating = rating;
    }

    public void uptate(Rating rating) {
        this.rating = rating;
    }

    public void incrementScore() {
        this.score++;
    }
}
