package snakevssnake.db;

/** Leaderboard row: lifetime statistics of one player name. */
public final class PlayerStat {

    private final String name;
    private final int games;
    private final int wins;
    private final int bestScore;

    public PlayerStat(String name, int games, int wins, int bestScore) {
        this.name = name;
        this.games = games;
        this.wins = wins;
        this.bestScore = bestScore;
    }

    public String getName() { return name; }
    public int getGames() { return games; }
    public int getWins() { return wins; }
    public int getBestScore() { return bestScore; }

    public int getWinPercent() {
        return games == 0 ? 0 : Math.round(100f * wins / games);
    }
}
