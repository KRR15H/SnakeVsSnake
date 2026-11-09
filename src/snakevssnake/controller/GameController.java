package snakevssnake.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import snakevssnake.db.MatchRecord;
import snakevssnake.db.ScoreStore;
import snakevssnake.db.StorageException;
import snakevssnake.model.Direction;
import snakevssnake.model.GameConfig;
import snakevssnake.model.PowerUp;
import snakevssnake.model.Snake;
import snakevssnake.util.SoundPlayer;

/**
 * Glue between the engine, the game-loop thread, sound and the database.
 * The view talks only to this class and to the engine's read-only getters.
 */
public class GameController implements GameListener {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GameEngine engine;
    private final GameLoop loop;
    private final Thread loopThread;
    private final ScoreStore store;
    private final SoundPlayer sound;
    private volatile String saveStatus = "";

    public GameController(GameConfig config, ScoreStore store, SoundPlayer sound) {
        this.engine = new GameEngine(config);
        this.engine.setListener(this);
        this.store = store;
        this.sound = sound;
        this.loop = new GameLoop(engine);
        this.loopThread = new Thread(loop, "game-loop");
        this.loopThread.setDaemon(true);
    }

    public GameEngine getEngine() {
        return engine;
    }

    public String getSaveStatus() {
        return saveStatus;
    }

    public void setFrameCallback(Runnable callback) {
        loop.setFrameCallback(callback);
    }

    public void start() {
        loopThread.start();
    }

    public void stop() {
        loop.stop();
        engine.shutdown();
    }

    public void steer(int player, Direction d) {
        engine.steer(player, d);
    }

    public void togglePause() {
        engine.togglePause();
    }

    public void pause() {
        engine.pause();
    }

    public boolean isOver() {
        return engine.getState() == GameEngine.State.OVER;
    }

    // ------------------------------------------------------------------ GameListener

    @Override
    public void onGameStart() {
        sound.play(SoundPlayer.Tone.GO);
    }

    @Override
    public void onFoodEaten(Snake snake) {
        sound.play(SoundPlayer.Tone.EAT);
    }

    @Override
    public void onPowerUpCollected(Snake snake, PowerUp powerUp) {
        sound.play(SoundPlayer.Tone.POWER_UP);
    }

    @Override
    public void onCrash(Snake snake) {
        sound.play(SoundPlayer.Tone.CRASH);
    }

    /** Saves the result on a separate thread so a slow disk or database never freezes the game. */
    @Override
    public void onGameOver(GameEngine e) {
        Snake winner = e.getWinner();
        final MatchRecord record = new MatchRecord(
                e.getPlayer1().getName(), e.getPlayer2().getName(),
                e.getPlayer1().getScore(), e.getPlayer2().getScore(),
                winner == null ? null : winner.getName(),
                e.getConfig().getMode().toString(),
                LocalDateTime.now().format(STAMP));
        saveStatus = "Saving result...";
        Thread saver = new Thread(() -> {
            try {
                store.saveMatch(record);
                saveStatus = "Result saved (" + store.backendName() + ")";
            } catch (StorageException ex) {
                saveStatus = "Could not save result: " + ex.getMessage();
            }
        }, "result-saver");
        saver.setDaemon(true);
        saver.start();
    }
}
