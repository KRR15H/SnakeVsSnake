package snakevssnake.model;

import java.awt.Color;
import java.util.concurrent.ScheduledExecutorService;

/** Doubles the points from food for 8 seconds. */
public class DoublePoints extends PowerUp {

    public DoublePoints(int x, int y, long spawnedAt) {
        super(x, y, spawnedAt);
    }

    @Override public String getName() { return "Double Points"; }
    @Override public String getDescription() { return "Food is worth double for 8 seconds."; }
    @Override public Color getColor() { return EffectType.DOUBLE_POINTS.getColor(); }
    @Override public String getSymbol() { return "2"; }

    @Override
    public void apply(Snake collector, Snake opponent, ScheduledExecutorService timer) {
        collector.addEffect(EffectType.DOUBLE_POINTS, 8000, timer);
    }
}
