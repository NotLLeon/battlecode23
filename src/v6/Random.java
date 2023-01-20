package v6;

import battlecode.common.Direction;

public class Random {

    static java.util.Random rng = null;

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

    static void initRandom(int seed) {
        rng = new java.util.Random(seed);

    }

    static int getDirectionOrderNum(Direction dir) {
        for (int i = 0; i < 8; ++i) {
            if (directions[i] == dir) return i;
        }
        return -1;
    }

    static Direction nextDir() {
        return directions[rng.nextInt(directions.length)];
    }

    static Direction nextDirWeighted(int[] weights, int totalWeight) {
        int idx = 0;
        for (int i = nextInt(totalWeight); idx < directions.length - 1; ++idx) {
            i -= weights[idx];
            if (i <= 0 && weights[idx] > 0) break;
        }
        return directions[idx];
    }

    static int nextFloatWeighted(float[] weights) {
        int sz = weights.length;
        float[] bounds = new float[sz];
        int x = 0;
        for (int i = 0; i < sz; i++) {
            x += weights[i];
            bounds[i] = x;
        }
        float random = nextFloat() * bounds[sz-1];
        for (int i = 0; i < sz; i++) {
            if (random <= bounds[i]) return i;
        }
        return -1;
    }


    static int nextInt(int bound) {
        return rng.nextInt(bound);
    }
    static float nextFloat() {return rng.nextFloat();}

    static boolean nextBoolean() {
        return rng.nextBoolean();
    }

}