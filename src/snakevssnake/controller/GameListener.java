package snakevssnake.controller;

import snakevssnake.model.PowerUp;
import snakevssnake.model.Snake;

/**
 * Callbacks fired by the {@link GameEngine} (observer pattern). The engine does not know about
 * sound or databases; whoever listens decides what to do. All methods have empty defaults, so a
 * listener only overrides what it needs. They run on the game-loop thread, so they must be quick.
 */
public interface GameListener {

    default void onGameStart() { }

    default void onFoodEaten(Snake snake) { }

    default void onPowerUpCollected(Snake snake, PowerUp powerUp) { }

    default void onCrash(Snake snake) { }

    default void onGameOver(GameEngine engine) { }
}
