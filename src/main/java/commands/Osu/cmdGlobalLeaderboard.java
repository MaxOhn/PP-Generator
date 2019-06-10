package main.java.commands.Osu;

public class cmdGlobalLeaderboard extends cmdMapLeaderboard {

    @Override
    protected lbType getType() {
        return lbType.GLOBAL;
    }

}
