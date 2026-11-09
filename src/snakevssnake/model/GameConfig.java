package snakevssnake.model;

/** Immutable set of options chosen on the main menu. */
public final class GameConfig {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 5;

    private final String player1Name;
    private final String player2Name;
    private final Mode mode;
    private final int speedLevel;
    private final boolean soundOn;

    public GameConfig(String player1Name, String player2Name, Mode mode, int speedLevel, boolean soundOn) {
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.mode = mode;
        this.speedLevel = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speedLevel));
        this.soundOn = soundOn;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public Mode getMode() {
        return mode;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public boolean isSoundOn() {
        return soundOn;
    }

    /** Milliseconds between two moves of a normal-speed snake (level 1 = 182 ms ... level 5 = 70 ms). */
    public long baseDelayMs() {
        return 210L - speedLevel * 28L;
    }
}
