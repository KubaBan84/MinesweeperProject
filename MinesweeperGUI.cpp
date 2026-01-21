#include <jni.h>
#include "MinesweeperGUI.h"
#include <ctime>
#include <cstdlib>
#include <vector>
#include <fstream>
#include <iostream>
#include <windows.h>
#include <algorithm>
// Global grid to store map data
enum class Celltype { EMPTY = 0, BOMB = -1 };
class cell {
    protected:
        int column;
        int row;
    public:
        cell(int r, int c){
            row = r;
            column = c;
        }
        int getRow() { return row; }
        int getColumn() { return column; }

        virtual ~cell() = default;
        virtual Celltype getType() const = 0;
        virtual int getValue() const { return 0; }
        virtual void incrementBombCount() {  }
};
class emptyCell : public cell {
    private:
        int neighboringBombs;
    public:
        emptyCell(int r, int c) : cell(r, c),neighboringBombs(0) {}
        void incrementBombCount() { neighboringBombs++; }
        int getValue() const override { return neighboringBombs; }
        Celltype getType() const override { return Celltype::EMPTY; }
};
class bombCell : public cell {
    public:
        bombCell(int r, int c) : cell(r, c) {}
        Celltype getType() const override { return Celltype::BOMB; }
        int getValue() const override { return -1; }
};

template <typename T>
class Grid {
private:
    T** data;
    int rows;
    int cols;

public:
    // Constructor: Allocates the 2D array
    Grid(int r, int c) : rows(r), cols(c) {// Allocate memory both will be always 10 in this project but just for generic purpose we let it be variable
        data = new T*[rows];
        for(int i = 0; i < rows; i++) {
            data[i] = new T[cols];
        }
    }

    // Destructor: Cleans up the array memory
    ~Grid() {
        for(int i = 0; i < rows; i++) {
            delete[] data[i];
        }
        delete[] data;
    }

    // Generic method to get value
    T& get(int r, int c) {
        return data[r][c];
    }

    // Generic method to set value
    void set(int r, int c, T val) {
        data[r][c] = val;
    }
};

class gameMap {
    private:
        Grid<cell*>* grid;
        int mapsize = 10;
    public:
        gameMap(){
             // Initialize grid (dynamic 2D array)
            grid = new Grid<cell*>(mapsize, mapsize);

            // Fill with empty cells
            for(int i=0; i<10; i++) {
                for(int j=0; j<10; j++) {
                    grid->set(i, j, new emptyCell(i,j));
                }
            }
        }
        ~gameMap(){
            for(int i=0; i<10; i++) {
                for(int j=0; j<10; j++) {
                    delete grid->get(i, j); // Delete the actual objects
                }
            }
            delete grid; // Delete the grid itself
        }
        void generateMap(int bombCount, bool fromfile) {
            std::srand(std::time(0));
            if(fromfile) {// Load from file
                std::ifstream infile;
                infile.open("minesweeper_map.txt");
                if(!infile.is_open()) {
                    MessageBox(
                        NULL,                // Parent window (NULL = Desktop)
                        "file oppen error.", // Message content
                        "Popup Title",       // Title bar text
                        MB_OK | MB_ICONINFORMATION // Buttons and Icon type
                    );
                    return;
                }
                bombCount = 0;
                for(int r=0; r<10; r++) {
                    for(int c=0; c<10; c++) {
                        char val;
                        infile >> val;
                        if(val == 'X') {// Bomb cell
                            delete grid->get(r, c); // Delete the existing emptyCell
                            grid->set(r, c, new bombCell(r,c));
                            bombCount++;
                        }
                    }
                }
                infile.close();

            }
            else{
                // Place bombs
                int placed = 0;
                while(placed < bombCount) {
                    int r = std::rand() % 10;
                    int c = std::rand() % 10;
                    if(grid->get(r,c)->getType() != Celltype::BOMB) {
                        delete grid->get(r, c); // Delete the existing emptyCell
                        grid->set(r, c, new bombCell(r,c));
                        placed++;
                    }
                }
            }
            

            // Calculate numbers
            for(int r=0; r<10; r++) {
                for(int c=0; c<10; c++) {
                    if(grid->get(r,c)->getType() == Celltype::BOMB) continue;
                    int count = 0;
                    for(int i=-1; i<=1; i++) {
                        for(int j=-1; j<=1; j++) {
                            int nr = r + i, nc = c + j;
                            if(nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && grid->get(nr,nc)->getType() == Celltype::BOMB) {
                                grid->get(r,c)->incrementBombCount();
                            }
                        }
                    }
                }
            }
        }
        void floodFill(int r, int c, std::vector<std::pair<int, int>>& results,bool** visited) {
            if (r < 0 || r >= 10 || c < 0 || c >= 10 || visited[r][c] || grid->get(r,c)->getType() == Celltype::BOMB) return;

            visited[r][c] = true;
            results.push_back({r, c});

            // If it's a number > 0, we stop expanding but keep the number revealed
            if (grid->get(r,c)->getValue() != 0) return;

            // Expand in 8 directions
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;
                    floodFill(r + i, c + j, results, visited);
                }
            }
        }
        Grid<cell*>* getGrid() {
            return grid;
        }


};


