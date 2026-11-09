package snakevssnake.model;

import java.awt.Color;

/** Timed effects a snake can be under because of a power-up. */
public enum EffectType {
    SPEED_BOOST("Speed", new Color(255, 214, 10)),
    SLOWED("Slowed", new Color(110, 165, 255)),
    DOUBLE_POINTS("x2 Points", new Color(255, 105, 180));

    private final String label;
    private final Color color;

    EffectType(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }
}
