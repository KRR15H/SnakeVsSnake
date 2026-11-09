package snakevssnake.controller;

/**
 * The game-loop thread body (Runnable). It runs in the background so the Swing window never
 * freezes: update the engine, ask the view to repaint, sleep until the next frame.
 */
public class GameLoop implements Runnable {

    private static final long FRAME_MS = 16;          // about 60 frames per second
    private static final long MAX_STEP_MS = 100;      // never simulate a huge jump (e.g. after a lag spike)

    private final GameEngine engine;
    private volatile Runnable frameCallback = () -> { };
    private volatile boolean running = true;

    public GameLoop(GameEngine engine) {
        this.engine = engine;
    }

    public void setFrameCallback(Runnable callback) {
        this.frameCallback = callback == null ? () -> { } : callback;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            long dt = Math.min(MAX_STEP_MS, (frameStart - last) / 1_000_000L);
            last = frameStart;

            engine.update(dt);
            frameCallback.run();

            long spent = (System.nanoTime() - frameStart) / 1_000_000L;
            try {
                Thread.sleep(Math.max(1, FRAME_MS - spent));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
