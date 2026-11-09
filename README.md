# Snake Vs Snake

Two-player Snake game for one keyboard, written entirely in Java (Swing front end, JDBC/file back end).
Team Unreal, Advanced Java Programming (CSE 213).

## Run it

1. Install a **JDK** (not only a JRE), version 8 or newer. Check with `javac -version` in a terminal.
2. Unzip this folder anywhere, for example `C:\Projects\SnakeVsSnake` or `~/Projects/SnakeVsSnake`.
3. Start the game:
   - **Windows:** double-click `run.bat`
   - **Linux / macOS:** open a terminal in the folder and run `./run.sh`

The script compiles everything into `out/` and launches the game.

### In an IDE (IntelliJ IDEA / Eclipse / VS Code)
Open the folder as a project, mark `src` as the sources root, and run `snakevssnake.Main`.

### By hand
```
javac -d out -cp "lib/*" $(find src -name "*.java")
java -cp "out:lib/*" snakevssnake.Main          (Windows: use ; instead of :)
```

## Controls

| Action | Player 1 | Player 2 |
|---|---|---|
| Move | W A S D | Arrow keys |

`P` or `Space` pause, `R` rematch (after game over), `Esc` back to menu.
The game pauses by itself if the window loses focus.

## Rules

- Eat food (+10) to grow. You crash if you hit a wall, an obstacle, your own body or the other snake.
- The surviving snake wins. If both crash at the same moment, the higher score wins (equal scores = draw).
- Modes: **Classic**, **Obstacles** (stones appear over time), **Power-ups**, **Chaos** (both).
- Power-ups: **F** speed boost (you, 5 s) - **S** slow-down (opponent, 5 s) - **2** double points (you, 8 s) - **R** shrink (lose 3 tail segments, +5 points).

## Storing scores: JDBC (optional jar)

Without any extra file the game saves results to `data/matches.tsv`.
To use a real **SQLite database through JDBC**, put the `sqlite-jdbc` jar into the `lib/` folder
(link and steps are in `lib/README.txt`). The menu footer then shows "SQLite database via JDBC"
and results go to `data/snake.db` (tables `players` and `matches`).
Settings (names, mode, speed, sound) are saved in `data/settings.properties`.

## Tests

`run_tests.bat` / `./run_tests.sh` runs 58 automated checks: snake rules, power-ups, effect timers,
pause, collisions, validation, settings, both storage back ends, and 400 full games played by random bots
in every mode (checks that snakes never overlap or leave the board).

## Project structure (MVC style)

```
src/snakevssnake/
  Main.java                  entry point
  model/                     Snake, Food, Obstacle, PowerUp (+ 4 subclasses), Direction, Mode, GameConfig
  controller/                GameEngine (rules), GameLoop (thread), InputHandler (keys), GameController
  view/                      MainFrame, MenuPanel, GamePanel, LeaderboardDialog, Theme, StyledButton
  db/                        ScoreStore (interface), JdbcScoreStore, FileScoreStore, DatabaseManager
  util/                      SettingsManager (file I/O), InputValidator, SoundPlayer
test/                        GameTest (automated tests), Screenshots (headless screen renderer)
docs/screenshots/            pictures of the menu, gameplay, pause and game-over screens
```

## Java concepts and where to find them (for the viva)

| Concept | Where |
|---|---|
| Classes & objects | `Snake`, `Food`, `Obstacle`, `GameEngine`, `MatchRecord` |
| Inheritance | `GameObject` -> `Food`, `Obstacle`, `PowerUp` -> `SpeedBoost`, `SlowDown`, `DoublePoints`, `Shrink` |
| Polymorphism | each power-up overrides `apply()`; `ScoreStore` has two implementations |
| Abstraction & interfaces | abstract `PowerUp`; `Drawable`, `Collidable`, `ScoreStore`, `GameListener` |
| Collections | `ArrayDeque` (snake body), `ArrayList`, `HashSet` (O(1) collision lookup), `HashMap` |
| Multithreading | `GameLoop` thread, effect-timer thread (`ScheduledExecutorService`), sound thread, result-saver thread, `SwingWorker`; `synchronized` shared state |
| Event handling | `KeyListener` (`InputHandler`), `ActionListener` (menu), `FocusListener` (auto-pause) |
| Exception handling | custom `StorageException`, `InvalidInputException`; missing driver, files and sound device handled |
| File I/O | `SettingsManager` (`FileInputStream` / `FileOutputStream`), `FileScoreStore` (NIO) |
| JDBC | `JdbcScoreStore`: `PreparedStatement`, transactions (commit / rollback), `ORDER BY` leaderboard |
| Swing | custom painting with `Graphics2D`, `CardLayout`, `JTable`, `JDialog` |
| Design patterns | MVC split, observer (`GameListener`), strategy/factory (`DatabaseManager`) |

## How the main logic works

- **Movement:** every tick adds a new head cell and removes the tail, unless the snake is growing.
- **Collisions:** all occupied cells go into a `HashSet`; the next head cell is checked against it in O(1).
  Both snakes' targets are computed first, so a head-on crash is detected fairly and the result does not depend on which player is processed first.
- **Game loop:** `update() -> repaint() -> sleep()` about 60 times per second on its own thread,
  so the window never freezes. Each snake has its own move timer, which is how speed boost and slow-down work.
- **Pause:** the game clock stops and the effect countdowns are frozen and restored on resume.
