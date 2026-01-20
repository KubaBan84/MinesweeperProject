import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MinesweeperGUI extends JFrame {
    private JButton[][] buttons = new JButton[10][10];
    private int[][] backendGrid; // Data received from C++
    
    public MinesweeperGUI() {
        setTitle("C++/Java Minesweeper");
        setSize(500, 500);
        setLayout(new BorderLayout());

        // 1. Bomb Selection Range
        String input = JOptionPane.showInputDialog(this, "Enter number of bombs (1-50):");
        int bombCount = Integer.parseInt(input);
        bombCount = Math.max(1, Math.min(50, bombCount));

        // 2. Call C++ Logic (Simulated via Native Method)
        backendGrid = requestMapFromCpp(bombCount);

        // 3. Build Grid
        JPanel gridPanel = new JPanel(new GridLayout(10, 10));
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                buttons[r][c] = new JButton();
                int finalR = r;
                int finalC = c;
                buttons[r][c].addActionListener(e -> revealCell(finalR, finalC));
                gridPanel.add(buttons[r][c]);
            }
        }
        
        add(gridPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void revealCell(int r, int c) {
        int val = backendGrid[r][c];
        if (val == -1) {
            buttons[r][c].setText("💣");
            buttons[r][c].setBackground(Color.RED);
            JOptionPane.showMessageDialog(this, "Game Over!");
        } else {
            buttons[r][c].setText(val == 0 ? "" : String.valueOf(val));
            buttons[r][c].setEnabled(false);
            buttons[r][c].setBackground(Color.LIGHT_GRAY);
        }
    }

    // This represents the JNI bridge
    public native int[][] requestMapFromCpp(int bombs);

    public static void main(String[] args) {
    try {
        System.loadLibrary("minesweeper");
    } catch (UnsatisfiedLinkError e) {
        System.err.println("Native code library failed to load.\n" + e);
        System.exit(1);
    }
    new MinesweeperGUI();
}
}