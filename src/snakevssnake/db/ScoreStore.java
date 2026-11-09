package snakevssnake.db;

import java.util.List;

/**
 * Where match results are kept. Two implementations exist (interface + polymorphism):
 * {@link JdbcScoreStore} (SQLite through JDBC) and {@link FileScoreStore} (plain text file,
 * used automatically when no JDBC driver is available).
 */
public interface ScoreStore extends AutoCloseable {

    void saveMatch(MatchRecord match) throws StorageException;

    /** Best players first: most wins, then best single-game score. */
    List<PlayerStat> topPlayers(int limit) throws StorageException;

    /** Newest match first. */
    List<MatchRecord> recentMatches(int limit) throws StorageException;

    /** Human readable name of the storage, shown in the UI. */
    String backendName();

    @Override
    void close();
}
