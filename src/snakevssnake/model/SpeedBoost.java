package snakevssnake.model;

import java.awt.Color;
import java.util.concurrent.ScheduledExecutorService;

/** Makes the collector move faster for 5 seconds. */
public class SpeedBoost extends PowerUp {

    public SpeedBoost(int x, int y, long spawnedAt) {
        super(x, y, spawnedAt);
    }

    @Override public String getName() { return "Speed Boost"; }
    @Override public String getDescription() { return "You move much faster for 5 seconds."; }
    @Override public Color getColor() { return EffectType.SPEED_BOOST.getColor(); }
    @Override public String getSymbol() { return "F"; }

    @Override
    public void apply(Snake collector, Snake opponent, ScheduledExecutorService timer) {
        collector.addEffect(EffectType.SPEED_BOOST, 5000, timer);
    }
}
