package snakevssnake.model;

/**
 * Base class for every single-cell object on the board (food, obstacles, power-ups).
 * Demonstrates inheritance and abstraction: subclasses only decide how they look.
 */
public abstract class GameObject implements Drawable, Collidable {

    protected final int x;
    protected final int y;

    protected GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean occupies(int px, int py) {
        return px == x && py == y;
    }
}
