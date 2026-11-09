package snakevssnake.view;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import snakevssnake.controller.GameController;
import snakevssnake.controller.GameEngine;
import snakevssnake.controller.InputHandler;
import snakevssnake.model.EffectType;
import snakevssnake.model.Snake;

/** The playing screen: score cards on top, the board in the middle, buttons at the bottom. */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int HUD_HEIGHT = 96;

    private final GameController controller;
    private final GameEngine engine;
    private final BoardView board = new BoardView();
    private final StyledButton pauseButton = new StyledButton("Pause", new Color(70, 86, 160));
    private volatile String pauseText = "Pause";

    public GamePanel(GameController controller, Runnable onMenu, Runnable onRematch) {
        super(new BorderLayout());
        this.controller = controller;
        this.engine = controller.getEngine();

        board.setFocusable(true);
        board.addKeyListener(new InputHandler(controller, onMenu, onRematch));
        board.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                controller.pause();                 // auto-pause when the window loses focus
            }
        });
        add(board, BorderLayout.CENTER);

        StyledButton restart = new StyledButton("Restart", new Color(70, 86, 160));
        StyledButton menu = new StyledButton("Menu", new Color(150, 62, 78));
        pauseButton.addActionListener(e -> {
            controller.togglePause();
            board.requestFocusInWindow();
        });
        restart.addActionListener(e -> onRematch.run());
        menu.addActionListener(e -> onMenu.run());
        // buttons must not steal keyboard focus from the board
        pauseButton.setFocusable(false);
        restart.setFocusable(false);
        menu.setFocusable(false);

        JLabel hint = new JLabel("P1: W A S D     P2: Arrow keys     P / Space: pause     R: rematch     Esc: menu");
        hint.setForeground(Theme.MUTED);
        hint.setFont(Theme.plain(12));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        bar.setBackground(new Color(18, 20, 40));
        bar.setBorder(new EmptyBorder(0, 0, 2, 0));
        bar.add(pauseButton);
        bar.add(restart);
        bar.add(menu);
        bar.add(hint);
        add(bar, BorderLayout.SOUTH);

        // the game-loop thread calls this about 60 times per second
        controller.setFrameCallback(() -> {
            board.repaint();
            String wanted = engine.getState() == GameEngine.State.PAUSED ? "Resume" : "Pause";
            if (!wanted.equals(pauseText)) {
                pauseText = wanted;
                SwingUtilities.invokeLater(() -> pauseButton.setText(wanted));
            }
        });
    }

    /** Called after the panel is shown so key presses reach the board. */
    public void activate() {
        board.requestFocusInWindow();
    }

    /** Paints one frame to the given graphics (also used by the headless screenshot test). */
    public void paintBoard(Graphics g) {
        board.paintComponent(g);
    }

    public void setBoardSize(int w, int h) {
        board.setSize(w, h);
    }

    // =========================================================================================

    private class BoardView extends JPanel {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.antialias(g);
            int w = getWidth();
            int h = getHeight();
            Theme.paintBackground(g, w, h);

            int margin = 14;
            int availW = w - 2 * margin;
            int availH = h - HUD_HEIGHT - margin;
            int cell = Math.max(8, Math.min(availW / GameEngine.COLS, availH / GameEngine.ROWS));
            int bw = cell * GameEngine.COLS;
            int bh = cell * GameEngine.ROWS;
            int bx = (w - bw) / 2;
            int by = HUD_HEIGHT + Math.max(0, (availH - bh) / 2);

            drawHud(g, bx, 8, bw);

            // frame and soft shadow
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRoundRect(bx - 4, by - 1, bw + 8, bh + 10, 18, 18);
            g.setColor(Theme.BOARD_BORDER);
            g.fillRoundRect(bx - 5, by - 5, bw + 10, bh + 10, 16, 16);

            Graphics2D bg = (Graphics2D) g.create();
            bg.translate(bx, by);
            bg.setClip(0, 0, bw, bh);
            for (int y = 0; y < GameEngine.ROWS; y++) {
                for (int x = 0; x < GameEngine.COLS; x++) {
                    bg.setColor(((x + y) & 1) == 0 ? Theme.BOARD_A : Theme.BOARD_B);
                    bg.fillRect(x * cell, y * cell, cell, cell);
                }
            }
            engine.drawWorld(bg, cell);
            drawOverlay(bg, bw, bh);
            bg.dispose();
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ HUD

    private void drawHud(Graphics2D g, int x, int y, int w) {
        int cardW = (int) (w * 0.37);
        int cardH = 66;
        drawPlayerCard(g, engine.getPlayer1(), Theme.P1, x, y, cardW, cardH, false);
        drawPlayerCard(g, engine.getPlayer2(), Theme.P2, x + w - cardW, y, cardW, cardH, true);

        int cx = x + w / 2;
        g.setColor(Theme.TEXT);
        g.setFont(Theme.bold(26));
        long sec = engine.getElapsedMs() / 1000;
        Theme.drawCentered(g, String.format("%02d:%02d", sec / 60, sec % 60), cx, y + 32);
        g.setColor(Theme.MUTED);
        g.setFont(Theme.plain(12));
        Theme.drawCentered(g, engine.getConfig().getMode().toString(), cx, y + 52);
    }

    private void drawPlayerCard(Graphics2D g, Snake s, Color color, int x, int y, int w, int h, boolean right) {
        g.setColor(Theme.PANEL);
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setColor(color);
        g.fillRoundRect(right ? x + w - 7 : x, y, 7, h, 7, 7);

        int pad = 18;
        int anchor = right ? x + w - pad - 7 : x + pad + 7;

        g.setColor(color);
        g.setFont(Theme.bold(16));
        Theme.drawAligned(g, s.getName(), anchor, y + 24, right);

        g.setColor(Theme.TEXT);
        g.setFont(Theme.bold(30));
        Theme.drawAligned(g, String.valueOf(s.getScore()), anchor, y + 56, right);

        g.setColor(Theme.MUTED);
        g.setFont(Theme.plain(13));
        int farAnchor = right ? x + pad + 7 : x + w - pad - 7;
        Theme.drawAligned(g, "Length " + s.getLength(), farAnchor, y + 56, !right);

        // active effect pills under the card
        int cx = right ? x + w : x;
        g.setFont(Theme.bold(11));
        FontMetrics fm = g.getFontMetrics();
        for (EffectType type : EffectType.values()) {
            if (!s.hasEffect(type)) {
                continue;
            }
            String text = type.getLabel() + " " + String.format("%.1fs", s.effectMsLeft(type) / 1000.0);
            int pw = fm.stringWidth(text) + 16;
            int px = right ? cx - pw : cx;
            Color c = type.getColor();
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.fillRoundRect(px, y + h + 5, pw, 18, 18, 18);
            g.setColor(c);
            g.drawString(text, px + 8, y + h + 18);
            cx += right ? -(pw + 6) : (pw + 6);
        }
    }

    // ------------------------------------------------------------------ overlays

    private void drawOverlay(Graphics2D g, int w, int h) {
        GameEngine.State state = engine.getState();
        int cx = w / 2;
        int cy = h / 2;
        switch (state) {
            case COUNTDOWN:
                dim(g, w, h, 120);
                g.setColor(Theme.TEXT);
                g.setFont(Theme.bold(Math.max(60, h / 4)));
                Theme.drawCentered(g, String.valueOf(engine.getCountdownSeconds()), cx, cy + h / 10);
                g.setColor(Theme.MUTED);
                g.setFont(Theme.bold(16));
                Theme.drawCentered(g, "GET READY", cx, cy - h / 6);
                g.setFont(Theme.plain(14));
                g.setColor(Theme.P1);
                Theme.drawCentered(g, engine.getPlayer1().getName() + ":  W A S D", cx, cy + h / 4);
                g.setColor(Theme.P2);
                Theme.drawCentered(g, engine.getPlayer2().getName() + ":  Arrow keys", cx, cy + h / 4 + 22);
                break;
            case RUNNING:
                if (engine.getElapsedMs() < 700) {
                    float alpha = 1f - engine.getElapsedMs() / 700f;
                    Graphics2D ga = (Graphics2D) g.create();
                    ga.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
                    ga.setColor(Theme.GOLD);
                    ga.setFont(Theme.bold(Math.max(48, h / 6)));
                    Theme.drawCentered(ga, "GO!", cx, cy + h / 14);
                    ga.dispose();
                }
                break;
            case PAUSED:
                dim(g, w, h, 150);
                g.setColor(Theme.TEXT);
                g.setFont(Theme.bold(Math.max(40, h / 8)));
                Theme.drawCentered(g, "PAUSED", cx, cy);
                g.setColor(Theme.MUTED);
                g.setFont(Theme.plain(16));
                Theme.drawCentered(g, "Press P or Space to resume", cx, cy + 34);
                break;
            default:
                drawGameOver(g, w, h);
                break;
        }
    }

    private void drawGameOver(Graphics2D g, int w, int h) {
        dim(g, w, h, 150);
        int cw = Math.min(w - 40, 540);
        int ch = Math.min(h - 20, 270);
        int x = (w - cw) / 2;
        int y = (h - ch) / 2;
        g.setColor(new Color(24, 29, 54, 245));
        g.fillRoundRect(x, y, cw, ch, 26, 26);

        Snake winner = engine.getWinner();
        Color titleColor = winner == null ? Theme.GOLD : winner.getHeadColor();
        g.setColor(titleColor);
        g.drawRoundRect(x, y, cw, ch, 26, 26);

        int cx = w / 2;
        g.setFont(Theme.bold(34));
        Theme.drawCentered(g, engine.getResultTitle(), cx, y + 56);

        Snake p1 = engine.getPlayer1();
        Snake p2 = engine.getPlayer2();
        g.setFont(Theme.bold(20));
        g.setColor(Theme.P1);
        Theme.drawAligned(g, p1.getName() + "  " + p1.getScore(), cx - 18, y + 98, true);
        g.setColor(Theme.MUTED);
        Theme.drawCentered(g, "-", cx, y + 98);
        g.setColor(Theme.P2);
        Theme.drawAligned(g, p2.getScore() + "  " + p2.getName(), cx + 18, y + 98, false);

        g.setFont(Theme.plain(14));
        g.setColor(Theme.TEXT);
        int ly = y + 132;
        for (Snake s : Arrays.asList(p1, p2)) {
            if (!s.isAlive()) {
                Theme.drawCentered(g, s.getName() + " " + s.getDeathReason() + ".", cx, ly);
                ly += 22;
            }
        }

        g.setFont(Theme.bold(14));
        g.setColor(Theme.GOLD);
        Theme.drawCentered(g, "R  = rematch        Esc  = back to menu", cx, y + ch - 52);
        g.setFont(Theme.plain(12));
        g.setColor(Theme.MUTED);
        Theme.drawCentered(g, controller.getSaveStatus(), cx, y + ch - 26);
    }

    private static void dim(Graphics2D g, int w, int h, int alpha) {
        g.setColor(new Color(5, 7, 18, alpha));
        g.fillRect(0, 0, w, h);
    }
}
