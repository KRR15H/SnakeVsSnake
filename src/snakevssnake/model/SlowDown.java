package snakevssnake.model;

import java.awt.Color;
import java.util.concurrent.ScheduledExecutorService;

/** Slows the OPPONENT down for 5 seconds. */
public class SlowDown extends PowerUp {

    public SlowDown(int x, int y, long spawnedAt) {
        super(x, y, spawnedAt);
    }

    @Override public String getName() { return "Slow-Down"; }
    @Override public String getDescription() { return "Your opponent moves slowly for 5 seconds."; }
    @Override public Color getColor() { return EffectType.SLOWED.getColor(); }
    @Override public String getSymbol() { return "S"; }

    @Override
    public void apply(Snake collector, Snake opponent, ScheduledExecutorService timer) {
        opponent.addEffect(EffectType.SLOWED, 5000, timer);
    }
}
