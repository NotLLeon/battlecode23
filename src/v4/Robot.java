package v4;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Robot {

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException { {
        moveTo(rc, dest, false,-1);
    }}

    public static void moveToAdj(RobotController rc, MapLocation dest) throws GameActionException { {
        moveTo(rc, dest, true,-1);
    }}

    public static void moveToRadius(RobotController rc, MapLocation dest, int radius) throws GameActionException { {
        moveTo(rc, dest, false,radius);
    }}

    private static void moveTo(RobotController rc, MapLocation dest, boolean adj, int radius) throws GameActionException {
        MapLocation curLoc = rc.getLocation();

        if (!rc.isMovementReady()
                || curLoc.equals(dest)
                || (adj && curLoc.isAdjacentTo(dest))
                || (radius != -1 && curLoc.distanceSquaredTo(dest) <= radius)) return;

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
            };
            rc.setIndicatorString("bump");
            for (Direction direction : dirs) {
                if (rc.canMove(direction)) {
                    rc.move(direction);
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

    // true - not sure, false - no
    static boolean isReachable(RobotController rc, MapLocation dest) {
        return BugNav.isReachable(dest);
    }
}
