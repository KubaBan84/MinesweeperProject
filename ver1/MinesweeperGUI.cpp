#include <jni.h>
#include "MinesweeperGUI.h"
#include <ctime>
#include <cstdlib>
#include <vector>

// Global grid to store map data
int grid[10][10];

// Logic to generate the map
void generateMap(int bombCount) {
    std::srand(std::time(0));
    // Clear grid
    for(int i=0; i<10; i++) {
        for(int j=0; j<10; j++) {
            grid[i][j] = 0;
        }
    }

    // Place bombs
    int placed = 0;
    while(placed < bombCount) {
        int r = std::rand() % 10;
        int c = std::rand() % 10;
        if(grid[r][c] != -1) {
            grid[r][c] = -1;
            placed++;
        }
    }

    // Calculate numbers
    for(int r=0; r<10; r++) {
        for(int c=0; c<10; c++) {
            if(grid[r][c] == -1) continue;
            int count = 0;
            for(int i=-1; i<=1; i++) {
                for(int j=-1; j<=1; j++) {
                    int nr = r + i, nc = c + j;
                    if(nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && grid[nr][nc] == -1) {
                        count++;
                    }
                }
            }
            grid[r][c] = count;
        }
    }
}

// JNI Linker - ONLY ONE DEFINITION HERE
extern "C" {
    // IMPORTANT: Ensure this name matches the header file exactly!
    JNIEXPORT jobjectArray JNICALL Java_MinesweeperGUI_requestMapFromCpp(JNIEnv *env, jobject obj, jint bombs) {
        generateMap(bombs);

        jclass intArrayClass = env->FindClass("[I");
        jobjectArray outerArray = env->NewObjectArray(10, intArrayClass, NULL);

        for (int i = 0; i < 10; i++) {
            jintArray innerArray = env->NewIntArray(10);
            env->SetIntArrayRegion(innerArray, 0, 10, (jint*)grid[i]);
            env->SetObjectArrayElement(outerArray, i, innerArray);
            env->DeleteLocalRef(innerArray);
        }
        return outerArray;
    }
}