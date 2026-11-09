package snakevssnake.model;

import java.awt.Color;
import java.util.concurrent.ScheduledExecutorService;

/** Cuts 3 segments off your own tail (more room to move) and gives 5 bonus points. */
public class Shrink extends PowerUp {

    public Shrink(int x, int y, long spawnedAt) {
        super(x, y, spawnedAt);
    }

    @Override public String getName() { return "Shrink"; }
    @Override public String getDescription() { return "Your snake loses 3 tail segments and you gain 5 points."; }
    @Override public Color getColor() { return new Color(90, 220, 140); }
    @Override public String getSymbol() { return "R"; }

    @Override
    public void apply(Snake collector, Snake opponent, ScheduledExecutorService timer) {
        collector.shrink(3);
        collector.addScore(5);
    }
}
