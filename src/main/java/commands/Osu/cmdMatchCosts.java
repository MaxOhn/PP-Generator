package main.java.commands.Osu;

import com.oopsjpeg.osu4j.OsuMatch;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.backend.EndpointMatches;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import main.java.commands.ICommand;
import main.java.core.Main;
import main.java.util.statics;
import main.java.util.utilGeneral;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.dv8tion.jda.core.utils.tuple.ImmutablePair;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class cmdMatchCosts implements ICommand {

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            event.getChannel().sendMessage(help(0)).queue();
            return false;
        }
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (args.length == 0) {
            event.getChannel().sendMessage(help(1)).queue();
            return;
        }
        // Parse match id from arguments
        int matchID;
        try {
            matchID = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            Pattern p = Pattern.compile(".*/matches/([0-9]*)");
            Matcher m = p.matcher(args[0]);
            if (m.find()) {
                matchID = Integer.parseInt(m.group(1));
            } else {
                event.getChannel().sendMessage(help(2)).queue();
                return;
            }
        }
        // Retrieve match
        OsuMatch match;
        try {
            match = Main.osu.matches.query(new EndpointMatches.ArgumentsBuilder(matchID).build());
        } catch (OsuAPIException e) {
            event.getChannel().sendMessage("Some osu! API issue, blame bade").queue();
            return;
        }
        int warmups = 2;
        if (args.length > 1) {
            try {
                warmups = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignore) {}
        }
        String matchTitle = match.getName().replaceAll("[()]", "");
        if (match.getGames().size() <= warmups) {
            StringBuilder msg = new StringBuilder("No games played yet in the match `").append(matchTitle).append("`");
            if (match.getGames().size() > 0 && warmups > 0)
                msg.append(" beyond the ").append(warmups).append("warmup").append(warmups > 1 ? "s" : "");
            event.getChannel().sendMessage(msg).queue();
            return;
        }
        OsuMatch.Game[] games = match
                .getGames()
                .stream()
                .skip(warmups)
                .toArray(OsuMatch.Game[]::new);
        // Key: UserID, Value: TeamInt
        HashMap<Integer, Integer> teams = new HashMap<>();
        // Key: UserID, Value: List of pointCosts
        // Point costs: How well did a user do compared to the average of the _game_
        HashMap<Integer, ArrayList<Double>> pointCosts = new HashMap<>();
        boolean teamVS = games[0].getTeamType() == OsuMatch.TeamType.TEAM_VS;
        // For each game
        for (OsuMatch.Game game : games) {
            // All scores summed up
            int scoreSum = game
                    .getScores()
                    .stream()
                    .map(OsuMatch.Game.Score::getScore)
                    .reduce(0, Integer::sum);
            double avg = (double) scoreSum / game.getScores().size();
            // Calculate point costs for each user in the game
            for (OsuMatch.Game.Score score : game.getScores()) {
                double pointCost = (score.getScore() / avg) + 0.4;
                pointCosts
                        .computeIfAbsent(score.getUserID(), k -> new ArrayList<>())
                        .add(pointCost);
                teams.computeIfAbsent(score.getUserID(), k -> teamVS ? score.getTeam() : k);
            }
        }
        // Key: TeamInt / UserID, Value: HashMap with Key: UserID, Value: Username-MatchCosts pair
        // Match costs: How well did a user do compared to the rest of the _match_
        HashMap<Integer, HashMap<Integer, ImmutablePair<String, Double>>> data = new HashMap<>();
        double highestCosts = 0;
        int mvpID = 0;
        // For each user in the match
        for (Map.Entry<Integer, ArrayList<Double>> entry: pointCosts.entrySet()) {
            // Retrieve the username
            OsuUser user;
            try {
                user = Main.osu.users.query(new EndpointUsers.ArgumentsBuilder(entry.getKey()).build());
            } catch (OsuAPIException e) {
                event.getChannel().sendMessage("Some osu! API issue, blame bade").queue();
                return;
            }
            // Calculate the match costs
            double pointCostSum = entry.getValue().stream().reduce(0.0, Double::sum);
            double matchCost = pointCostSum / entry.getValue().size();
            matchCost *= Math.pow(1.2, Math.pow((double)entry.getValue().size() / games.length, 0.4));
            data.computeIfAbsent(teams.get(user.getID()), k -> new HashMap<>())
                    .put(user.getID(), new ImmutablePair<>(user.getUsername(), matchCost));
            if (matchCost > highestCosts) {
                highestCosts = matchCost;
                mvpID = user.getID();
            }
        }
        // Formulate the message
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.green)
                .setTitle(matchTitle, "https://osu.ppy.sh/community/matches/" + matchID)
                .setThumbnail("https://a.ppy.sh/" + mvpID);
        StringBuilder description = new StringBuilder();
        if (teamVS) {
            // Prepare lists
            List<ImmutablePair<String, Double>> bluePlayers = new ArrayList<>(data.get(1).values());
            bluePlayers.sort(new MatchCostComparator());
            List<ImmutablePair<String, Double>> redPlayers = new ArrayList<>(data.get(2).values());
            redPlayers.sort(new MatchCostComparator());
            boolean blueHasMvp = bluePlayers.size() > 0 &&
                    (redPlayers.size() == 0 || bluePlayers.get(0).getRight() > redPlayers.get(0).getRight());

            // Put players into string
            description.append(":blue_circle: **Blue Team** :blue_circle:\n");
            buildFromList(description, bluePlayers, blueHasMvp);
            description.append("\n:red_circle: **Red Team** :red_circle:\n");
            buildFromList(description, redPlayers, !blueHasMvp);
            // Delete last '\n'
            if (redPlayers.size() > 0)
                description.deleteCharAt(description.length() - 1);
        } else {
            // Prepare list
            List<ImmutablePair<String, Double>> players = data.values()
                    .stream()
                    .flatMap(v -> v.values().stream())
                    .sorted(new MatchCostComparator())
                    .collect(Collectors.toList());
            // Put players into string
            buildFromList(description, players, true);
            // Delete last '\n'
            if (players.size() > 0)
                description.deleteCharAt(description.length() - 1);
        }
        eb.setDescription(description);
        MessageAction msg = event.getChannel().sendMessage(eb.build());
        if (warmups > 0) {
            StringBuilder content = new StringBuilder("Ignoring the first ")
                    .append(warmups)
                    .append(" maps as warmup");
            if (warmups > 1)
                content.append("s");
            msg = msg.append(content);
        }
        msg.queue();
    }

    @Override
    public String help(int hCode) {
        String help = " (`" + statics.prefix + "matchcosts -h` for more help)";
        switch(hCode) {
            case 0:
                return "Enter `" + statics.prefix + "matchcosts [match url or match id] [amount warmups]` to make me calculate a performance "
                        + "rating for each player in the multiplayer match."
                        + "\nIf no warmup amount is specified, it defaults to 2."
                        + "\nI will assume that the match type (e.g. TeamVs, HeadToHead, ...) will not change after the first non-warmup map."
                        + "\nI will also assume that each player will be in no more than 1 team during the match."
                        + "\nIf those assumptions are wrong, I might produce garbage :^)";
            case 1:
                return "Requires at least 1 argument, namely either the URL to the match, or just the match id." + help;
            case 2:
                return "Could not parse match id. Make sure to give it either as pure number or as URL." + help;
            default:
                return help(0);
        }
    }

    @Override
    public utilGeneral.Category getCategory() {
        return utilGeneral.Category.OSU_GENERAL;
    }

    static class MatchCostComparator implements Comparator<ImmutablePair<String, Double>> {
        public int compare(ImmutablePair<String, Double> a, ImmutablePair<String, Double> b) {
            return b.getRight().compareTo(a.getRight());
        }
    }

    void buildFromList(StringBuilder builder, List<ImmutablePair<String, Double>> players, boolean withMVP) {
        int idx = 0;
        for (ImmutablePair<String, Double> player : players) {
            builder.append("**")
                    .append(++idx)
                    .append("**: [")
                    .append(player.getLeft())
                    .append("](https://osu.ppy.sh/users/")
                    .append(player.getLeft().replaceAll(" ", "+"))
                    .append(") - **")
                    .append((double)Math.round(100 * player.getRight()) / 100)
                    .append("**");
            if (withMVP && idx == 1)
                builder.append(" :crown:");
            builder.append("\n");
        }
    }
}
