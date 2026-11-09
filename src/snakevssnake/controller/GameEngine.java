package snakevssnake.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import snakevssnake.model.Direction;
import snakevssnake.model.EffectType;
import snakevssnake.model.Food;
import snakevssnake.model.GameConfig;
import snakevssnake.model.Obstacle;
import snakevssnake.model.PowerUp;
import snakevssnake.model.Snake;

/**
 * The rules of the game. It owns the board state and advances it in small time steps.
 *
 * Threading: update() is called by the game-loop thread, paint/steer by the Swing thread and
 * timed effects expire on the effect-timer thread. Every public method is synchronized so
 * the Swing thread never sees a half-updated board.
 *
 * Collision detection uses a HashSet of occupied cells (key = y * COLS + x), which gives O(1)
 * lookups instead of scanning every snake segment for every check.
 */
public class GameEngine {

    public static final int COLS = 32;
    public static final int ROWS = 22;

    public enum State { COUNTDOWN, RUNNING, PAUSED, OVER }

    private static final int FOOD_COUNT = 2;
    private static final int MAX_OBSTACLES = 14;
    private static final int MAX_POWER_UPS = 2;
    private static final long COUNTDOWN_MS = 3000;
    private static final long OBSTACLE_EVERY_MS = 4000;
    private static final long POWER_UP_EVERY_MS = 6000;
    private static final int START_LENGTH = 3;

    private final GameConfig config;
    private final Random random = new Random();
    private final Snake player1;
    private final Snake player2;
    private final List<Food> foods = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final ScheduledExecutorService effectTimer;

    private GameListener listener = new GameListener() { };
    private State state = State.COUNTDOWN;
    private long countdownLeft = COUNTDOWN_MS;
    private long clock;                       // game time in ms; frozen while paused
    private long nextObstacleAt = 3000;
    private long nextPowerUpAt = 4500;
    private Snake winner;
    private boolean draw;

