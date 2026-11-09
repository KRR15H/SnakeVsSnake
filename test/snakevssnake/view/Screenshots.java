package snakevssnake.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

import snakevssnake.controller.GameController;
import snakevssnake.controller.GameEngine;
import snakevssnake.controller.GameLoop;
import snakevssnake.db.FileScoreStore;
import snakevssnake.model.*;
import snakevssnake.util.SettingsManager;
import snakevssnake.util.SoundPlayer;

/** Renders the screens to PNG files without opening a window (headless). */
public class Screenshots {
    static void save(JPanel p, int w, int h, String name) throws Exception {
        p.setSize(w, h);
        p.doLayout();
        layoutAll(p);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        p.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", new File(name));
    }
    static void layoutAll(java.awt.Container c) {
        c.doLayout();
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof java.awt.Container) layoutAll((java.awt.Container) k);
        }
    }
    static void steerSafely(GameEngine e, Snake s, int pl, java.util.Random r) {
        java.awt.Point h = s.head();
        java.util.List<Direction> ok = new java.util.ArrayList<>();
        for (Direction d : Direction.values()) {
            int nx = h.x + d.dx, ny = h.y + d.dy;
            if (d == s.getDirection().opposite() || nx < 1 || ny < 1 || nx >= GameEngine.COLS - 1 || ny >= GameEngine.ROWS - 1) continue;
            if (e.getPlayer1().occupies(nx, ny) || e.getPlayer2().occupies(nx, ny)) continue;
            ok.add(d);
        }
        if (ok.isEmpty()) return;
        if (!ok.contains(s.getDirection()) || r.nextInt(25) == 0) e.steer(pl, ok.get(r.nextInt(ok.size())));
    }

    public static void main(String[] a) throws Exception {
        Path dir = Files.createTempDirectory("shots");
        String out = a[0];
        SettingsManager sm = new SettingsManager(dir);
        GameConfig cfg = new GameConfig("Ramprasad", "Krrish", Mode.POWER_UPS, 3, false);
        MenuPanel menu = new MenuPanel(cfg, "SQLite database via JDBC", sm, c -> {}, () -> {}, () -> {}, () -> {});
        save(menu, 920, 680, out + "/menu.png");

        GameController gc = new GameController(cfg, new FileScoreStore(dir.resolve("m.tsv")), new SoundPlayer());
        GameEngine e = gc.getEngine();
        GamePanel gp = new GamePanel(gc, () -> {}, () -> {});
        gp.setSize(920, 680);
        save(gp, 920, 680, out + "/countdown.png");

        java.util.Random r = new java.util.Random(7);
        e.update(3001);
        int t = 0;
        while (t++ < 1400 && e.getState() == GameEngine.State.RUNNING) {
            for (int pl = 1; pl <= 2; pl++) {
                Snake s = pl == 1 ? e.getPlayer1() : e.getPlayer2();
                steerSafely(e, s, pl, r);
            }
            e.update(16);
            if (t == 1200) break;
        }
        save(gp, 920, 680, out + "/playing.png");
        e.pause();
        save(gp, 920, 680, out + "/paused.png");
        e.resume();
        t = 0;
        while (e.getState() == GameEngine.State.RUNNING && t++ < 20000) e.update(16);   // no steering: they hit walls
        Thread.sleep(300);
        save(gp, 920, 680, out + "/gameover.png");
        gc.stop();
        System.exit(0);
    }
}
