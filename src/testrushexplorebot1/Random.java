package testrushexplorebot1;

import org.apache.commons.lang3.ArrayUtils;

import battlecode.common.Direction;

public class Random {
    
    static final java.util.Random rng = new java.util.Random(6147);

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
        return ArrayUtils.indexOf(directions, dir);
    }

    static Direction nextDir() {
        return directions[rng.nextInt(directions.length)];
    }

    static Direction nextDirWeighted(int weights[], int totalWeight) {
        int idx = 0;
        for (int i = totalWeight * nextInt(6147); idx < directions.length; ++idx) {
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
