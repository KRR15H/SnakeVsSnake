package snakevssnake;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import snakevssnake.controller.GameEngine;
import snakevssnake.controller.GameListener;
import snakevssnake.db.FileScoreStore;
import snakevssnake.db.JdbcScoreStore;
import snakevssnake.db.MatchRecord;
import snakevssnake.db.PlayerStat;
import snakevssnake.db.ScoreStore;
import snakevssnake.db.StorageException;
import snakevssnake.model.Direction;
import snakevssnake.model.DoublePoints;
import snakevssnake.model.EffectType;
import snakevssnake.model.GameConfig;
import snakevssnake.model.Mode;
import snakevssnake.model.PowerUp;
import snakevssnake.model.Shrink;
import snakevssnake.model.SlowDown;
import snakevssnake.model.Snake;
import snakevssnake.model.SpeedBoost;
import snakevssnake.util.InputValidator;
import snakevssnake.util.InvalidInputException;
import snakevssnake.util.SettingsManager;

/**
 * Plain-Java test suite (no JUnit needed).
 * Run:  java -cp out:test-out snakevssnake.GameTest     (use ; instead of : on Windows)
 */
public class GameTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testSnakeBasics();
        testPowerUps();
        testEffectTimers();
        testEngineCountdownAndSteering();
        testWallCrashGivesWinner();
        testValidator();
        testSettings();
        testStore("FileScoreStore", new FileScoreStore(Files.createTempDirectory("svs").resolve("m.tsv")));
        testJdbcIfAvailable();
        fuzz();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ helpers

    private static void check(boolean condition, String message) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL: " + message);
        }
    }

    private static Snake newSnake() {
        return new Snake("T", java.awt.Color.GREEN, java.awt.Color.BLUE, 10, 10, Direction.RIGHT, 3);
    }

    private static GameConfig config(Mode mode, int speed) {
        return new GameConfig("Alice", "Bob", mode, speed, false);
    }

    // ------------------------------------------------------------------ unit tests

    private static void testSnakeBasics() {
        System.out.println("Snake basics");
        Snake s = newSnake();
        check(s.getLength() == 3, "start length is 3");
        check(s.head().equals(new Point(10, 10)), "head position");
        check(s.tail().equals(new Point(8, 10)), "tail position");

        s.steer(Direction.LEFT);                                   // reverse is ignored
        check(s.peekNextHead().equals(new Point(11, 10)), "reversing into the neck is ignored");
        s.steer(Direction.UP);
        check(s.peekNextHead().equals(new Point(10, 9)), "turning up works");

        s.advance(s.peekNextHead());
        check(s.getLength() == 3 && s.head().equals(new Point(10, 9)), "moving keeps length");
        s.steer(Direction.DOWN);                                   // now moving UP, so DOWN is a reversal
        check(s.peekNextHead().equals(new Point(10, 8)), "reversal ignored after a turn");

        s.grow(2);
        s.advance(s.peekNextHead());
        s.advance(s.peekNextHead());
        check(s.getLength() == 5, "grow(2) adds two cells over two moves");
        s.shrink(10);
        check(s.getLength() == Snake.MIN_LENGTH, "shrink never goes below minimum length");
        check(s.occupies(s.head().x, s.head().y), "occupies() true on the head");
        check(!s.occupies(-5, -5), "occupies() false elsewhere");
    }

    private static void testPowerUps() {
        System.out.println("Power-ups (polymorphism)");
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        try {
            Snake a = newSnake();
            Snake b = newSnake();
            long base = 140;

            PowerUp p = new SpeedBoost(0, 0, 0);
            p.apply(a, b, timer);
            check(a.hasEffect(EffectType.SPEED_BOOST) && !b.hasEffect(EffectType.SPEED_BOOST), "SpeedBoost affects collector only");
            check(a.moveDelay(base) < base, "boosted snake moves faster");

            p = new SlowDown(0, 0, 0);
            p.apply(a, b, timer);
            check(b.hasEffect(EffectType.SLOWED) && !a.hasEffect(EffectType.SLOWED), "SlowDown affects opponent only");
            check(b.moveDelay(base) > base, "slowed snake moves slower");

            p = new DoublePoints(0, 0, 0);
            p.apply(a, b, timer);
            check(a.hasEffect(EffectType.DOUBLE_POINTS), "DoublePoints affects collector");

            Snake c = newSnake();
            c.grow(4);
            for (int i = 0; i < 4; i++) {
                c.advance(c.peekNextHead());
            }
            int len = c.getLength();
            new Shrink(0, 0, 0).apply(c, a, timer);
            check(c.getLength() == len - 3 && c.getScore() == 5, "Shrink removes 3 segments and gives 5 points");
        } finally {
            timer.shutdownNow();
        }
    }

    private static void testEffectTimers() throws InterruptedException {
        System.out.println("Effect timers (separate thread, pause/resume)");
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        try {
            Snake s = newSnake();
            s.addEffect(EffectType.SPEED_BOOST, 300, timer);
            check(s.hasEffect(EffectType.SPEED_BOOST), "effect active right after start");
            Thread.sleep(500);
            check(!s.hasEffect(EffectType.SPEED_BOOST), "effect expires on its own");

            s.addEffect(EffectType.DOUBLE_POINTS, 600, timer);
            Thread.sleep(150);
            s.suspendEffects();
            Thread.sleep(800);                                      // longer than the effect duration
            check(s.hasEffect(EffectType.DOUBLE_POINTS), "effect does not expire while paused");
            long left = s.effectMsLeft(EffectType.DOUBLE_POINTS);
            check(left > 300 && left <= 500, "remaining time is kept while paused (was " + left + " ms)");
            s.resumeEffects(timer);
            Thread.sleep(700);
            check(!s.hasEffect(EffectType.DOUBLE_POINTS), "effect expires after resuming");

            s.addEffect(EffectType.SLOWED, 300, timer);
            Thread.sleep(150);
            s.addEffect(EffectType.SLOWED, 600, timer);              // restart before the first one ends
            Thread.sleep(300);                                       // the first timer would have fired by now
            check(s.hasEffect(EffectType.SLOWED), "restarting an effect cancels the old expiry");
        } finally {
            timer.shutdownNow();
        }
    }

    private static void testEngineCountdownAndSteering() {
        System.out.println("Engine: countdown and steering");
        GameEngine e = new GameEngine(config(Mode.CLASSIC, 3));
        check(e.getState() == GameEngine.State.COUNTDOWN, "starts in countdown");
        check(e.getCountdownSeconds() == 3, "countdown shows 3");
        e.update(1000);
        check(e.getCountdownSeconds() == 2, "countdown shows 2");
        e.update(1000);
        e.update(1001);
        check(e.getState() == GameEngine.State.RUNNING, "running after 3 seconds");

        e.togglePause();
        check(e.getState() == GameEngine.State.PAUSED, "pause works");
        long before = e.getElapsedMs();
        e.update(500);
        check(e.getElapsedMs() == before, "game clock is frozen while paused");
        e.togglePause();
        check(e.getState() == GameEngine.State.RUNNING, "resume works");
        e.shutdown();
    }

    private static void testWallCrashGivesWinner() {
        System.out.println("Engine: wall crash");
        GameEngine e = new GameEngine(config(Mode.CLASSIC, 5));
        e.update(3001);                                             // skip countdown
        e.steer(1, Direction.UP);                                   // player 1 runs into the top wall first
        int guard = 0;
        while (e.getState() == GameEngine.State.RUNNING && guard++ < 10000) {
            e.update(16);
        }
        check(e.getState() == GameEngine.State.OVER, "game ends");
        check(!e.getPlayer1().isAlive() && e.getPlayer2().isAlive(), "player 1 died, player 2 alive");
        check(e.getWinner() == e.getPlayer2(), "player 2 wins");
        check("hit the wall".equals(e.getPlayer1().getDeathReason()), "reason is 'hit the wall'");
        check(e.getResultTitle().equals("BOB WINS!"), "result title");
        e.shutdown();
    }

    private static void testValidator() {
        System.out.println("Input validation");
        try {
            check("Ram Kumar".equals(InputValidator.validateName("  Ram   Kumar ", "P")), "trims and collapses spaces");
        } catch (InvalidInputException ex) {
            check(false, "valid name rejected");
        }
        String[] bad = {"", "   ", "ThisNameIsWayTooLong", "no;semicolons", "<script>"};
        for (String b : bad) {
            try {
                InputValidator.validateName(b, "P");
                check(false, "should reject '" + b + "'");
            } catch (InvalidInputException ex) {
                check(true, "rejects bad name");
            }
        }
        try {
            InputValidator.requireDifferent("Sam", "sam");
            check(false, "same names (ignoring case) must be rejected");
        } catch (InvalidInputException ex) {
            check(true, "rejects identical names");
        }
    }

    private static void testSettings() throws Exception {
        System.out.println("Settings file");
        Path dir = Files.createTempDirectory("svs-settings");
        SettingsManager m = new SettingsManager(dir);
        GameConfig defaults = m.load();
        check(defaults.getPlayer1Name().equals("Player 1") && defaults.getSpeedLevel() == 3, "defaults on first run");
        m.save(new GameConfig("Zed", "Amy", Mode.CHAOS, 5, false));
        GameConfig loaded = m.load();
        check(loaded.getPlayer1Name().equals("Zed") && loaded.getPlayer2Name().equals("Amy")
                && loaded.getMode() == Mode.CHAOS && loaded.getSpeedLevel() == 5 && !loaded.isSoundOn(),
                "saved settings are loaded back");
        Files.write(dir.resolve("settings.properties"), "speed=banana\nmode=???\nplayer1=;;;\n".getBytes());
        GameConfig broken = m.load();
        check(broken.getSpeedLevel() == 3 && broken.getMode() == Mode.CLASSIC && broken.getPlayer1Name().equals("Player 1"),
                "damaged settings fall back to defaults");
    }

    private static void testJdbcIfAvailable() throws Exception {
        try {
            Path dir = Files.createTempDirectory("svs-db");
            ScoreStore jdbc = new JdbcScoreStore(dir.resolve("t.db"));
            testStore("JdbcScoreStore (SQLite)", jdbc);
        } catch (StorageException e) {
            System.out.println("JdbcScoreStore: SKIPPED (" + e.getMessage() + ")");
        }
    }

    private static void testStore(String name, ScoreStore store) throws Exception {
        System.out.println(name);
        check(store.topPlayers(10).isEmpty() && store.recentMatches(10).isEmpty(), "empty at the start");
        store.saveMatch(new MatchRecord("Ann", "Ben", 50, 20, "Ann", "Classic", "2026-11-02 10:00:00"));
        store.saveMatch(new MatchRecord("Ann", "Ben", 30, 40, "Ben", "Chaos", "2026-11-02 10:05:00"));
        store.saveMatch(new MatchRecord("ann", "Cy", 70, 10, "ann", "Classic", "2026-11-02 10:10:00"));
        store.saveMatch(new MatchRecord("Ben", "Cy", 10, 10, null, "Classic", "2026-11-02 10:15:00"));

        List<PlayerStat> top = store.topPlayers(10);
        check(top.size() == 3, "three distinct players (names are case-insensitive), got " + top.size());
        check(top.get(0).getName().equalsIgnoreCase("Ann") && top.get(0).getWins() == 2 && top.get(0).getGames() == 3,
                "Ann leads with 2 wins in 3 games");
        check(top.get(0).getBestScore() == 70, "best score is the highest single game");
        PlayerStat ben = null;
        for (PlayerStat p : top) {
            if (p.getName().equals("Ben")) {
                ben = p;
            }
        }
        check(ben != null && ben.getWins() == 1 && ben.getGames() == 3, "Ben: 1 win, 3 games (draw counts as a game, not a win)");

        List<MatchRecord> recent = store.recentMatches(2);
        check(recent.size() == 2 && recent.get(0).getPlayedAt().endsWith("10:15:00"), "recent matches are newest first and limited");
        check("Draw".equals(recent.get(0).getWinnerLabel()), "draw is labelled 'Draw'");
        check(store.topPlayers(1).size() == 1, "limit is respected");
        store.close();
    }

    // ------------------------------------------------------------------ fuzz test

    /** Plays many full games with random bots and checks the rules hold on every tick. */
    private static void fuzz() {
        System.out.println("Fuzz: bots playing full games in every mode");
        Random rnd = new Random(2026);
        int games = 0;
        int over = 0;
        int totalEats = 0;
        int totalPowerUps = 0;
        int invariantErrors = 0;
        int accountingErrors = 0;
        AtomicInteger gameOverCalls = new AtomicInteger();

        for (Mode mode : Mode.values()) {
            for (int g = 0; g < 100; g++) {
                games++;
                GameEngine e = new GameEngine(config(mode, 1 + rnd.nextInt(5)));
                int[] eats = new int[2];
                AtomicInteger powerUps = new AtomicInteger();
                AtomicInteger overForThisGame = new AtomicInteger();
                e.setListener(new GameListener() {
                    @Override public void onFoodEaten(Snake s) { eats[s == e.getPlayer1() ? 0 : 1]++; }
                    @Override public void onPowerUpCollected(Snake s, PowerUp p) { powerUps.incrementAndGet(); }
                    @Override public void onGameOver(GameEngine engine) { overForThisGame.incrementAndGet(); gameOverCalls.incrementAndGet(); }
                });

                int ticks = 0;
                while (e.getState() != GameEngine.State.OVER && ticks++ < 12000) {
                    bot(e, e.getPlayer1(), 1, rnd);
                    bot(e, e.getPlayer2(), 2, rnd);
                    e.update(16);
                    if (!boardIsConsistent(e)) {
                        invariantErrors++;
                        break;
                    }
                }
                if (e.getState() == GameEngine.State.OVER) {
                    over++;
                    if (overForThisGame.get() != 1) {
                        invariantErrors++;
                    }
                    boolean someoneDead = !e.getPlayer1().isAlive() || !e.getPlayer2().isAlive();
                    boolean resultOk = e.isDraw() ? e.getWinner() == null : e.getWinner() != null;
                    if (!someoneDead || !resultOk) {
                        invariantErrors++;
                    }
                }
                totalEats += eats[0] + eats[1];
                totalPowerUps += powerUps.get();

                if (!mode.hasPowerUps()) {   // growth and score bookkeeping must match exactly
                    Snake[] ps = {e.getPlayer1(), e.getPlayer2()};
                    for (int i = 0; i < 2; i++) {
                        boolean lengthOk = ps[i].getLength() + ps[i].getPendingGrowth() == 3 + eats[i];
                        boolean scoreOk = ps[i].getScore() == 10 * eats[i];
                        if (!lengthOk || !scoreOk) {
                            accountingErrors++;
                        }
                    }
                }
                e.shutdown();
            }
        }
        System.out.println("  games=" + games + " finished=" + over + " foodEaten=" + totalEats
                + " powerUpsCollected=" + totalPowerUps);
        check(invariantErrors == 0, "no overlapping/out-of-board snakes and exactly one game-over per game (errors: " + invariantErrors + ")");
        check(accountingErrors == 0, "length and score match food eaten (errors: " + accountingErrors + ")");
        check(over > games * 0.9, "most games finish within the tick budget (" + over + "/" + games + ")");
        check(totalEats > 200, "bots actually ate food (" + totalEats + ")");
        check(gameOverCalls.get() == over, "onGameOver fired once per finished game");
    }

    private static void bot(GameEngine e, Snake s, int player, Random rnd) {
        if (!s.isAlive()) {
            return;
        }
        Point h = s.head();
        Direction current = s.getDirection();
        Set<Integer> occupied = new HashSet<>();
        for (Snake t : new Snake[]{e.getPlayer1(), e.getPlayer2()}) {
            for (Point p : t.cells()) {
                occupied.add(p.y * GameEngine.COLS + p.x);
            }
        }
        List<Direction> safe = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (d == current.opposite()) {
                continue;
            }
            int nx = h.x + d.dx;
            int ny = h.y + d.dy;
            if (nx < 0 || ny < 0 || nx >= GameEngine.COLS || ny >= GameEngine.ROWS) {
                continue;
            }
            if (occupied.contains(ny * GameEngine.COLS + nx)) {
                continue;
            }
            safe.add(d);
        }
        Direction pick = current;
        if (!safe.isEmpty()) {
            pick = (safe.contains(current) && rnd.nextInt(100) >= 8) ? current : safe.get(rnd.nextInt(safe.size()));
        }
        e.steer(player, pick);
    }

    private static boolean boardIsConsistent(GameEngine e) {
        Set<Integer> seen = new HashSet<>();
        for (Snake s : new Snake[]{e.getPlayer1(), e.getPlayer2()}) {
            for (Point p : s.cells()) {
                if (p.x < 0 || p.y < 0 || p.x >= GameEngine.COLS || p.y >= GameEngine.ROWS) {
                    return false;
                }
                if (!seen.add(p.y * GameEngine.COLS + p.x)) {
                    return false;                                   // two cells on top of each other
                }
            }
        }
        return true;
    }
}
