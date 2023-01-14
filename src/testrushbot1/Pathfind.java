package testrushbot1;

import battlecode.common.*;

public class Pathfind {

    static Direction getDir(RobotController rc, MapLocation dest) {
        if(!rc.isMovementReady()) return Direction.CENTER;

        MapLocation curLoc = rc.getLocation();
        Direction bestDir = curLoc.directionTo(dest);
        if(rc.canMove(bestDir)) return bestDir;
        else if(bestDir == Direction.CENTER || curLoc.isAdjacentTo(dest)) {
            return Direction.CENTER;
        }

        // If optimal direction is not movable, move randomly
        // TODO: sample without replacement
        Direction dir = Random.nextDir();
        while(!rc.canMove(dir)) dir = Random.nextDir();
        return dir;
    }

}
