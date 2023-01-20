package v5;

import battlecode.common.*;

public class Explore {
    static int prevlocIdx = 0;
    static int numMoves = 0;
    static MapLocation[] prevLocs = new MapLocation[Constants.NUM_TRACKED_LOCATIONS];

    public static Direction exploreAwayFromLoc(RobotController rc, MapLocation loc) {
        Direction locDir = rc.getLocation().directionTo(loc);

        // 8 Directions, init all weight 1
        int[] weights = {1, 1, 1, 1, 1, 1, 1, 1};

        if (locDir != Direction.CENTER) {
            // Directions pointing towards loc lowest weight
            // Directions away from loc higher weight
            weights[Random.getDirectionOrderNum(locDir.opposite().rotateLeft())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.opposite().rotateRight())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.opposite())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.rotateLeft().rotateLeft())] *= Constants.HIGH_WEIGHT_DIRECTION;
            weights[Random.getDirectionOrderNum(locDir.rotateRight().rotateRight())] *= Constants.HIGH_WEIGHT_DIRECTION;
        }
        for(int i = 0; i < 8; ++i) {
            Direction tmp = Random.directions[i];
            if(!rc.canMove(tmp)) weights[i] = 0;
        }
        int totalWeight = 0;
        for (int w : weights) totalWeight += w;
        if(totalWeight == 0) return Direction.CENTER;
        return Random.nextDirWeighted(weights, totalWeight);
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
            for (int i = 0; i < 8; ++i) {
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
                dir = dir.rotateLeft();
            }

        } else {

//            MapLocation avgLoc = getAvgLocation(prevLocs);
            Direction dir = exploreAwayFromLoc(rc, getAvgLocation(prevLocs));
            if(dir != Direction.CENTER) {
                if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
                    prevLocs[prevlocIdx] = rc.getLocation();
                    prevlocIdx = (prevlocIdx + 1) % Constants.NUM_TRACKED_LOCATIONS;
                }
                rc.move(dir);
                exploreNewArea(rc);
            }
//            for (int i = 0; i < Constants.MAX_DIRECTION_SEARCH_ATTEMPTS; ++i) {
//                if (rc.canMove(dir)) {
//                    if ((++numMoves) % Constants.MOVES_TO_TRACK_LOCATION == 0) {
//                        prevLocs[prevlocIdx] = rc.getLocation();
//                        prevlocIdx = (prevlocIdx + 1) % Constants.NUM_TRACKED_LOCATIONS;
//                    }
//                    rc.move(dir);
//                    exploreNewArea(rc);
//                    break;
//                }
//                dir = exploreAwayFromLoc(rc, getAvgLocation(prevLocs));
//            }
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
