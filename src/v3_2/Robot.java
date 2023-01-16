package v3_2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
    static MapLocation getClosestHQ(RobotController rc) throws GameActionException {
        MapLocation current_location = rc.getLocation();
        int t = 0;
        int min_loc = 0;
        int min_dist = 10000;
        for (int i = 0; i < 8 && (t = rc.readSharedArray(i)) != 0; i++) {
            int x = (t-1)%rc.getMapWidth();
            int y = (t-1)/rc.getMapWidth();
            int cur_dist = (x-current_location.x) * (x-current_location.x) + (y-current_location.y)*(y-current_location.y);
            if (cur_dist < min_dist) {
                min_dist = cur_dist;
                min_loc = (t-1);
            }
        }
        return new MapLocation(min_loc%rc.getMapWidth(), min_loc/rc.getMapWidth());
    }
}
