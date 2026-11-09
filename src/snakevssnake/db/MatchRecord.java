package snakevssnake.db;

/** One finished match. {@code winner} is null for a draw. */
public final class MatchRecord {

    private final String player1;
    private final String player2;
    private final int score1;
    private final int score2;
    private final String winner;
    private final String mode;
    private final String playedAt;

    public MatchRecord(String player1, String player2, int score1, int score2,
                       String winner, String mode, String playedAt) {
        this.player1 = player1;
        this.player2 = player2;
        this.score1 = score1;
        this.score2 = score2;
        this.winner = (winner == null || winner.isEmpty()) ? null : winner;
        this.mode = mode;
        this.playedAt = playedAt;
    }

    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public String getWinner() { return winner; }
    public String getMode() { return mode; }
    public String getPlayedAt() { return playedAt; }

    public String getWinnerLabel() {
        return winner == null ? "Draw" : winner;
    }

    public boolean isWinner(String name) {
        return winner != null && winner.equalsIgnoreCase(name);
    }
}
