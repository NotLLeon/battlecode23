package smartCollectors;

import battlecode.common.*;

import java.util.Random;

public class Pathfind {

    static final Random rng = new Random(6147);

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

    static Direction getDir(RobotController rc, MapLocation dest) {
        MapLocation curLoc = rc.getLocation();
        Direction bestDir = curLoc.directionTo(dest);
        if(rc.canMove(bestDir)) return bestDir;

        // If optimal direction is not movable, move randomly
        Direction dir = Direction.CENTER;
        for (int i = 0; i < 10; i++) {
            if (rc.canMove(dir)) {
                dir = directions[rng.nextInt(directions.length)];
                break;
            }
        }
        return dir;
    }
}
