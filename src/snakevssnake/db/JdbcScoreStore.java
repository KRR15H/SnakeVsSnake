package snakevssnake.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation backed by an SQLite database file.
 *
 * Needs the sqlite-jdbc driver jar on the classpath (put it in the lib/ folder).
 * Tables:
 *   players(name, games, wins, best_score)
 *   matches(id, player1, player2, score1, score2, winner, mode, played_at)
 */
public class JdbcScoreStore implements ScoreStore {

    private final Connection connection;

    public JdbcScoreStore(Path dbFile) throws StorageException {
        Connection c;
        try {
            Files.createDirectories(dbFile.toAbsolutePath().getParent());
            Class.forName("org.sqlite.JDBC");                       // load the driver
            c = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
            createSchema(c);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new StorageException("SQLite JDBC driver not found on the classpath", e);
        } catch (SQLException | IOException e) {
            throw new StorageException("Could not open the database: " + e.getMessage(), e);
        }
        this.connection = c;
    }

    private static void createSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS players ("
                    + "name TEXT PRIMARY KEY COLLATE NOCASE, "
                    + "games INTEGER NOT NULL DEFAULT 0, "
                    + "wins INTEGER NOT NULL DEFAULT 0, "
                    + "best_score INTEGER NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS matches ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "player1 TEXT NOT NULL, player2 TEXT NOT NULL, "
                    + "score1 INTEGER NOT NULL, score2 INTEGER NOT NULL, "
                    + "winner TEXT, mode TEXT NOT NULL, played_at TEXT NOT NULL)");
        }
    }

    /** Inserts the match and updates both players' totals inside one transaction. */
    @Override
    public synchronized void saveMatch(MatchRecord m) throws StorageException {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO matches(player1, player2, score1, score2, winner, mode, played_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, m.getPlayer1());
                insert.setString(2, m.getPlayer2());
                insert.setInt(3, m.getScore1());
                insert.setInt(4, m.getScore2());
                insert.setString(5, m.getWinner());
                insert.setString(6, m.getMode());
                insert.setString(7, m.getPlayedAt());
                insert.executeUpdate();
            }
            updatePlayer(m.getPlayer1(), m.getScore1(), m.isWinner(m.getPlayer1()));
            updatePlayer(m.getPlayer2(), m.getScore2(), m.isWinner(m.getPlayer2()));
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do; the original error is reported below
            }
            throw new StorageException("Database error: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // connection is probably closed already
            }
        }
    }

    private void updatePlayer(String name, int score, boolean won) throws SQLException {
        try (PreparedStatement create = connection.prepareStatement(
                "INSERT OR IGNORE INTO players(name) VALUES (?)");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE players SET games = games + 1, wins = wins + ?, "
                             + "best_score = MAX(best_score, ?) WHERE name = ?")) {
            create.setString(1, name);
            create.executeUpdate();
            update.setInt(1, won ? 1 : 0);
            update.setInt(2, score);
            update.setString(3, name);
            update.executeUpdate();
        }
    }

    @Override
    public synchronized List<PlayerStat> topPlayers(int limit) throws StorageException {
        List<PlayerStat> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, games, wins, best_score FROM players "
                        + "ORDER BY wins DESC, best_score DESC, name LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PlayerStat(rs.getString("name"), rs.getInt("games"),
                            rs.getInt("wins"), rs.getInt("best_score")));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read leaderboard: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public synchronized List<MatchRecord> recentMatches(int limit) throws StorageException {
        List<MatchRecord> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player1, player2, score1, score2, winner, mode, played_at "
                        + "FROM matches ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MatchRecord(rs.getString("player1"), rs.getString("player2"),
                            rs.getInt("score1"), rs.getInt("score2"), rs.getString("winner"),
                            rs.getString("mode"), rs.getString("played_at")));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read match history: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public String backendName() {
        return "SQLite database via JDBC";
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // closing on exit; nothing useful to do
        }
    }
}
