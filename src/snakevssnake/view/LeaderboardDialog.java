package snakevssnake.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import snakevssnake.db.MatchRecord;
import snakevssnake.db.PlayerStat;
import snakevssnake.db.ScoreStore;
import snakevssnake.db.StorageException;

/** Shows the top players and the most recent matches. Data is loaded off the Swing thread. */
public class LeaderboardDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final DefaultTableModel playersModel = model("#", "Player", "Wins", "Games", "Win %", "Best score");
    private final DefaultTableModel matchesModel =
            model("Played", "Player 1", "Score", "Player 2", "Score", "Winner", "Mode");
    private final JLabel status = new JLabel("Loading...", SwingConstants.CENTER);

    private static class Data {
        List<PlayerStat> players;
        List<MatchRecord> matches;
    }

    public LeaderboardDialog(Frame owner, ScoreStore store) {
        super(owner, "Leaderboard", true);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.BG_TOP);
        root.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("LEADERBOARD", SwingConstants.CENTER);
        title.setFont(Theme.bold(26));
        title.setForeground(Theme.GOLD);
        root.add(title, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 12));
        tables.setOpaque(false);
        tables.add(section("Top players", playersModel, new int[]{0, 2, 3, 4, 5}));
        tables.add(section("Recent matches", matchesModel, new int[]{2, 4}));
        root.add(tables, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        status.setForeground(Theme.MUTED);
        status.setFont(Theme.plain(12));
        StyledButton close = new StyledButton("Close", new Color(70, 86, 160));
        close.addActionListener(e -> dispose());
        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(close);
        south.add(status, BorderLayout.NORTH);
        south.add(buttonRow, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        setPreferredSize(new Dimension(780, 600));
        pack();
        setLocationRelativeTo(owner);
        load(store);
    }

    private void load(ScoreStore store) {
        status.setText("Loading from " + store.backendName() + "...");
        new SwingWorker<Data, Void>() {
            @Override
            protected Data doInBackground() throws StorageException {
                Data d = new Data();
                d.players = store.topPlayers(15);
                d.matches = store.recentMatches(15);
                return d;
            }

            @Override
            protected void done() {
                try {
                    Data d = get();
                    int rank = 1;
                    for (PlayerStat p : d.players) {
                        playersModel.addRow(new Object[]{rank++, p.getName(), p.getWins(), p.getGames(),
                                p.getWinPercent() + "%", p.getBestScore()});
                    }
                    for (MatchRecord m : d.matches) {
                        matchesModel.addRow(new Object[]{m.getPlayedAt(), m.getPlayer1(), m.getScore1(),
                                m.getPlayer2(), m.getScore2(), m.getWinnerLabel(), m.getMode()});
                    }
                    status.setText(d.players.isEmpty() ? "No games played yet. Play a match first!"
                            : "Source: " + store.backendName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    status.setForeground(Theme.DANGER);
                    status.setText("Could not load scores: " + (cause == null ? e.getMessage() : cause.getMessage()));
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------ table styling

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel section(String heading, DefaultTableModel model, int[] centeredColumns) {
        JTable table = new JTable(model);
        table.setRowHeight(27);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.TEXT);
        table.setFont(Theme.plain(14));
        table.setGridColor(new Color(44, 52, 88));
        table.setSelectionBackground(new Color(60, 72, 120));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(new Color(40, 48, 86));
        table.getTableHeader().setForeground(Theme.GOLD);
        table.getTableHeader().setFont(Theme.bold(13));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int c : centeredColumns) {
            table.getColumnModel().getColumn(c).setCellRenderer(center);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(60, 70, 112)));

        JLabel label = new JLabel(heading);
        label.setForeground(Theme.TEXT);
        label.setFont(Theme.bold(15));
        label.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(label, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }
}
