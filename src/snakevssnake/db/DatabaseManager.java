package snakevssnake.db;

import java.nio.file.Path;

/** Chooses the best available storage: SQLite via JDBC if possible, otherwise a plain file. */
public final class DatabaseManager {

    private DatabaseManager() { }

    public static ScoreStore open(Path dataDir) {
        try {
            return new JdbcScoreStore(dataDir.resolve("snake.db"));
        } catch (StorageException e) {
            System.err.println("[storage] " + e.getMessage() + " - using text file storage instead.");
            return new FileScoreStore(dataDir.resolve("matches.tsv"));
        }
    }
}
