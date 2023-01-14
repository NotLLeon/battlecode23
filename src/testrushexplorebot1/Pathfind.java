package testrushexplorebot1;

import battlecode.common.*;

public class Pathfind {

    static Direction getDir(RobotController rc, MapLocation dest) {
        MapLocation curLoc = rc.getLocation();
        Direction bestDir = curLoc.directionTo(dest);
        if(rc.canMove(bestDir)) return bestDir;

        // If optimal direction is not movable, move randomly
        Direction dir = Random.nextDir();
        for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) if (!rc.canMove(dir)) dir = Random.nextDir();
        return dir;
    }
}
