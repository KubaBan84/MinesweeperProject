import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Scanner; 

// !!ENUMERATION!!
enum GameMode {
    REVEAL,
    FLAGGING
}
// !!CONSTRUCTOR!!
public class MinesweeperGUI extends JFrame {
    
    // !!INHERITANCE!!
    class MineTile extends JButton {
        public int r;
        public int c;

        public MineTile(int r, int c) {
            super();
            this.r = r;
            this.c = c;
            this.setFont(new Font("Arial", Font.BOLD, 16));
        }
    }

    // !!ENCAPSULATION!!
    public MineTile[][] buttons = new MineTile[10][10];
    private GridWrapper<Integer> backendGrid;
    private boolean[][] revealed = new boolean[10][10];
    private int totalBombs = 0;
    private int revealedCount = 0;
    private int remaningflags = 0;
    private JLabel flagsLabel;
    private long gamePtr;

    private int bestTime = 9999;

    // !!PARALLEL PROGRAMMING!!
    private JLabel timerLabel;
    private Thread timerThread;
    private volatile boolean gameRunning;
    private int timeElapsed = 0;

    // !!POLYMORPHISM!!
    private ClickHandler currentHandler; 

    // --- JNI METHODS ---
    public native int[][] requestMapFromCpp(int bombs, long gamePtr, boolean fromfile);
    public native int[][] getFloodFill(int r, int c, long gamePtr);
    public native long setupcpp();
    public native void endofgamecpp(long gamePtr);
    
    static {
        System.loadLibrary("minesweeper");
    }

    // !!CONSTRUCTOR!!
    public MinesweeperGUI() {
        setupGame();
    }

    private void setupGame() {
        getContentPane().removeAll();
        gamePtr = setupcpp(); // Initialize C++ backend
        revealed = new boolean[10][10];
        revealedCount = 0;

        currentHandler = new RevealHandler();

        setTitle("Minesweeper - OOP Final Project");
        setSize(600, 750);
        setLayout(new BorderLayout());

        // --- Top UI: Bomb Selection and Flag Toggle ---
        JPanel topPanel = new JPanel(new GridLayout(4, 1));
        
        Object[] options = {"Yes", "No"};
        int n = JOptionPane.showOptionDialog(null,
                "Load map from file?", 
                "Map Loading", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, options, options[0]);
        
        boolean loadFromFile = (n == JOptionPane.YES_OPTION);
        
        // Input validation loop
        while(!loadFromFile && (totalBombs < 1 || totalBombs > 50)) {
            String input = JOptionPane.showInputDialog(this, "Enter number of bombs (1-50):");
            if(input == null) continue;
            try {
                totalBombs = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                continue; 
            }
        }
        
        int[][] rawData = requestMapFromCpp(totalBombs, gamePtr, loadFromFile);
        backendGrid = new GridWrapper<>(10, 10);
        for(int r=0; r<10; r++) {
            for(int c=0; c<10; c++) {
                backendGrid.set(r, c, rawData[r][c]);
            }
        }

        // !!ERROR HANDLING!!
        if(backendGrid == null || rawData == null) {
            JOptionPane.showMessageDialog(this, "Failed to load map. Generating random map.");
            loadFromFile = false;
            rawData = requestMapFromCpp(totalBombs, gamePtr, loadFromFile);
            backendGrid = new GridWrapper<>(10, 10);
            for(int r=0; r<10; r++) {
                for(int c=0; c<10; c++) {
                    backendGrid.set(r, c, rawData[r][c]);
                }
            }
        }

        // Recalculate bombs
        if(!loadFromFile) {
             totalBombs = countBombsInGrid();
        } else {
             totalBombs = countBombsInGrid();
        }
        
        remaningflags = totalBombs;
        loadBestTime();

        JToggleButton flagToggle = new JToggleButton("MODE: REVEAL");
        flagToggle.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
        flagToggle.addActionListener(e -> {
            boolean isSelected = flagToggle.isSelected();
            GameMode mode = isSelected ? GameMode.FLAGGING : GameMode.REVEAL;

            if (mode == GameMode.FLAGGING) {
                currentHandler = new FlagHandler();
                flagToggle.setText("MODE: FLAGGING ( \uD83D\uDEA9 )"); 
                flagToggle.setForeground(Color.RED);
            } else {
                currentHandler = new RevealHandler();
                flagToggle.setText("MODE: REVEAL");
                flagToggle.setForeground(Color.BLUE);
            }
        });

        flagsLabel = new JLabel("Remaining Flags: " + remaningflags, SwingConstants.CENTER);

        timerLabel = new JLabel("Time: 0s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));

        topPanel.add(new JLabel("Goal: Reveal all safe spaces. Total Bombs: " + totalBombs, SwingConstants.CENTER));
        topPanel.add(flagsLabel);
        topPanel.add(timerLabel);
        topPanel.add(flagToggle);
        add(topPanel, BorderLayout.NORTH);

        startTimer();

        // --- Grid Panel ---
        JPanel gridPanel = new JPanel(new GridLayout(10, 10));
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                buttons[r][c] = new MineTile(r, c);
                MineTile btn = buttons[r][c]; 
                btn.addActionListener(e -> handleClick(btn));
                gridPanel.add(btn);
            }
        }
        add(gridPanel, BorderLayout.CENTER);
        
