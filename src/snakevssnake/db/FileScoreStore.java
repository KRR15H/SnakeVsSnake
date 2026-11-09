package snakevssnake.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fallback storage: one tab-separated line per match in a text file.
 * Leaderboard totals are calculated from the match lines when requested.
 */
public class FileScoreStore implements ScoreStore {

    private final Path file;

    public FileScoreStore(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void saveMatch(MatchRecord m) throws StorageException {
        String line = String.join("\t",
                clean(m.getPlayedAt()), clean(m.getPlayer1()), clean(m.getPlayer2()),
                String.valueOf(m.getScore1()), String.valueOf(m.getScore2()),
                clean(m.getWinner() == null ? "" : m.getWinner()), clean(m.getMode()));
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, Collections.singletonList(line), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new StorageException("Could not write " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized List<PlayerStat> topPlayers(int limit) throws StorageException {
        // lower-case name -> {games, wins, best}; first spelling of a name is kept for display
        Map<String, int[]> totals = new LinkedHashMap<>();
        Map<String, String> display = new LinkedHashMap<>();
        for (MatchRecord m : readAll()) {
            add(totals, display, m.getPlayer1(), m.getScore1(), m.isWinner(m.getPlayer1()));
            add(totals, display, m.getPlayer2(), m.getScore2(), m.isWinner(m.getPlayer2()));
        }
        List<PlayerStat> list = new ArrayList<>();
        for (Map.Entry<String, int[]> e : totals.entrySet()) {
            int[] t = e.getValue();
            list.add(new PlayerStat(display.get(e.getKey()), t[0], t[1], t[2]));
        }
        list.sort(Comparator.comparingInt(PlayerStat::getWins).reversed()
                .thenComparing(Comparator.comparingInt(PlayerStat::getBestScore).reversed())
                .thenComparing(PlayerStat::getName, String.CASE_INSENSITIVE_ORDER));
        return list.size() > limit ? new ArrayList<>(list.subList(0, limit)) : list;
    }

    private static void add(Map<String, int[]> totals, Map<String, String> display,
                            String name, int score, boolean won) {
        String key = name.toLowerCase();
        display.putIfAbsent(key, name);
        int[] t = totals.computeIfAbsent(key, k -> new int[3]);
        t[0]++;
        if (won) {
            t[1]++;
        }
        t[2] = Math.max(t[2], score);
    }

    @Override
    public synchronized List<MatchRecord> recentMatches(int limit) throws StorageException {
        List<MatchRecord> all = readAll();
        Collections.reverse(all);
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }

    private List<MatchRecord> readAll() throws StorageException {
        List<MatchRecord> list = new ArrayList<>();
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return list;                                   // no games played yet
        } catch (IOException e) {
            throw new StorageException("Could not read " + file + ": " + e.getMessage(), e);
        }
        for (String line : lines) {
            String[] f = line.split("\t", -1);
            if (f.length != 7) {
                continue;                                  // skip damaged lines instead of crashing
            }
            try {
                list.add(new MatchRecord(f[1], f[2], Integer.parseInt(f[3]), Integer.parseInt(f[4]),
                        f[5], f[6], f[0]));
            } catch (NumberFormatException e) {
                // skip damaged line
            }
        }
        return list;
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    @Override
    public String backendName() {
        return "text file (add sqlite-jdbc jar to lib/ for JDBC)";
    }

    @Override
    public void close() {
        // nothing to release
    }
}
