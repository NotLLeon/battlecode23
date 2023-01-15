package v3;

import battlecode.common.Direction;

public class Random {
    
    static java.util.Random rng = new java.util.Random(6147);

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static int getDirectionOrderNum(Direction dir) {
        for (int i = 0; i < 8; ++i) {
            if (directions[i] == dir) {
                return i;
            }
        }
        return 0;
    }

    static Direction nextDir() {
        return directions[rng.nextInt(directions.length)];
    }

    static Direction nextDirWeighted(int weights[], int totalWeight) {
        int idx = 0;
        for (int i = nextInt(totalWeight); idx < directions.length - 1; ++idx) {
            i -= weights[idx];
            if (i <= 0) break;
        }
        return directions[idx];
    }

    static int nextInt(int bound) {
        return rng.nextInt(bound);
    }

    static boolean nextBoolean() {
         return rng.nextBoolean();
    }

}
