package snakevssnake.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Sound effects generated in code (no audio files needed) with javax.sound.sampled.
 * Playing happens on its own thread. If the computer has no audio device the player simply
 * switches itself off instead of crashing the game.
 */
public class SoundPlayer {

    public enum Tone { EAT, POWER_UP, CRASH, GO }

    private static final float SAMPLE_RATE = 22050f;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sound");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean enabled = true;
    private volatile boolean unavailable = false;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void play(Tone tone) {
        if (!enabled || unavailable) {
            return;
        }
        try {
            worker.submit(() -> render(tone));
        } catch (RejectedExecutionException e) {
            // player already shut down
        }
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    private void render(Tone tone) {
        byte[] data = synthesize(tone);
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException | RuntimeException e) {
            unavailable = true;                       // no sound device: stay silent from now on
        }
    }

    private byte[] synthesize(Tone tone) {
        switch (tone) {
            case EAT:      return tone(700, 1000, 70, 0.35);
            case POWER_UP: return concat(tone(523, 523, 70, 0.35), tone(659, 659, 70, 0.35), tone(784, 784, 120, 0.35));
            case CRASH:    return concat(tone(260, 70, 450, 0.55));
            default:       return tone(880, 880, 160, 0.30);
        }
    }

    /** A sine wave sliding from f0 to f1 Hz, fading out. */
    private static byte[] tone(double f0, double f1, int ms, double volume) {
        int n = (int) (SAMPLE_RATE * ms / 1000);
        byte[] out = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            phase += 2 * Math.PI * (f0 + (f1 - f0) * t) / SAMPLE_RATE;
            double envelope = Math.min(1.0, i / 200.0) * (1 - t);
            short s = (short) (Math.sin(phase) * volume * envelope * Short.MAX_VALUE);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            total += p.length;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
