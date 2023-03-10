package v5;

import battlecode.common.*;

//import java.util.Arrays;

public class BugNav {
    private static boolean obstacle = false;
    private static MapLocation curDest = null;
    private static int dis = 0;
    private static boolean infSlope = false;
    private static double slope = 0;
    private static Direction traceDir = null;
    private static MapLocation collisionLoc = null;
    private static boolean isReachable = true;
    private static MapLocation assumedLoc = null;

    // should be even, don't ask why
    private final static int numPastLocs = 6;
    private static MapLocation[] pastLocs = new MapLocation[numPastLocs];
    private static int locIndex = -1;

    public static void reset() {
        curDest = null;
        assumedLoc = null;
        collisionLoc = null;
        obstacle = false;
        isReachable = true;
    }

    public static boolean isReachable(MapLocation dest) {
        return !dest.equals(curDest) || isReachable;
    }

    private static double lineEval(double x) {
        return slope*(x-collisionLoc.x) + collisionLoc.y;
    }

    private static boolean onLine(MapLocation loc) {
        if(infSlope) return loc.x == collisionLoc.x;
        if(slope == 0) return loc.y == collisionLoc.y;
        double eval = lineEval(loc.x);
        boolean cond1 = lineEval(loc.x + 0.5) >= eval;
        boolean cond2 = lineEval(loc.x - 0.5) >= eval;
        return (cond1^cond2);
    }

    private static void computeSlope(MapLocation p1, MapLocation p2) {
        if(p1.x == p2.x) {
            infSlope = true;
            slope = 0;
            return;
        }
        slope = ((double)(p1.y-p2.y))/(p1.x-p2.x);
        infSlope = false;
    }

    private static boolean isPassable(RobotController rc, Direction dir) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        if(!rc.onTheMap(loc)) return false;
        MapInfo locInfo = rc.senseMapInfo(loc);
        Direction currentDir = locInfo.getCurrentDirection();
        boolean goodCurrent = currentDir == Direction.CENTER
                || currentDir == dir
                || currentDir == dir.rotateLeft()
                || currentDir == dir.rotateRight();
        RobotInfo hasRobot = rc.senseRobotAtLocation(loc);
        boolean goodRobot = hasRobot == null || hasRobot.getType() == RobotType.HEADQUARTERS;
//        return locInfo.isPassable() && goodCurrent && goodRobot;
        return rc.canMove(dir) && goodCurrent;
    }

    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
        // Bug2
//        rc.setIndicatorString(""+collisionLoc);

        MapLocation curLoc = rc.getLocation();

        // probably stuck in same place
        if(locIndex < 0 || !curLoc.equals(pastLocs[locIndex])) {
//            rc.setIndicatorString(Arrays.toString(pastLocs));
            locIndex = (locIndex+1) % numPastLocs;
            for (int i = 0; i < numPastLocs; ++i) {
                if (i == ((numPastLocs + locIndex - 2) % numPastLocs)) continue;
                MapLocation loc = pastLocs[i];
                if (loc != null && loc.equals(curLoc)) {
                    reset();
//                    rc.setIndicatorDot(curLoc, 0, 0, 256);
                    break;
                }
            }
            pastLocs[locIndex] = curLoc;
        }

        if(!dest.equals(curDest) || !curLoc.equals(assumedLoc)) {
            reset();
            curDest = dest;
            assumedLoc = curLoc;
        }
        Direction dir = curLoc.directionTo(dest);

//        if(obstacle) {
//            rc.setIndicatorLine(collisionLoc, dest, 256, 0, 0);
//            rc.setIndicatorString(
//                    "curLoc: " + curLoc
//                            + "dest: "+ dest
//                            + "slope: " + slope
//                            +"infSlope: " + infSlope);
//        }

        Direction nextDir = null;
        if(!obstacle) {
            if (isPassable(rc, dir)) {
//                rc.setIndicatorString("move: " + dir);
                assumedLoc = curLoc.add(dir);
                return dir;
            }

            obstacle = true;
            computeSlope(curLoc, dest);
//            rc.setIndicatorString("found obs at: " + curLoc);
            traceDir = dir;
            dis = curLoc.distanceSquaredTo(dest);
            collisionLoc = curLoc;
            nextDir = dir;
        } else {
            int curDis = curLoc.distanceSquaredTo(dest);
//            rc.setIndicatorString(dis + " " + curDis + " " +onLine(curLoc) + " " + (lineEval(curLoc.x) - curLoc.y) + " " + collisionLoc);
            if(onLine(curLoc) && curDis < dis) {
//                rc.setIndicatorDot(curLoc, 256, 0, 0);
                reset();
                return getDir(rc, dest);
            }

            if(curLoc.equals(collisionLoc)) {
                reset();
                isReachable = false;
//                rc.setIndicatorString("broke");
                return Direction.CENTER;
            }
            nextDir = traceDir.rotateRight().rotateRight();
        }
        for(int i = 0; i < 8; ++i) {
            if(isPassable(rc, nextDir)) {
                traceDir = nextDir;
                assumedLoc = curLoc.add(traceDir);
                return traceDir;
            } else {
                nextDir = nextDir.rotateLeft();
            }
        }
//        rc.setIndicatorString("cent");

        return Direction.CENTER;
    }
}