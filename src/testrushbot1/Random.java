package testrushbot1;

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

    static Direction nextDir() {
        return directions[rng.nextInt(directions.length)];
    }
    static int nextInt(int bound) {
        return rng.nextInt(bound);
    }

    static boolean nextBoolean() {
         return rng.nextBoolean();
    }

}
