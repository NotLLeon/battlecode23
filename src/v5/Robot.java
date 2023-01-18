package v5;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Robot {

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException {
        {
            moveTo(rc, dest, false, -1);
        }
    }

    public static void moveToAdj(RobotController rc, MapLocation dest) throws GameActionException {
        {
            moveTo(rc, dest, true, -1);
        }
    }

    public static void moveToRadius(RobotController rc, MapLocation dest, int radius) throws GameActionException {
        {
            moveTo(rc, dest, false, radius);
        }
    }

    private static void moveTo(RobotController rc, MapLocation dest, boolean adj, int radius) throws GameActionException {
        MapLocation curLoc = rc.getLocation();

        if (!rc.isMovementReady()
                || curLoc.equals(dest)
                || (adj && curLoc.isAdjacentTo(dest))
                || (radius != -1 && curLoc.distanceSquaredTo(dest) <= radius)) return;

        Direction dir = BugNav.getDir(rc, dest);
        if (rc.senseRobotAtLocation(curLoc.add(dir)) != null) {
            BugNav.reset();
            Direction[] dirs = {
                    dir.rotateLeft(),
                    dir.rotateRight(),
                    dir.rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft(),
                    dir.rotateLeft().opposite(),
                    dir.rotateRight().opposite(),
                    dir.opposite()
            };
//            rc.setIndicatorString("bump");
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

    static MapLocation findClosestLoc(RobotController rc, MapLocation[] locs) {
        MapLocation curLoc = rc.getLocation();
        int minDist = 10000;
        MapLocation closest = new MapLocation(0, 0);
        for (MapLocation loc : locs) {
            if (loc == null) continue;
            int newDist = curLoc.distanceSquaredTo(loc);
            if (newDist < minDist) {
                minDist = newDist;
                closest = loc;
            }
        }
        return closest;
    }

    static MapLocation getClosestHQ(RobotController rc) throws GameActionException {
        return findClosestLoc(rc, Comms.getHQs(rc));
    }

    /**
     * Returns true if not sure and false if definitely not reachable
     */
    static boolean isReachable(RobotController rc, MapLocation dest) {
        return BugNav.isReachable(dest);
    }

    public static void exploreNewArea(RobotController rc) throws GameActionException {
        Explore.exploreNewArea(rc);
    }
}
