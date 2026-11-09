package snakevssnake.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * One player's snake.
 *
 * The body is a Deque of cells (head first). Moving = add a new head, drop the tail.
 * All public methods are synchronized because the snake is touched by three threads:
 * the game-loop thread (moves), the Swing thread (painting) and the effect-timer thread
 * (power-up expiry).
 */
public class Snake implements Drawable, Collidable {

    public static final int MIN_LENGTH = 3;

    private final String name;
    private final Color headColor;
    private final Color tailColor;
    private final Deque<Point> body = new ArrayDeque<>();

    private Direction direction;      // direction of the last completed move
    private Direction pending;        // direction requested by the player for the next move
    private int growPending;
    private int score;
    private boolean alive = true;
    private long nextMoveAt;
    private String deathReason = "";

    // ---- timed power-up effects (expire on a separate timer thread) ----
    private static final int EFFECTS = EffectType.values().length;
    private final boolean[] active = new boolean[EFFECTS];
    private final long[] endAt = new long[EFFECTS];
    private final long[] remaining = new long[EFFECTS];
    private final int[] generation = new int[EFFECTS];
    private final ScheduledFuture<?>[] futures = new ScheduledFuture<?>[EFFECTS];
    private boolean suspended;

    public Snake(String name, Color headColor, Color tailColor,
                 int headX, int headY, Direction direction, int length) {
        this.name = name;
        this.headColor = headColor;
        this.tailColor = tailColor;
        this.direction = direction;
        this.pending = direction;
        for (int i = 0; i < length; i++) {
            body.addLast(new Point(headX - direction.dx * i, headY - direction.dy * i));
        }
    }

    // ------------------------------------------------------------------ basic info

    public String getName() {
        return name;
    }

    public Color getHeadColor() {
        return headColor;
    }

    public synchronized boolean isAlive() {
        return alive;
    }

    public synchronized void kill(String reason) {
        alive = false;
        deathReason = reason;
    }

