package main.java.commands.Osu;

import main.java.util.statics;

/*
    Display the global leaderboard of a map
 */
public class cmdGlobalLeaderboard extends cmdMapLeaderboard {

    @Override
    protected lbType getType() {
        return lbType.GLOBAL;
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "globallb -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "globallb[number] [beatmap url or beatmap id] [+<nm/hd/nfeznc/...>[!]] [-<nm/hd/nfeznc/...>!]` to make me show the beatmap's "
                        + " global top 10 scores."
                        + "\nWith `+` you can chose included mods, e.g. `+hddt`, with `+mod!` you can chose exact mods, and with `-mod!` you can chose excluded mods."
                        + "\nBeatmap urls from both the new and old website are supported."
                        + "\nIf no beatmap is specified, I will search the channel's history for scores instead and consider the map of the [number]-th score, default to 1.";
            case 1:
                return "The first argument must either be the link to a beatmap e.g. `https://osu.ppy.sh/b/1613091&m=0`, or just the id of the beatmap" + help;
            default:
                return help(0);
        }
    }
}
