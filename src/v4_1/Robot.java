package v4_1;

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

    static int prevlocIdx = 0;
    static int numMoves = 0;
    static MapLocation [] prevLocs = new MapLocation[Constants.NUM_TRACKED_LOCATIONS];


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
            //rc.setIndicatorString("bump");
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
    static MapLocation findClosestLoc (RobotController rc, MapLocation [] locs) {
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

    public static Direction exploreAwayFromLoc(RobotController rc, MapLocation loc) throws GameActionException {
        Direction locDir = rc.getLocation().directionTo(loc);

        // 8 Directions, init all weight 1
        int weights [] = {1, 1, 1, 1, 1, 1, 1, 1};

        Direction dir;
        if (locDir != Direction.CENTER) {
            // Directions pointing towards loc lowest weight
            // Directions away from loc higher weight
            weights[Random.getDirectionOrderNum(locDir.opposite().rotateLeft())] *= Constants.MID_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.opposite().rotateRight())] *= Constants.MID_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.opposite())] *= Constants.MID_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.rotateLeft().rotateLeft())] *= Constants.MID_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.rotateRight().rotateRight())] *= Constants.MID_WEIGHT_DIRECTION;
        }
        dir = Random.nextDirWeighted(weights, Constants.TOTAL_WEIGHT_DIRECTIONS);
        for(int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
            if (rc.canMove(dir)) {
                break;
            }
            dir = Random.nextDirWeighted(weights, Constants.TOTAL_WEIGHT_DIRECTIONS);
        }
        return dir;
    }

    public static void exploreNewArea(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;

        if (prevLocs[0] == null) {
            Direction dir = Random.nextDir();
            for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
                if (rc.canMove(dir)) {
                    if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
                        prevLocs[prevlocIdx] = rc.getLocation();
                        prevlocIdx++;
                    }
                    rc.move(dir);
                    break;
                }
                dir = Random.nextDir();
            }

        } else {
            Direction dir = exploreAwayFromLoc(rc, findClosestLoc(rc, prevLocs));
            for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
                if (rc.canMove(dir)) {
                    if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
                        prevLocs[prevlocIdx] = rc.getLocation();
                        prevlocIdx = (prevlocIdx + 1) % Constants.NUM_TRACKED_LOCATIONS;
                    }
                    rc.move(dir);
                    break;
                }
                dir = exploreAwayFromLoc(rc, findClosestLoc(rc, prevLocs));
            }
        }
    }
}
