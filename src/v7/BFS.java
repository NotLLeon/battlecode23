package v7;

import battlecode.common.*;


/**
 * "BFS"
 */
public class BFS {

    static Direction bestDir;

    // Assumes hasDir is true
    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
        MapLocation curLoc = rc.getLocation();
        bestDir = curLoc.directionTo(dest);
        return getDetourDir(rc);
    }

    // TODO: ADD MORE
    /**
     * 0  - optimal direction
     * n  - optimal direction rotated right n times
     * -n - optimal direction rotated left n times
     */
    private static final int[][] detour1 = {{0}};
    private static final int[][] detour2 = {{1, -1}};
    private static final int[][] detour3 = {{1, 0, -1}, {-2, 0, 2}};
    private static final int[][] detour4 = {
            {1, 0, 0, -1},
            {1, 1, -1, -1},
            {2, 0, 0, -2},
    };
    private static final int[][] detour5 = {
            {1, 0, 0, 0, -1},
            {2, 0, 0, 0, -2},
            {1, 1, 0, -1, -1},
            {2, 2, 0, -2, -2},
            {3, 1, 0, -1, -2},
            {3, 1, 0, -1, -1},
    };

    private static final int[][] detour6 = {
            {1, 0, 0, 0, 0, -1},
            {2, 0, 0, 0, 0, -2},
            {1, 1, 0, 0, -1, -1},
            {2, 2, 0, 0, -2, -2},
            {1, 1, 1, -1, -1, -1},
    };

    private static Direction getDetourDir(RobotController rc) throws GameActionException {

        MapLocation curLoc = rc.getLocation();
        boolean works;
        Direction firstDir ;
        Direction curDir;

        for (int[] dirs : detour1) {
            MapLocation loc = curLoc;
            works = true;
            firstDir = null;
            for (int r : dirs) {
                curDir = rotateInt(bestDir, r);
                loc = loc.add(curDir);
                if (!isMoveable(rc, loc, curDir, firstDir == null)) {
                    works = false;
                    break;
                }
                if (firstDir == null) firstDir = curDir;
            }
            if (works) return firstDir;
        }

        for (int[] dirs : detour2) {
            MapLocation loc = curLoc;
            works = true;
            firstDir = null;
            for (int r : dirs) {
                curDir = rotateInt(bestDir, r);
                loc = loc.add(curDir);
                if (!isMoveable(rc, loc, curDir, firstDir == null)) {
                    works = false;
                    break;
                }
                if (firstDir == null) firstDir = curDir;
            }
            if (works) return firstDir;
        }

        for (int[] dirs : detour3) {
            MapLocation loc = curLoc;
            works = true;
            firstDir = null;
            for (int r : dirs) {
                curDir = rotateInt(bestDir, r);
                loc = loc.add(curDir);
                if (!isMoveable(rc, loc, curDir, firstDir == null)) {
                    works = false;
                    break;
                }
                if (firstDir == null) firstDir = curDir;
            }
            if (works) return firstDir;
        }

        for (int[] dirs : detour4) {
            MapLocation loc = curLoc;
            works = true;
            firstDir = null;
            for (int r : dirs) {
                curDir = rotateInt(bestDir, r);
                loc = loc.add(curDir);
                if (!isMoveable(rc, loc, curDir, firstDir == null)) {
                    works = false;
                    break;
                }
                if (firstDir == null) firstDir = curDir;
            }
            if (works) return firstDir;
        }

        for (int[] dirs : detour5) {
            MapLocation loc = curLoc;
            works = true;
            firstDir = null;
            for (int r : dirs) {
                curDir = rotateInt(bestDir, r);
                loc = loc.add(curDir);
                if (!isMoveable(rc, loc, curDir, firstDir == null)) {
                    works = false;
                    break;
                }
                if (firstDir == null) firstDir = curDir;
            }
            if (works) return firstDir;
        }

        for (int[] dirs : detour6) {
            MapLocation loc = curLoc;
            works = true;
            firstDir = null;
            for (int r : dirs) {
                curDir = rotateInt(bestDir, r);
                loc = loc.add(curDir);
                if (!isMoveable(rc, loc, curDir, firstDir == null)) {
                    works = false;
                    break;
                }
                if (firstDir == null) firstDir = curDir;
            }
            if (works) return firstDir;
        }
        return Direction.CENTER;
    }

    private static Direction rotateInt(Direction dir, int rotate) {
        switch(rotate) {
            case 0: return dir;
            case 1: return dir.rotateRight();
            case -1: return dir.rotateLeft();
            case 2: return dir.rotateRight().rotateRight();
            case -2: return dir.rotateLeft().rotateLeft();
            case 3: return dir.rotateLeft().opposite();
            case -3: return dir.rotateRight().opposite();
            default: return dir.opposite();
        }
    }

    private static boolean isMoveable(RobotController rc, MapLocation loc, Direction dir, boolean firstMove) throws GameActionException {
        if(!rc.canSenseLocation(loc)) return false;
        MapInfo info = rc.senseMapInfo(loc);
        return rc.onTheMap(loc) && info.isPassable()
                && (!firstMove || !rc.canSenseRobotAtLocation(loc))
                && (goodCurrent(info.getCurrentDirection(), dir));
    }

    private static boolean goodCurrent(Direction current, Direction dir) {
        return current == Direction.CENTER
                || current == dir
                || current == dir.rotateLeft()
                || current == dir.rotateRight();
    }
//    private static int getVisionRadius(RobotController rc) throws GameActionException {
//        MapLocation loc = rc.getLocation();
//        if(rc.senseCloud(loc)) return GameConstants.CLOUD_VISION_RADIUS_SQUARED;
//        return rc.getType().visionRadiusSquared;
//    }


}
