package snakevssnake.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/** A stone block that appears during the game. Hitting it ends the game for that snake. */
public class Obstacle extends GameObject {

    public Obstacle(int x, int y) {
        super(x, y);
    }

    @Override
    public void draw(Graphics2D g, int cell) {
        int px = x * cell;
        int py = y * cell;
        int pad = Math.max(1, cell / 12);
        int size = cell - 2 * pad;
        int arc = Math.max(3, cell / 4);

        g.setColor(new Color(104, 112, 138));
        g.fillRoundRect(px + pad, py + pad, size, size, arc, arc);
        g.setColor(new Color(64, 70, 92));
        g.setStroke(new BasicStroke(Math.max(1.5f, cell / 12f)));
        g.drawRoundRect(px + pad, py + pad, size, size, arc, arc);

        // simple "brick" cracks
        g.setColor(new Color(72, 78, 102));
        g.drawLine(px + cell / 4, py + cell / 2, px + 3 * cell / 4, py + cell / 2);
        g.drawLine(px + cell / 2, py + pad + 2, px + cell / 2, py + cell / 2);
        g.setColor(new Color(150, 158, 182, 160));
        g.drawLine(px + pad + 3, py + pad + 3, px + cell - pad - 4, py + pad + 3);
    }
}
