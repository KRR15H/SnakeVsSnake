package snakevssnake.view;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import snakevssnake.controller.GameController;
import snakevssnake.db.DatabaseManager;
import snakevssnake.db.ScoreStore;
import snakevssnake.model.GameConfig;
import snakevssnake.util.SettingsManager;
import snakevssnake.util.SoundPlayer;

/** The application window. Switches between the menu and the game screen with a CardLayout. */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String MENU = "menu";
    private static final String GAME = "game";

    private final CardLayout layout = new CardLayout();
    private final JPanel root = new JPanel(layout);
    private final ScoreStore store;
    private final SoundPlayer sound = new SoundPlayer();

    private GamePanel gamePanel;
    private GameController controller;

    public MainFrame() {
        super("Snake Vs Snake  -  Team Unreal");
        Path dataDir = Paths.get("data");
        SettingsManager settings = new SettingsManager(dataDir);
        store = DatabaseManager.open(dataDir);

        MenuPanel menu = new MenuPanel(settings.load(), store.backendName(), settings,
                this::startGame, this::showLeaderboard, this::showHelp, this::quit);
        root.add(menu, MENU);
        setContentPane(root);

        setMinimumSize(new Dimension(760, 620));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void startGame(GameConfig config) {
        stopGame();
        sound.setEnabled(config.isSoundOn());
        controller = new GameController(config, store, sound);
        gamePanel = new GamePanel(controller, this::showMenu, () -> startGame(config));
        root.add(gamePanel, GAME);
        layout.show(root, GAME);
        gamePanel.activate();
        controller.start();
    }

    private void stopGame() {
        if (controller != null) {
            controller.stop();
            controller = null;
        }
        if (gamePanel != null) {
            root.remove(gamePanel);
            gamePanel = null;
        }
    }

    private void showMenu() {
        stopGame();
        layout.show(root, MENU);
        root.revalidate();
        root.repaint();
    }

    private void showLeaderboard() {
        new LeaderboardDialog(this, store).setVisible(true);
    }

    private void showHelp() {
        String html = "<html><body style='width:430px'>"
                + "<h2>How to play</h2>"
                + "<p>Two players share one keyboard. Eat food to grow and score. "
                + "The first snake to crash loses the round.</p>"
                + "<p><b>Player 1:</b> W A S D &nbsp;&nbsp; <b>Player 2:</b> Arrow keys<br>"
                + "<b>P</b> or <b>Space</b>: pause &nbsp; <b>R</b>: rematch &nbsp; <b>Esc</b>: menu</p>"
                + "<p><b>You crash if you hit:</b> a wall, an obstacle, your own body, "
                + "or the other snake.</p>"
                + "<p><b>Power-ups</b> (Power-ups and Chaos modes):<br>"
                + "<b>F</b> Speed boost: you move faster for 5 s<br>"
                + "<b>S</b> Slow-down: your opponent moves slower for 5 s<br>"
                + "<b>2</b> Double points: food is worth double for 8 s<br>"
                + "<b>R</b> Shrink: lose 3 tail segments, gain 5 points</p>"
                + "<p><b>Modes:</b> Classic (food only), Obstacles (stones appear over time), "
                + "Power-ups, Chaos (everything).</p>"
                + "<p>If both snakes crash in the same moment, the higher score wins.</p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, new JLabel(html), "How to Play", JOptionPane.PLAIN_MESSAGE);
    }

    private void quit() {
        stopGame();
        sound.shutdown();
        store.close();
        dispose();
        System.exit(0);
    }
}