extern "C" {
    JNIEXPORT jlong JNICALL Java_MinesweeperGUI_setupcpp(JNIEnv *env, jobject obj) {// Initialize game map
        gameMap *game = new gameMap();
        return (jlong)game; // return a pointer
    }

    JNIEXPORT jobjectArray JNICALL Java_MinesweeperGUI_requestMapFromCpp(JNIEnv *env, jobject obj, jint bombs, jlong gamePtr,jboolean fromfile=false) {
        bool fromfilecpp = (fromfile == JNI_TRUE);
        gameMap* game = (gameMap*)gamePtr;// Cast back to gameMap pointer
        game->generateMap(bombs,fromfilecpp);
        Grid<cell*>* grid = game->getGrid();
        jclass intArrayClass = env->FindClass("[I");
        jobjectArray outerArray = env->NewObjectArray(10, intArrayClass, NULL);
        for (int i = 0; i < 10; i++) {
            jintArray innerArray = env->NewIntArray(10);
            jint temp[10];
            for(int j = 0; j <10; j++) {
                temp[j] = (jint)grid->get(i,j)->getValue();
            }
            env->SetIntArrayRegion(innerArray, 0, 10, temp);
            env->SetObjectArrayElement(outerArray, i, innerArray);
            env->DeleteLocalRef(innerArray);
        }
        return outerArray;
    }

    // New function to get the "flood fill" list
    JNIEXPORT jobjectArray JNICALL Java_MinesweeperGUI_getFloodFill(JNIEnv *env, jobject obj, jint r, jint c, jlong gamePtr) {
        gameMap* game = (gameMap*)gamePtr;// Cast back to gameMap pointer
        std::vector<std::pair<int, int>> results;
        bool **visited = new bool*[10];
            for(int i = 0; i < 10; i++) {
                visited[i] = new bool[10];
                for(int j = 0; j < 10; j++) {
                    visited[i][j] = false;
                }
            }
        game->floodFill(r, c,results,visited);

        std::sort(results.begin(), results.end(), [](const std::pair<int, int>& a, const std::pair<int, int>& b) {
            if (a.first != b.first)
                return a.first < b.first; // Sort by Row first
            return a.second < b.second;   // Then by Column
        });

        jclass intArrayClass = env->FindClass("[I");
        jobjectArray outerArray = env->NewObjectArray(results.size(), intArrayClass, NULL);

        for (int i = 0; i < results.size(); i++) {
            jintArray innerArray = env->NewIntArray(2);
            jint coords[2] = { (jint)results[i].first, (jint)results[i].second };
            env->SetIntArrayRegion(innerArray, 0, 2, coords);
            env->SetObjectArrayElement(outerArray, i, innerArray);
            env->DeleteLocalRef(innerArray);
        }
        for(int i = 0; i < 10; i++) {// Clean up visited array
            delete[] visited[i];
        }
        delete[] visited;
        return outerArray;
    }
    JNIEXPORT void JNICALL Java_MinesweeperGUI_endofgamecpp(JNIEnv *env, jobject obj, jlong gamePtr) {
        gameMap* game = (gameMap*)gamePtr;
        if(game == nullptr) return;
        delete game; // Clean up(Destructor call)
    }
}