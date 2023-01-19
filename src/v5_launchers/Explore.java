package v5_launchers;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Explore {
    static int prevlocIdx = 0;
    static int numMoves = 0;
    static MapLocation[] prevLocs = new MapLocation[Constants.NUM_TRACKED_LOCATIONS];

    public static Direction exploreAwayFromLoc(RobotController rc, MapLocation loc) throws GameActionException {
        Direction locDir = rc.getLocation().directionTo(loc);

        // 8 Directions, init all weight 1
        int weights [] = {1, 1, 1, 1, 1, 1, 1, 1};

        Direction dir;
        if (locDir != Direction.CENTER) {
            // Directions pointing towards loc lowest weight
            // Directions away from loc higher weight
            weights[Random.getDirectionOrderNum(locDir.opposite().rotateLeft())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.opposite().rotateRight())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.opposite())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.rotateLeft().rotateLeft())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.rotateRight().rotateRight())] *= Constants.HIGH_WEIGHT_DIRECTION;
        }
        int totalWeight = 0;
        for (int w : weights) totalWeight += w;
        dir = Random.nextDirWeighted(weights, totalWeight);

        for(int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
            if (rc.canMove(dir)) {
                break;
            }
            dir = Random.nextDirWeighted(weights, totalWeight);
        }
        return dir;
    }

    public static void exploreNewArea(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;

        int numClosePrevLocs = 0;
        for (MapLocation loc : prevLocs) {
            if (loc != null && rc.canSenseLocation(loc)) numClosePrevLocs++;
        }

        if (numClosePrevLocs == Constants.NUM_TRACKED_LOCATIONS) {
//            rc.setIndicatorString("TRAPPED");
            numMoves = 0;
        }

        if (prevLocs[0] == null) {
            Direction dir = Random.nextDir();
            for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
                if (rc.canMove(dir)) {
                    if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
                        prevLocs[prevlocIdx] = rc.getLocation();
                        prevlocIdx++;
                    }
                    rc.move(dir);
                    // for empty carriers
                    exploreNewArea(rc);
                    break;
                }
                dir = Random.nextDir();
            }

        } else {

            Direction dir = exploreAwayFromLoc(rc, getAvgLocation(prevLocs));

            for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
                if (rc.canMove(dir)) {
                    if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
                        prevLocs[prevlocIdx] = rc.getLocation();
                        prevlocIdx = (prevlocIdx + 1) % Constants.NUM_TRACKED_LOCATIONS;
                    }
                    rc.move(dir);
                    exploreNewArea(rc);
                    break;
                }
                dir = exploreAwayFromLoc(rc, getAvgLocation(prevLocs));
            }
        }
    }


    static MapLocation getAvgLocation(MapLocation [] locs) {
        int len = locs.length;
        int x = 0;
        int y = 0;
        for (MapLocation loc : locs) {
            if (loc == null) {
                len--;
                continue;
            }
            x += loc.x;
            y += loc.y;
        }
        return new MapLocation(x / len, y / len);
    }
}
