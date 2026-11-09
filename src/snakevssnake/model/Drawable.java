package snakevssnake.model;

import java.awt.Graphics2D;

/** Anything that can paint itself on the board. The graphics origin is the board's top-left corner. */
public interface Drawable {
    /**
     * @param g    graphics context (already translated to the board origin)
     * @param cell size of one board cell in pixels
     */
    void draw(Graphics2D g, int cell);
}
