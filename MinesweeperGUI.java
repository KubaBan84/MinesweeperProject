import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MinesweeperGUI extends JFrame {
    private JButton[][] buttons = new JButton[10][10];
    private int[][] backendGrid;
    private boolean[][] revealed = new boolean[10][10];
    private boolean flagMode = false;
    private int totalBombs = 0;
    private int revealedCount = 0;

    public native int[][] requestMapFromCpp(int bombs);
    public native int[][] getFloodFill(int r, int c);

    static {
        System.loadLibrary("minesweeper");
    }

    public MinesweeperGUI() {
        setupGame();
    }

    private void setupGame() {
        getContentPane().removeAll();
        revealed = new boolean[10][10];
        revealedCount = 0;

        setTitle("Minesweeper - C++ Logic / Java GUI");
        setSize(600, 700);
        setLayout(new BorderLayout());

        // Top UI: Bomb Selection and Flag Toggle
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        String input = JOptionPane.showInputDialog(this, "Enter number of bombs (1-50):");
        totalBombs = (input == null) ? 10 : Integer.parseInt(input);
        backendGrid = requestMapFromCpp(totalBombs);

        JToggleButton flagToggle = new JToggleButton("MODE: REVEAL");
        flagToggle.setFont(new Font("Arial", Font.BOLD, 14));
        flagToggle.addActionListener(e -> {
            flagMode = flagToggle.isSelected();
            flagToggle.setText(flagMode ? "MODE: FLAGGING (🚩)" : "MODE: REVEAL");
            flagToggle.setForeground(flagMode ? Color.RED : Color.BLUE);
        });

        topPanel.add(new JLabel("Goal: Reveal all safe spaces. Total Bombs: " + totalBombs, SwingConstants.CENTER));
        topPanel.add(flagToggle);
        add(topPanel, BorderLayout.NORTH);

        // Grid Panel
        JPanel gridPanel = new JPanel(new GridLayout(10, 10));
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                buttons[r][c] = new JButton();
                buttons[r][c].setFont(new Font("Arial", Font.BOLD, 16));
                int finalR = r;
                int finalC = c;
                buttons[r][c].addActionListener(e -> handleClick(finalR, finalC));
                gridPanel.add(buttons[r][c]);
            }
        }
        add(gridPanel, BorderLayout.CENTER);
        
        revalidate();
        repaint();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void handleClick(int r, int c) {
        if (revealed[r][c]) return;

        if (flagMode) {
            // Explicitly check for the flag text
            if ("🚩".equals(buttons[r][c].getText())) {
                buttons[r][c].setText("");
                buttons[r][c].setForeground(null); // Reset color
            } else {
                // Set a font that is guaranteed to have emojis on Windows
                buttons[r][c].setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                buttons[r][c].setText("🚩");
                buttons[r][c].setForeground(Color.RED);
            }
            return;
        }

        // ... rest of your logic for clicking bombs or numbers ...
        if (backendGrid[r][c] == -1) {
            revealAllBombs();
            endGame("💣 YOU LOSE! You hit a bomb.");
        } else {
            int[][] toReveal = getFloodFill(r, c);
            for (int[] coord : toReveal) {
                showCell(coord[0], coord[1]);
            }
            checkWin();
        }
    }

    private void showCell(int r, int c) {
        if (revealed[r][c]) return;
        
        revealed[r][c] = true;
        revealedCount++;
        int val = backendGrid[r][c];
        
        buttons[r][c].setEnabled(false);
        buttons[r][c].setBackground(Color.LIGHT_GRAY);
        buttons[r][c].setText(val == 0 ? "" : String.valueOf(val));
        
        // Coloring for numbers
        if (val == 1) buttons[r][c].setForeground(Color.BLUE);
        else if (val == 2) buttons[r][c].setForeground(new Color(0, 128, 0));
        else if (val >= 3) buttons[r][c].setForeground(Color.RED);
    }

    private void checkWin() {
        // Win if all non-bomb tiles are revealed
        if (revealedCount == (100 - totalBombs)) {
            endGame("🏆 GAME OVER - YOU WIN! You found all safe spots.");
        }
    }

    private void revealAllBombs() {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (backendGrid[r][c] == -1) {
                    // Ensure font supports emojis
                    buttons[r][c].setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                    buttons[r][c].setText("💣");
                    buttons[r][c].setBackground(Color.RED);
                    buttons[r][c].setForeground(Color.BLACK);
                    
                    // On some systems, we must disable the button to stop the 
                    // "hover" effect from hiding the icon
                    buttons[r][c].setEnabled(false);
                }
            }
        }
    }

    private void endGame(String message) {
        int response = JOptionPane.showConfirmDialog(this, message + "\nPlay again?", "Game Over", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            setupGame();
        } else {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        new MinesweeperGUI();
    }
}