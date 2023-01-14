package v1;

import battlecode.common.*;

public abstract class Robot {

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException {
        if(rc.isMovementReady()) {
            Direction dir = Pathfind.getDir(rc, dest);
            if(dir != Direction.CENTER) rc.move(dir);
        }
    }

}
