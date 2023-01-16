package v3pathfind;

import battlecode.common.*;

public abstract class Robot {

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException {
        MapLocation curLoc = rc.getLocation();
        if (!rc.isMovementReady() || curLoc.equals(dest)) return;

//        if(!BFS.moveTo(rc, dest) && !BugNav.moveTo(rc, dest)) {
        if(!BFS.moveTo(rc, dest)) {
            Direction randomDir = Random.nextDir();
            rc.setIndicatorString("bump");
            for(int i = 0; i < 8; ++i) {
                if(rc.canMove(randomDir)) {
                    rc.move(randomDir);
                    return;
                }
                randomDir = randomDir.rotateRight();
            }
        }
    }

//    public static void()
}