    public GameEngine(GameConfig config) {
        this.config = config;
        this.effectTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "effect-timer");
            t.setDaemon(true);
            return t;
        });
        int y1 = ROWS / 3;
        int y2 = ROWS - ROWS / 3 - 1;
        player1 = new Snake(config.getPlayer1Name(), new Color(70, 235, 130), new Color(20, 120, 90),
                6, y1, Direction.RIGHT, START_LENGTH);
        player2 = new Snake(config.getPlayer2Name(), new Color(255, 150, 70), new Color(175, 55, 70),
                COLS - 7, y2, Direction.LEFT, START_LENGTH);
        refillFood();
    }

    // ------------------------------------------------------------------ public API

    public synchronized void setListener(GameListener listener) {
        this.listener = listener == null ? new GameListener() { } : listener;
    }

    /** Player input. playerNumber is 1 or 2. Allowed during the countdown so players can pre-aim. */
    public synchronized void steer(int playerNumber, Direction direction) {
        if (state == State.COUNTDOWN || state == State.RUNNING) {
            (playerNumber == 1 ? player1 : player2).steer(direction);
        }
    }

    public synchronized void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            player1.suspendEffects();
            player2.suspendEffects();
        }
    }

    public synchronized void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            player1.resumeEffects(effectTimer);
            player2.resumeEffects(effectTimer);
        }
    }

    public synchronized void togglePause() {
        if (state == State.RUNNING) {
            pause();
        } else if (state == State.PAUSED) {
            resume();
        }
    }

    /** Stops the effect-timer thread. Call when the game is discarded. */
    public synchronized void shutdown() {
        effectTimer.shutdownNow();
    }

    /**
     * Advances the game by dt milliseconds. Called ~60 times per second by the game loop.
     * Snakes move only when their own move timer is due, so a boosted snake moves more often.
     */
    public synchronized void update(long dt) {
        if (state == State.COUNTDOWN) {
            countdownLeft -= dt;
            if (countdownLeft <= 0) {
                state = State.RUNNING;
                long first = clock + config.baseDelayMs();
                player1.setNextMoveAt(first);
                player2.setNextMoveAt(first);
                listener.onGameStart();
            }
            return;
        }
        if (state != State.RUNNING) {
            return;
        }
        clock += dt;

        for (PowerUp p : powerUps) {
            p.updateLife(clock);
        }
        powerUps.removeIf(PowerUp::isExpired);
        spawnTimedObjects();

        List<Snake> movers = new ArrayList<>(2);
        if (clock >= player1.getNextMoveAt()) {
            movers.add(player1);
        }
        if (clock >= player2.getNextMoveAt()) {
            movers.add(player2);
        }
        if (!movers.isEmpty()) {
            step(movers);
        }
    }

    public synchronized void drawWorld(Graphics2D g, int cell) {
        for (Obstacle o : obstacles) {
            o.draw(g, cell);
        }
        for (Food f : foods) {
            f.draw(g, cell);
        }
        for (PowerUp p : powerUps) {
            p.draw(g, cell);
        }
        player1.draw(g, cell);
        player2.draw(g, cell);
    }

    // ------------------------------------------------------------------ getters

    public GameConfig getConfig() {
        return config;
    }

    public Snake getPlayer1() {
        return player1;
    }

    public Snake getPlayer2() {
        return player2;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized int getCountdownSeconds() {
        return (int) Math.ceil(Math.max(0, countdownLeft) / 1000.0);
    }

    public synchronized long getElapsedMs() {
        return clock;
    }

    /** Null if the game is not over yet or ended in a draw. */
    public synchronized Snake getWinner() {
        return winner;
    }

    public synchronized boolean isDraw() {
        return draw;
    }

    public synchronized String getResultTitle() {
        if (state != State.OVER) {
            return "";
        }
        return draw ? "IT'S A DRAW!" : winner.getName().toUpperCase() + " WINS!";
    }

    // ------------------------------------------------------------------ one simulation step

    /**
     * Moves every snake in {@code movers} by one cell and resolves collisions.
     * All new head positions are computed first and checked against the board as it was before
     * the step, so the result does not depend on which player is processed first.
     */
    private void step(List<Snake> movers) {
        // every occupied cell of both snakes
        Set<Integer> blocked = new HashSet<>();
        for (Snake s : new Snake[]{player1, player2}) {
            for (Point p : s.cells()) {
                blocked.add(key(p.x, p.y));
            }
        }

        // where does each mover want to go? A non-growing mover frees its tail cell.
        Map<Snake, Point> target = new HashMap<>();
        for (Snake m : movers) {
            Point t = m.peekNextHead();
            target.put(m, t);
            boolean growing = m.willGrow() || foodAt(t.x, t.y) != null;
            if (!growing) {
                Point tail = m.tail();
                blocked.remove(key(tail.x, tail.y));
            }
        }

        // who crashes?
        Map<Snake, String> crashed = new HashMap<>();
        for (Snake m : movers) {
            Point t = target.get(m);
            String reason = null;
            if (!inside(t)) {
                reason = "hit the wall";
            } else if (obstacleAt(t.x, t.y) != null) {
                reason = "hit an obstacle";
            } else if (blocked.contains(key(t.x, t.y))) {
                reason = m.occupies(t.x, t.y) ? "ran into itself"
                        : "ran into " + other(m).getName() + "'s snake";
            } else {
                for (Snake o : movers) {
                    if (o != m && target.get(o).equals(t)) {
                        reason = "crashed head-on with " + o.getName();
                    }
                }
            }
            if (reason != null) {
                crashed.put(m, reason);
            }
        }

        // survivors move, eat and collect
        for (Snake m : movers) {
            if (crashed.containsKey(m)) {
                continue;
            }
            Point t = target.get(m);
            Food food = foodAt(t.x, t.y);
            PowerUp powerUp = powerUpAt(t.x, t.y);
            if (food != null) {
                foods.remove(food);
                m.grow(1);
                m.addScore(Food.POINTS * (m.hasEffect(EffectType.DOUBLE_POINTS) ? 2 : 1));
                listener.onFoodEaten(m);
            }
            m.advance(t);
            m.setNextMoveAt(clock + m.moveDelay(config.baseDelayMs()));
            if (powerUp != null) {
                powerUps.remove(powerUp);
                powerUp.apply(m, other(m), effectTimer);
                listener.onPowerUpCollected(m, powerUp);
            }
        }

        if (crashed.isEmpty()) {
            refillFood();
            return;
        }
        for (Map.Entry<Snake, String> e : crashed.entrySet()) {
            e.getKey().kill(e.getValue());
            listener.onCrash(e.getKey());
        }
        finishGame();
    }

    private void finishGame() {
        state = State.OVER;
        boolean alive1 = player1.isAlive();
        boolean alive2 = player2.isAlive();
        if (alive1 && !alive2) {
            winner = player1;
        } else if (alive2 && !alive1) {
            winner = player2;
        } else if (player1.getScore() > player2.getScore()) {      // both crashed: higher score wins
            winner = player1;
        } else if (player2.getScore() > player1.getScore()) {
            winner = player2;
        } else {
            draw = true;
        }
        player1.clearEffects();
        player2.clearEffects();
        effectTimer.shutdownNow();
        listener.onGameOver(this);
    }

    // ------------------------------------------------------------------ spawning

    private void spawnTimedObjects() {
        if (config.getMode().hasObstacles() && clock >= nextObstacleAt) {
            nextObstacleAt = clock + OBSTACLE_EVERY_MS;
            if (obstacles.size() < MAX_OBSTACLES) {
                Point p = randomFreeCell(6);          // never right in front of a snake
                if (p != null) {
                    obstacles.add(new Obstacle(p.x, p.y));
                }
            }
        }
        if (config.getMode().hasPowerUps() && clock >= nextPowerUpAt) {
            nextPowerUpAt = clock + POWER_UP_EVERY_MS;
            if (powerUps.size() < MAX_POWER_UPS) {
                Point p = randomFreeCell(0);
                if (p != null) {
                    powerUps.add(PowerUp.random(p.x, p.y, clock, random));
                }
            }
        }
    }

    private void refillFood() {
        while (foods.size() < FOOD_COUNT) {
            Point p = randomFreeCell(0);
            if (p == null) {
                return;                                // board is full
            }
            foods.add(new Food(p.x, p.y));
        }
    }

    /** Picks a random empty cell; returns null if none was found. Retries instead of scanning the grid. */
    private Point randomFreeCell(int minDistanceFromHeads) {
        Set<Integer> occupied = occupiedKeys();
        for (int attempt = 0; attempt < 300; attempt++) {
            int x = random.nextInt(COLS);
            int y = random.nextInt(ROWS);
            if (occupied.contains(key(x, y))) {
                continue;
            }
            if (minDistanceFromHeads > 0
                    && (distance(player1.head(), x, y) < minDistanceFromHeads
                    || distance(player2.head(), x, y) < minDistanceFromHeads)) {
                continue;
            }
            return new Point(x, y);
        }
        return null;
    }

    private Set<Integer> occupiedKeys() {
        Set<Integer> set = new HashSet<>();
        for (Snake s : new Snake[]{player1, player2}) {
            for (Point p : s.cells()) {
                set.add(key(p.x, p.y));
            }
        }
        for (Food f : foods) {
            set.add(key(f.getX(), f.getY()));
        }
        for (Obstacle o : obstacles) {
            set.add(key(o.getX(), o.getY()));
        }
        for (PowerUp p : powerUps) {
            set.add(key(p.getX(), p.getY()));
        }
        return set;
    }

    // ------------------------------------------------------------------ small helpers

    private Snake other(Snake s) {
        return s == player1 ? player2 : player1;
    }

    private static int key(int x, int y) {
        return y * COLS + x;
    }

    private static boolean inside(Point p) {
        return p.x >= 0 && p.x < COLS && p.y >= 0 && p.y < ROWS;
    }

    private static int distance(Point head, int x, int y) {
        return Math.abs(head.x - x) + Math.abs(head.y - y);
    }

    private Food foodAt(int x, int y) {
        for (Food f : foods) {
            if (f.occupies(x, y)) {
                return f;
            }
        }
        return null;
    }

    private Obstacle obstacleAt(int x, int y) {
        for (Obstacle o : obstacles) {
            if (o.occupies(x, y)) {
                return o;
            }
        }
        return null;
    }

    private PowerUp powerUpAt(int x, int y) {
        for (PowerUp p : powerUps) {
            if (p.occupies(x, y)) {
                return p;
            }
        }
        return null;
    }
}
