#include <iostream>
#include <vector>
#include <ctime>
#include <algorithm>

// This represents the logic that Java will call
class MinesweeperLogic {
public:
    int grid[10][10];

    void generateMap(int bombCount) {
        // Initialize grid with 0
        for(int i=0; i<10; i++)
            for(int j=0; j<10; j++) grid[i][j] = 0;

        // Random bomb placement (-1 represents a bomb)
        int placed = 0;
        while(placed < bombCount) {
            int r = rand() % 10;
            int c = rand() % 10;
            if(grid[r][c] != -1) {
                grid[r][c] = -1;
                placed++;
            }
        }

        // Calculate neighbor numbers
        for(int r=0; r<10; r++) {
            for(int c=0; c<10; c++) {
                if(grid[r][c] == -1) continue;
                grid[r][c] = countAdjacentBombs(r, c);
            }
        }
    }

private:
    int countAdjacentBombs(int r, int c) {
        int count = 0;
        for(int i=-1; i<=1; i++) {
            for(int j=-1; j<=1; j++) {
                int nr = r + i, nc = c + j;
                if(nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && grid[nr][nc] == -1) {
                    count++;
                }
            }
        }
        return count;
    }
};