        revalidate();
        repaint();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }
    
    private int countBombsInGrid() {
        int count = 0;
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (backendGrid.get(r, c) == -1) count++;
            }
        }
        return count;
    }

    private void handleClick(MineTile btn) {
        currentHandler.onTileClicked(btn, this);
    }

    // --- GAME LOGIC HELPERS ---
    public void showCell(int r, int c) {
        if (revealed[r][c]) return;
        
        revealed[r][c] = true;
        revealedCount++;
        int val = backendGrid.get(r, c);
        
        MineTile btn = buttons[r][c];
        btn.setEnabled(false);
        btn.setBackground(Color.LIGHT_GRAY);
        btn.setText(val == 0 ? "" : String.valueOf(val));
        
        if (val == 1) btn.setForeground(Color.BLUE);
        else if (val == 2) btn.setForeground(new Color(0, 128, 0));
        else if (val >= 3) btn.setForeground(Color.RED);
    }

    public void checkWin() {
        if (revealedCount == (100 - totalBombs)) {
            String message = "\uD83C\uDFC6 GAME OVER - YOU WIN!";
            if (timeElapsed < bestTime) {
                String oldBest = (bestTime == 9999) ? "None" : bestTime + "s";
                message += "\nNEW HIGH SCORE! " + timeElapsed + "s (Old Best: " + oldBest + ")";
                saveBestTime(timeElapsed); 
            } else {
                message += "\nYour Time: " + timeElapsed + "s";
                message += "\nBest Time to Beat: " + bestTime + "s";
            }
            
            endGame(message);
        }
    }

    public void revealAllBombs() {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (backendGrid.get(r, c) == -1) {
                    buttons[r][c].setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                    buttons[r][c].setText("\uD83D\uDCA3");
                    buttons[r][c].setBackground(Color.RED);
                    buttons[r][c].setForeground(Color.BLACK);
                    buttons[r][c].setEnabled(false);
                }
            }
        }
    }

    // !!DESTRUCTOR!!
    public void endGame(String message) {
        gameRunning = false; 
        totalBombs = 0;
        endofgamecpp(gamePtr);
        int response = JOptionPane.showConfirmDialog(this, message +  "\nPlay again?", "Game Over", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            setupGame();
        } else {
            System.exit(0);
        }
    }
    
    // --- GETTERS & UPDATERS ---
    public void updateFlagLabel() { flagsLabel.setText("Remaining Flags: " + remaningflags); }
    public GridWrapper<Integer> getBackendGrid() { return backendGrid; }
    public long getGamePtr() { return gamePtr; }
    public boolean[][] getRevealed() { return revealed; }
    public void incrementFlags() { remaningflags++; }
    public void decrementFlags() { remaningflags--; }

    private void startTimer() {
        timeElapsed = 0;
        gameRunning = true;
        timerLabel.setText("Time: 0s");
        timerThread = new Thread(() -> {
            while (gameRunning) {
                try {
                    Thread.sleep(1000); 
                    timeElapsed++;
                    SwingUtilities.invokeLater(() -> {
                        timerLabel.setText("Time: " + timeElapsed + "s");
                    });
                } catch (InterruptedException e) {
                    break; 
                }
            }
        });
        timerThread.start();
    }

    // !!READ/WRITE TO FILE!!
    private void loadBestTime() {
        File file = new File("highscore.txt");
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                if (scanner.hasNextInt()) {
                    bestTime = scanner.nextInt();
                }
            } catch (FileNotFoundException e) {
                System.err.println("Could not load high score.");
            }
        }
    }

    private void saveBestTime(int newTime) {
        try (FileWriter writer = new FileWriter("highscore.txt")) {
            writer.write(String.valueOf(newTime));
            bestTime = newTime; // Update memory too
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving high score!");
        }
    }

    // !!POLYMORPHISM!! 
    // !!INTERFACE!!
    interface ClickHandler {
        void onTileClicked(MineTile btn, MinesweeperGUI game);
    }
    class RevealHandler implements ClickHandler {
        @Override
        public void onTileClicked(MineTile btn, MinesweeperGUI game) {
            int r = btn.r; 
            int c = btn.c;

            if ("\uD83D\uDEA9".equals(btn.getText())) return; 

            if (game.getBackendGrid().get(r, c) == -1) {
                game.revealAllBombs();
                game.endGame("\uD83D\uDCA3 YOU LOSE! You hit a bomb.");
            } else {
                int[][] toReveal = game.getFloodFill(r, c, game.getGamePtr());
                for (int[] coord : toReveal) {
                    game.showCell(coord[0], coord[1]);
                }
                game.checkWin();
            }
        }
    }
    class FlagHandler implements ClickHandler {
        // !!OVERRIDE!!
        @Override
        public void onTileClicked(MineTile btn, MinesweeperGUI game) {
            int r = btn.r; 
            int c = btn.c;

            if (game.getRevealed()[r][c]) return;

            if ("\uD83D\uDEA9".equals(btn.getText())) {
                btn.setText("");
                btn.setForeground(null);
                game.incrementFlags();
            } else {
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                btn.setText("\uD83D\uDEA9");
                btn.setForeground(Color.RED);
                game.decrementFlags();
            }
            game.updateFlagLabel();
        }
    }

    // !!GENERIC CLASS!!
    class GridWrapper<T> {
        private T[][] grid;
        private int rows;
        private int cols;

        public GridWrapper(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            this.grid = (T[][]) new Object[rows][cols];
        }
        public void set(int r, int c, T value) {
            if (isValid(r, c)) grid[r][c] = value;
        }
        public T get(int r, int c) {
            if (isValid(r, c)) return grid[r][c];
            return null;
        }
        private boolean isValid(int r, int c) {
            return r >= 0 && r < rows && c >= 0 && c < cols;
        }
    }

    public static void main(String[] args) {
        new MinesweeperGUI();
    }
}