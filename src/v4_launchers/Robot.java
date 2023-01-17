package v4_launchers;

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
        MapLocation curLoc = rc.getLocation();
        MapLocation[] hqs = Comms.getHQs(rc);
        int min_dist = 10000;
        MapLocation closestHq = null;
        for(MapLocation hq: hqs) {
            int dis = curLoc.distanceSquaredTo(hq);
            if(dis < min_dist) {
                min_dist = dis;
                closestHq = hq;
            }
        }
        return closestHq;
    }

    /**
     * Returns true if not sure and false if definitely not reachable
     */
    static boolean isReachable(RobotController rc, MapLocation dest) {
        return BugNav.isReachable(dest);
    }
}
