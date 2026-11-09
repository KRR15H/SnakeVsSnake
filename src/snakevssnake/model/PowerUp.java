package snakevssnake.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract power-up. Every concrete power-up overrides {@link #apply} (polymorphism):
 * the game engine just calls apply() without knowing which kind it is.
 */
public abstract class PowerUp extends GameObject {

    /** How long (in game time) a power-up stays on the board if nobody collects it. */
    public static final long LIFETIME_MS = 9000;

    private final long spawnedAt;
    private volatile double life = 1.0;       // 1.0 = just spawned, 0.0 = about to vanish

    protected PowerUp(int x, int y, long spawnedAt) {
        super(x, y);
        this.spawnedAt = spawnedAt;
    }

    /** Short name shown in the help screen. */
    public abstract String getName();

    /** One-line explanation shown in the help screen. */
    public abstract String getDescription();

    public abstract Color getColor();

    /** Single character drawn on the icon. */
    public abstract String getSymbol();

    /**
     * Applies the effect.
     *
     * @param collector the snake that picked it up
     * @param opponent  the other snake
     * @param timer     shared timer thread used for timed effects
     */
    public abstract void apply(Snake collector, Snake opponent, ScheduledExecutorService timer);

    public void updateLife(long gameClockMs) {
        life = 1.0 - (double) (gameClockMs - spawnedAt) / LIFETIME_MS;
    }

    public boolean isExpired() {
        return life <= 0;
    }

    /** Creates a random power-up of any kind. */
    public static PowerUp random(int x, int y, long clock, Random random) {
        switch (random.nextInt(4)) {
            case 0:  return new SpeedBoost(x, y, clock);
            case 1:  return new SlowDown(x, y, clock);
            case 2:  return new DoublePoints(x, y, clock);
            default: return new Shrink(x, y, clock);
        }
    }

    @Override
    public void draw(Graphics2D g, int cell) {
        // blink during the last 25% of its life so players see it is about to vanish
        if (life < 0.25 && (System.currentTimeMillis() / 140) % 2 == 0) {
            return;
        }
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0);
        int cx = x * cell + cell / 2;
        int cy = y * cell + cell / 2;
        Color c = getColor();

        int glow = (int) (cell * (1.15 + 0.25 * pulse));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
        g.fillOval(cx - glow / 2, cy - glow / 2, glow, glow);

        int d = (int) (cell * 0.78);
        g.setColor(c);
        g.fillOval(cx - d / 2, cy - d / 2, d, d);

        // countdown ring
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1.5f, cell / 11f)));
        int ring = d + Math.max(3, cell / 6);
        g.drawArc(cx - ring / 2, cy - ring / 2, ring, ring, 90, (int) (360 * Math.max(0, life)));

        // symbol
        g.setColor(new Color(20, 22, 34));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, (int) (cell * 0.55))));
        FontMetrics fm = g.getFontMetrics();
        String s = getSymbol();
        g.drawString(s, cx - fm.stringWidth(s) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }
}
