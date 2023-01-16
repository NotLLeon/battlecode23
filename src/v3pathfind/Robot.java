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
            Direction [] dirs = {
                    dir.rotateLeft(),
                    dir.rotateRight(),
                    dir.rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft(),
                    dir.rotateLeft().opposite(),
                    dir.rotateRight().opposite(),
                    dir.opposite()
            };
            // TODO: weight differently
            rc.setIndicatorString("bump");
            for(int i = 0; i < 7; ++i) {
                if(rc.canMove(dirs[i])) {
                    rc.move(dirs[i]);
                    return;
                }
            }
        } else {
            rc.move(dir);
        }


    }
}
