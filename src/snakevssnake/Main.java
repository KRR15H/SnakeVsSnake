package snakevssnake;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import snakevssnake.view.MainFrame;

/**
 * Entry point of Snake Vs Snake.
 * Run with:  java -cp out snakevssnake.Main
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Cross-platform look and feel so the dark theme looks the same on Windows, Linux and macOS.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("Could not set look and feel, using default: " + e.getMessage());
        }
        // All Swing components must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
