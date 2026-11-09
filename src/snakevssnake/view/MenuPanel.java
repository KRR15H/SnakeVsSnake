package snakevssnake.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.io.IOException;
import java.util.Hashtable;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import snakevssnake.model.GameConfig;
import snakevssnake.model.Mode;
import snakevssnake.util.InputValidator;
import snakevssnake.util.InvalidInputException;
import snakevssnake.util.SettingsManager;

/** Main menu: player names, mode, speed, sound and navigation buttons. */
public class MenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField player1Field;
    private final JTextField player2Field;
    private final JComboBox<Mode> modeBox;
    private final JSlider speedSlider;
    private final JCheckBox soundBox;
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final SettingsManager settings;
    private final Consumer<GameConfig> onStart;

    public MenuPanel(GameConfig initial, String storageText, SettingsManager settings,
                     Consumer<GameConfig> onStart, Runnable onLeaderboard, Runnable onHelp, Runnable onQuit) {
        super(new BorderLayout());
        this.settings = settings;
        this.onStart = onStart;
        setPreferredSize(new Dimension(920, 680));

        player1Field = styledField(initial.getPlayer1Name(), Theme.P1);
        player2Field = styledField(initial.getPlayer2Name(), Theme.P2);

        modeBox = new JComboBox<>(Mode.values());
        modeBox.setSelectedItem(initial.getMode());
        modeBox.setFont(Theme.plain(15));
        modeBox.setBackground(new Color(14, 18, 36));
        modeBox.setForeground(Theme.TEXT);

        speedSlider = new JSlider(GameConfig.MIN_SPEED, GameConfig.MAX_SPEED, initial.getSpeedLevel());
        speedSlider.setOpaque(false);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setForeground(Theme.MUTED);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        String[] names = {"Chill", "Easy", "Normal", "Fast", "Insane"};
        for (int i = 0; i < names.length; i++) {
            JLabel l = new JLabel(names[i]);
            l.setForeground(Theme.MUTED);
            l.setFont(Theme.plain(11));
            labels.put(i + 1, l);
        }
        speedSlider.setLabelTable(labels);

        soundBox = new JCheckBox("Sound effects", initial.isSoundOn());
        soundBox.setOpaque(false);
        soundBox.setForeground(Theme.TEXT);
        soundBox.setFont(Theme.plain(14));
        soundBox.setFocusPainted(false);

        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setFont(Theme.bold(13));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(190, 20, 10, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        gc.weighty = 1;
        center.add(buildCard(onLeaderboard, onHelp, onQuit), gc);
        add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("Score storage: " + storageText + "   |   Team Unreal  -  Advanced Java Programming (CSE 213)",
                SwingConstants.CENTER);
        footer.setForeground(Theme.MUTED);
        footer.setFont(Theme.plain(12));
        footer.setBorder(new EmptyBorder(0, 0, 14, 0));
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildCard(Runnable onLeaderboard, Runnable onHelp, Runnable onQuit) {
        JPanel card = new JPanel(new GridBagLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                // fixed width, natural height (a fixed height of 0 would collapse the card)
                return new Dimension(460, super.getPreferredSize().height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.antialias(g2);
                g2.setColor(new Color(34, 41, 74, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(110, 122, 180, 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 28, 20, 28));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 0, 3, 0);

        int row = 0;
        c.gridy = row++;
        card.add(caption("PLAYER 1   (W A S D)", Theme.P1), c);
        c.gridy = row++;
        card.add(player1Field, c);
        c.gridy = row++;
        card.add(caption("PLAYER 2   (ARROW KEYS)", Theme.P2), c);
        c.gridy = row++;
        card.add(player2Field, c);
        c.gridy = row++;
        card.add(caption("GAME MODE", Theme.GOLD), c);
        c.gridy = row++;
        card.add(modeBox, c);
        c.gridy = row++;
        card.add(caption("SPEED", Theme.GOLD), c);
        c.gridy = row++;
        card.add(speedSlider, c);
        c.gridy = row++;
        card.add(soundBox, c);
        c.gridy = row++;
        card.add(messageLabel, c);

        StyledButton start = new StyledButton("START GAME", new Color(46, 170, 96));
        start.setFont(Theme.bold(18));
        start.addActionListener(e -> startPressed());
        c.gridy = row++;
        c.insets = new Insets(6, 0, 8, 0);
        card.add(start, c);

        JPanel small = new JPanel(new GridLayout(1, 3, 10, 0));
        small.setOpaque(false);
        StyledButton board = new StyledButton("Leaderboard", new Color(70, 86, 160));
        StyledButton help = new StyledButton("How to Play", new Color(70, 86, 160));
        StyledButton quit = new StyledButton("Quit", new Color(150, 62, 78));
        board.addActionListener(e -> onLeaderboard.run());
        help.addActionListener(e -> onHelp.run());
        quit.addActionListener(e -> onQuit.run());
        small.add(board);
        small.add(help);
        small.add(quit);
        c.gridy = row;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(small, c);
        return card;
    }

    private void startPressed() {
        try {
            String n1 = InputValidator.validateName(player1Field.getText(), "Player 1");
            String n2 = InputValidator.validateName(player2Field.getText(), "Player 2");
            InputValidator.requireDifferent(n1, n2);
            GameConfig config = new GameConfig(n1, n2, (Mode) modeBox.getSelectedItem(),
                    speedSlider.getValue(), soundBox.isSelected());
            try {
                settings.save(config);
            } catch (IOException e) {
                System.err.println("[settings] could not save settings: " + e.getMessage());
            }
            messageLabel.setText(" ");
            onStart.accept(config);
        } catch (InvalidInputException e) {
            messageLabel.setText(e.getMessage());
        }
    }

    private JTextField styledField(String text, Color accent) {
        JTextField f = new JTextField(text);
        f.setFont(Theme.bold(16));
        f.setBackground(new Color(14, 18, 36));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2, true), new EmptyBorder(7, 12, 7, 12)));
        f.addActionListener(e -> startPressed());
        return f;
    }

    private JLabel caption(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(Theme.bold(12));
        return l;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);
        Theme.paintBackground(g2, getWidth(), getHeight());

        int cx = getWidth() / 2;

        // title: SNAKE (player 1 green)  VS (gold)  SNAKE (player 2 orange)
        g2.setFont(Theme.bold(62));
        FontMetrics big = g2.getFontMetrics();
        int wSnake = big.stringWidth("SNAKE");
        g2.setFont(Theme.bold(30));
        int wVs = g2.getFontMetrics().stringWidth("VS");
        int gap = 22;
        int total = wSnake * 2 + wVs + gap * 2;
        int x = cx - total / 2;
        int base = 98;

        g2.setFont(Theme.bold(62));
        g2.setColor(new Color(0, 0, 0, 110));
        g2.drawString("SNAKE", x + 3, base + 3);
        g2.setColor(Theme.P1);
        g2.drawString("SNAKE", x, base);

        g2.setFont(Theme.bold(30));
        g2.setColor(Theme.GOLD);
        g2.drawString("VS", x + wSnake + gap, base - 14);

        g2.setFont(Theme.bold(62));
        g2.setColor(new Color(0, 0, 0, 110));
        g2.drawString("SNAKE", x + wSnake + gap * 2 + wVs + 3, base + 3);
        g2.setColor(Theme.P2);
        g2.drawString("SNAKE", x + wSnake + gap * 2 + wVs, base);

        g2.setFont(Theme.plain(16));
        g2.setColor(Theme.MUTED);
        Theme.drawCentered(g2, "Two players. One keyboard. One winner.", cx, base + 30);

        // two little snakes about to meet around an apple
        drawMiniSnake(g2, cx - 40, base + 62, -1, Theme.P1, new Color(20, 120, 90));
        drawMiniSnake(g2, cx + 40, base + 62, 1, Theme.P2, new Color(175, 55, 70));
        g2.setColor(new Color(235, 55, 75));
        g2.fillOval(cx - 8, base + 54, 16, 16);
        g2.setColor(new Color(90, 190, 90));
        g2.drawLine(cx, base + 54, cx + 3, base + 49);
        g2.dispose();
    }

    /** Draws a small decorative snake that grows away from (x, y); dir = -1 grows to the left. */
    private void drawMiniSnake(Graphics2D g2, int x, int y, int dir, Color head, Color tail) {
        int n = 9;
        int size = 15;
        for (int i = n - 1; i >= 0; i--) {
            float t = (float) i / (n - 1);
            int r = Math.round(head.getRed() + (tail.getRed() - head.getRed()) * t);
            int gr = Math.round(head.getGreen() + (tail.getGreen() - head.getGreen()) * t);
            int b = Math.round(head.getBlue() + (tail.getBlue() - head.getBlue()) * t);
            g2.setColor(new Color(r, gr, b));
            int pad = Math.round(t * 3);
            int px = x + dir * (i * 14 + 4) - (dir > 0 ? 0 : size);
            g2.fillRoundRect(px + pad, y - size / 2 + pad, size - 2 * pad, size - 2 * pad, 8, 8);
        }
        // eyes on the head, looking toward the apple
        int hx = x + dir * 4 - (dir > 0 ? 0 : size);
        int ex = hx + (dir > 0 ? 3 : size - 7);
        g2.setColor(Color.WHITE);
        g2.fillOval(ex, y - 5, 5, 5);
        g2.fillOval(ex, y + 1, 5, 5);
        g2.setColor(new Color(20, 22, 34));
        g2.fillOval(ex + (dir < 0 ? 0 : 2), y - 4, 3, 3);
        g2.fillOval(ex + (dir < 0 ? 0 : 2), y + 2, 3, 3);
    }
}
