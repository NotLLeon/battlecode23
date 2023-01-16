package v3Capped;

import battlecode.common.*;

import java.util.Map;

public abstract class Robot {

    static Direction currentDirection = null;

    static int prevlocIdx = 0;
    static int numMoves = 0;
    static MapLocation [] prevLocs = new MapLocation[Constants.NUM_TRACKED_LOCATIONS];

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException {

        if (rc.getLocation().equals(dest)) {
            return;
        }
//        if (!rc.isActionReady()) {
//            return;
//        }
        Direction d = rc.getLocation().directionTo(dest);
        if (rc.canMove(d)) {
            rc.move(d);
            currentDirection = null; // there is no obstacle we're going around
        } else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
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
            if (!rc.canMove(dir)) {
                dir = Random.nextDirWeighted(weights, Constants.TOTAL_WEIGHT_DIRECTIONS);
            }
        }
        return dir;
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

    public static void exploreNewArea(RobotController rc) throws GameActionException {
        String indicator = "";
        for (MapLocation l : prevLocs) {
            if (l != null) indicator += l + " ";
        }
        if (prevLocs.length == 0) {
            rc.setIndicatorString("MOVING RANDOMLY");
            Direction dir = Random.nextDir();
            for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
                if (!rc.canMove(dir)) {
                    dir = Random.nextDir();
                }
            }
            if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
                prevLocs[prevlocIdx] = rc.getLocation();
                prevlocIdx++;
            }
            rc.move(dir);
        } else {
            rc.setIndicatorString("MOVING AWAY FROM" + indicator);
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
