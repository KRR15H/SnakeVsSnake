package snakevssnake.model;

/** The selectable game modes. */
public enum Mode {
    CLASSIC("Classic", false, false),
    OBSTACLES("Obstacles", true, false),
    POWER_UPS("Power-ups", false, true),
    CHAOS("Chaos (obstacles + power-ups)", true, true);

    private final String label;
    private final boolean obstacles;
    private final boolean powerUps;

    Mode(String label, boolean obstacles, boolean powerUps) {
        this.label = label;
        this.obstacles = obstacles;
        this.powerUps = powerUps;
    }

    public boolean hasObstacles() {
        return obstacles;
    }

    public boolean hasPowerUps() {
        return powerUps;
    }

    /** Parses a stored name; falls back to CLASSIC if the stored value is unknown or corrupted. */
    public static Mode fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return CLASSIC;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
