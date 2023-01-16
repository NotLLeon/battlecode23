package v3pathfind;

import battlecode.common.*;

public abstract class Robot {

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException {
        MapLocation curLoc = rc.getLocation();

        if (!rc.isMovementReady() || curLoc.equals(dest)) return;

        Direction dir = BugNav.getDir(rc, dest);
//        rc.setIndicatorString(""+dir);
//        Direction best = curLoc.directionTo(dest);
//        if(rc.canMove(best)) {
//            BugNav.reset();
//            rc.move(best);
//            return;
//        }

//        if(!BFS.moveTo(rc, dest) && !BugNav.moveTo(rc, dest)) {
//        Direction bugNavDir = BugNav.getDir(rc, dest);
        if(rc.senseRobotAtLocation(curLoc.add(dir)) != null) {
            BugNav.reset();

            // TODO: weight differently
            Direction randomDir = Random.nextDir();
            rc.setIndicatorString("bump");
            for(int i = 0; i < 8; ++i) {
                if(rc.canMove(randomDir)) {
                    rc.move(randomDir);
                    return;
                }
                randomDir = randomDir.rotateRight();
            }
        } else {
            rc.move(dir);
        }


    }
}
