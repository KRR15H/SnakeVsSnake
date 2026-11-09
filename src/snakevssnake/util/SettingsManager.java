package snakevssnake.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import snakevssnake.model.GameConfig;
import snakevssnake.model.Mode;

/** Saves and loads the menu choices (names, mode, speed, sound) with file I/O streams. */
public class SettingsManager {

    private final Path file;

    public SettingsManager(Path dataDir) {
        this.file = dataDir.resolve("settings.properties");
    }

    /** Loads the saved settings; any missing or damaged value falls back to a default. */
    public GameConfig load() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file.toFile())) {
            props.load(in);
        } catch (FileNotFoundException e) {
            // first run: no settings file yet, defaults are used
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("[settings] could not read settings, using defaults: " + e.getMessage());
        }

        String p1 = safeName(props.getProperty("player1"), "Player 1");
        String p2 = safeName(props.getProperty("player2"), "Player 2");
        if (p1.equalsIgnoreCase(p2)) {
            p1 = "Player 1";
            p2 = "Player 2";
        }
        Mode mode = Mode.fromName(props.getProperty("mode"));
        int speed = 3;
        try {
            speed = Integer.parseInt(props.getProperty("speed", "3").trim());
        } catch (NumberFormatException e) {
            // keep default
        }
        boolean sound = Boolean.parseBoolean(props.getProperty("sound", "true"));
        return new GameConfig(p1, p2, mode, speed, sound);
    }

    public void save(GameConfig config) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Properties props = new Properties();
        props.setProperty("player1", config.getPlayer1Name());
        props.setProperty("player2", config.getPlayer2Name());
        props.setProperty("mode", config.getMode().name());
        props.setProperty("speed", String.valueOf(config.getSpeedLevel()));
        props.setProperty("sound", String.valueOf(config.isSoundOn()));
        try (OutputStream out = new FileOutputStream(file.toFile())) {
            props.store(out, "Snake Vs Snake settings");
        }
    }

    private static String safeName(String raw, String fallback) {
        try {
            return InputValidator.validateName(raw, "Player");
        } catch (InvalidInputException e) {
            return fallback;
        }
    }
}
