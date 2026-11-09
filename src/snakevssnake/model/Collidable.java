package snakevssnake.model;

/** Anything that occupies cells on the board and can therefore be collided with. */
public interface Collidable {
    /** @return true if this object covers the board cell (x, y) */
    boolean occupies(int x, int y);
}
