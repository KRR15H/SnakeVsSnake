package snakevssnake.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Colours, fonts and small drawing helpers shared by all screens. */
public final class Theme {

    public static final Color BG_TOP = new Color(11, 14, 30);
    public static final Color BG_BOTTOM = new Color(30, 22, 54);
    public static final Color PANEL = new Color(27, 33, 60);
    public static final Color BOARD_A = new Color(17, 22, 41);
    public static final Color BOARD_B = new Color(21, 27, 49);
    public static final Color BOARD_BORDER = new Color(84, 96, 150);
    public static final Color TEXT = new Color(236, 239, 252);
    public static final Color MUTED = new Color(147, 156, 186);
    public static final Color P1 = new Color(70, 235, 130);
    public static final Color P2 = new Color(255, 150, 70);
    public static final Color GOLD = new Color(255, 205, 60);
    public static final Color DANGER = new Color(255, 100, 110);

    private Theme() { }

    public static Font bold(int size) {
        return new Font("SansSerif", Font.BOLD, size);
    }

    public static Font plain(int size) {
        return new Font("SansSerif", Font.PLAIN, size);
    }

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** Dark gradient with a faint grid, used behind every screen. */
    public static void paintBackground(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 9));
        for (int x = 0; x < w; x += 40) {
            g.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 40) {
            g.drawLine(0, y, w, y);
        }
    }

    public static void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    public static void drawAligned(Graphics2D g, String text, int anchorX, int baselineY, boolean alignRight) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, alignRight ? anchorX - fm.stringWidth(text) : anchorX, baselineY);
    }
}
