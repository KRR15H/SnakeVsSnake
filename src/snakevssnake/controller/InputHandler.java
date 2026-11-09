package snakevssnake.controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import snakevssnake.model.Direction;

/**
 * Keyboard handling for two players on one keyboard.
 * Player 1: W A S D        Player 2: arrow keys
 * P or Space: pause        R: rematch (after game over)        Esc: back to menu
 */
public class InputHandler implements KeyListener {

    private final GameController controller;
    private final Runnable onMenu;
    private final Runnable onRematch;

    public InputHandler(GameController controller, Runnable onMenu, Runnable onRematch) {
        this.controller = controller;
        this.onMenu = onMenu;
        this.onRematch = onRematch;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:     controller.steer(1, Direction.UP); break;
            case KeyEvent.VK_S:     controller.steer(1, Direction.DOWN); break;
            case KeyEvent.VK_A:     controller.steer(1, Direction.LEFT); break;
            case KeyEvent.VK_D:     controller.steer(1, Direction.RIGHT); break;

            case KeyEvent.VK_UP:    controller.steer(2, Direction.UP); break;
            case KeyEvent.VK_DOWN:  controller.steer(2, Direction.DOWN); break;
            case KeyEvent.VK_LEFT:  controller.steer(2, Direction.LEFT); break;
            case KeyEvent.VK_RIGHT: controller.steer(2, Direction.RIGHT); break;

            case KeyEvent.VK_P:
            case KeyEvent.VK_SPACE: controller.togglePause(); break;

            case KeyEvent.VK_R:
                if (controller.isOver()) {
                    onRematch.run();
                }
                break;
            case KeyEvent.VK_ESCAPE: onMenu.run(); break;
            default: break;
        }
        e.consume();
    }

    @Override
    public void keyReleased(KeyEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) { }
}