    public synchronized String getDeathReason() {
        return deathReason;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized void addScore(int points) {
        score += points;
    }

    public synchronized int getLength() {
        return body.size();
    }

    public synchronized int getPendingGrowth() {
        return growPending;
    }

    public synchronized Direction getDirection() {
        return direction;
    }

    public synchronized long getNextMoveAt() {
        return nextMoveAt;
    }

    public synchronized void setNextMoveAt(long t) {
        nextMoveAt = t;
    }

    // ------------------------------------------------------------------ movement

    public synchronized Point head() {
        return body.peekFirst();
    }

    public synchronized Point tail() {
        return body.peekLast();
    }

    /** All cells of the body as a copy, head first. */
    public synchronized List<Point> cells() {
        return new ArrayList<>(body);
    }

    /** The cell the head will enter on the next move. */
    public synchronized Point peekNextHead() {
        Point h = body.peekFirst();
        return new Point(h.x + pending.dx, h.y + pending.dy);
    }

    /** Player input. Reversing straight into the neck is ignored. */
    public synchronized void steer(Direction d) {
        if (alive && d != direction.opposite()) {
            pending = d;
        }
    }

    public synchronized void grow(int cells) {
        growPending += cells;
    }

    /** True if the next move will not remove the tail. */
    public synchronized boolean willGrow() {
        return growPending > 0;
    }

    /** Moves the snake into newHead: adds a head and (unless growing) removes the tail. */
    public synchronized void advance(Point newHead) {
        direction = pending;
        body.addFirst(newHead);
        if (growPending > 0) {
            growPending--;
        } else {
            body.removeLast();
        }
    }

    /** Removes up to {@code cells} tail segments, never going below MIN_LENGTH. */
    public synchronized void shrink(int cells) {
        for (int i = 0; i < cells && body.size() > MIN_LENGTH; i++) {
            body.removeLast();
        }
    }

    /** Delay between moves in ms, taking speed-boost / slow effects into account. */
    public synchronized long moveDelay(long baseDelayMs) {
        double d = baseDelayMs;
        if (active[EffectType.SPEED_BOOST.ordinal()]) {
            d /= 1.7;
        }
        if (active[EffectType.SLOWED.ordinal()]) {
            d *= 1.8;
        }
        return Math.max(35, Math.round(d));
    }

    @Override
    public synchronized boolean occupies(int x, int y) {
        for (Point p : body) {
            if (p.x == x && p.y == y) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ timed effects

    /**
     * Starts (or restarts) a timed effect. The effect is switched off again by a task
     * scheduled on the shared effect-timer thread.
     */
    public synchronized void addEffect(EffectType type, long durationMs, ScheduledExecutorService timer) {
        int i = type.ordinal();
        cancelFuture(i);
        active[i] = true;
        if (suspended) {
            remaining[i] = durationMs;
            return;
        }
        endAt[i] = System.currentTimeMillis() + durationMs;
        schedule(i, durationMs, timer);
    }

    private void schedule(int i, long ms, ScheduledExecutorService timer) {
        final int gen = ++generation[i];
        futures[i] = timer.schedule(() -> expire(i, gen), ms, TimeUnit.MILLISECONDS);
    }

    private synchronized void expire(int i, int gen) {
        if (generation[i] == gen) {          // ignore stale tasks of an effect that was restarted
            active[i] = false;
            futures[i] = null;
        }
    }

    private void cancelFuture(int i) {
        generation[i]++;
        if (futures[i] != null) {
            futures[i].cancel(false);
            futures[i] = null;
        }
    }

    /** Freezes effect countdowns while the game is paused. */
    public synchronized void suspendEffects() {
        suspended = true;
        long now = System.currentTimeMillis();
        for (int i = 0; i < EFFECTS; i++) {
            if (active[i]) {
                remaining[i] = Math.max(1, endAt[i] - now);
                cancelFuture(i);
            }
        }
    }

    /** Continues effect countdowns after a pause. */
    public synchronized void resumeEffects(ScheduledExecutorService timer) {
        suspended = false;
        long now = System.currentTimeMillis();
        for (int i = 0; i < EFFECTS; i++) {
            if (active[i]) {
                endAt[i] = now + remaining[i];
                schedule(i, remaining[i], timer);
            }
        }
    }

    public synchronized void clearEffects() {
        for (int i = 0; i < EFFECTS; i++) {
            cancelFuture(i);
            active[i] = false;
        }
    }

    public synchronized boolean hasEffect(EffectType type) {
        return active[type.ordinal()];
    }

    public synchronized long effectMsLeft(EffectType type) {
        int i = type.ordinal();
        if (!active[i]) {
            return 0;
        }
        return suspended ? remaining[i] : Math.max(0, endAt[i] - System.currentTimeMillis());
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public synchronized void draw(Graphics2D g, int cell) {
        List<Point> cells = new ArrayList<>(body);
        int n = cells.size();
        Color head = alive ? headColor : new Color(165, 168, 180);
        Color tail = alive ? tailColor : new Color(95, 98, 112);

        // body from tail to head so the head ends up on top
        for (int i = n - 1; i >= 0; i--) {
            Point p = cells.get(i);
            float t = n <= 1 ? 0f : (float) i / (n - 1);          // 0 = head, 1 = tail
            g.setColor(blend(head, tail, t));
            int pad = i == 0 ? 0 : 1 + Math.round(t * cell * 0.16f);
            int size = cell - 2 * pad;
            int arc = Math.max(4, cell / 2 - pad);
            g.fillRoundRect(p.x * cell + pad, p.y * cell + pad, size, size, arc, arc);
        }

        Point h = cells.get(0);
        int hx = h.x * cell;
        int hy = h.y * cell;

        // coloured halo for every active effect
        if (alive) {
            for (EffectType type : EffectType.values()) {
                if (active[type.ordinal()]) {
                    Color c = type.getColor();
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                    int halo = (int) (cell * 1.9);
                    g.fillOval(hx + cell / 2 - halo / 2, hy + cell / 2 - halo / 2, halo, halo);
                }
            }
        }

        // eyes look in the direction of travel
        double[][] eyes;
        switch (direction) {
            case UP:    eyes = new double[][]{{0.30, 0.30}, {0.70, 0.30}}; break;
            case DOWN:  eyes = new double[][]{{0.30, 0.70}, {0.70, 0.70}}; break;
            case LEFT:  eyes = new double[][]{{0.30, 0.30}, {0.30, 0.70}}; break;
            default:    eyes = new double[][]{{0.70, 0.30}, {0.70, 0.70}}; break;
        }
        int er = Math.max(3, (int) (cell * 0.24));
        int pr = Math.max(2, (int) (cell * 0.12));
        for (double[] e : eyes) {
            int ex = hx + (int) (e[0] * cell);
            int ey = hy + (int) (e[1] * cell);
            g.setColor(alive ? Color.WHITE : new Color(230, 230, 235));
            g.fillOval(ex - er / 2, ey - er / 2, er, er);
            g.setColor(new Color(20, 22, 34));
            if (alive) {
                g.fillOval(ex - pr / 2 + direction.dx * 1, ey - pr / 2 + direction.dy * 1, pr, pr);
            } else {   // little "x" eyes when dead
                g.drawLine(ex - er / 3, ey - er / 3, ex + er / 3, ey + er / 3);
                g.drawLine(ex - er / 3, ey + er / 3, ex + er / 3, ey - er / 3);
            }
        }
    }

    private static Color blend(Color a, Color b, float t) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int gr = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, gr, bl);
    }
}
