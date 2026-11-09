package snakevssnake.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/** A food pellet. Eating it grows the snake by one cell and scores points. */
public class Food extends GameObject {

    public static final int POINTS = 10;

    public Food(int x, int y) {
        super(x, y);
    }

    @Override
    public void draw(Graphics2D g, int cell) {
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0);
        int cx = x * cell + cell / 2;
        int cy = y * cell + cell / 2;

        // soft glow that breathes in and out
        int glow = (int) (cell * (1.1 + 0.35 * pulse));
        g.setColor(new Color(255, 70, 90, 55));
        g.fillOval(cx - glow / 2, cy - glow / 2, glow, glow);

        // the apple itself
        int r = (int) (cell * 0.56);
        g.setColor(new Color(235, 55, 75));
        g.fillOval(cx - r / 2, cy - r / 2 + 1, r, r);
        g.setColor(new Color(255, 150, 160, 190));
        g.fillOval(cx - r / 4 - 1, cy - r / 3, Math.max(2, r / 4), Math.max(2, r / 4));

        // little stem
        g.setColor(new Color(90, 190, 90));
        g.setStroke(new BasicStroke(Math.max(1.5f, cell / 10f)));
        g.drawLine(cx, cy - r / 2, cx + cell / 8, cy - r / 2 - cell / 6);
    }
}
