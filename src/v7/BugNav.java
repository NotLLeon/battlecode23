package v7;

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
    //    private static MapLocation assumedLoc = null;
    private final static int numPastLocs = 15;
    private static MapLocation[] pastLocs = new MapLocation[numPastLocs];
    private static int locIndex = -1;
    private static MapLocation blockingRobot = null;
    private static boolean traceLeft = true;
    private static int turnsTracingObstacle = 0;
    private static boolean changedTrace = false;

    public static boolean tracingObstacle() {
        return obstacle;
    }

    public static void reset() {
        pastLocs = new MapLocation[numPastLocs];
        locIndex = -1;
        curDest = null;
//        assumedLoc = null;
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
//        RobotInfo hasRobot = rc.senseRobotAtLocation(loc);
//        boolean goodRobot = hasRobot == null || hasRobot.getType() == RobotType.HEADQUARTERS;
//        return locInfo.isPassable() && goodCurrent && goodRobot;
        return rc.canMove(dir) && goodCurrent;
    }

    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
//        rc.setIndicatorString(locIndex + Arrays.toString(pastLocs));
//        rc.setIndicatorDot(rc.getLocation(), 256, 0, 0);
        // Bug2
//        rc.setIndicatorString(""+collisionLoc);
        MapLocation curLoc = rc.getLocation();
//        rc.setIndicatorString("" +dest);

        // probably stuck in same place
        if(locIndex < 0 || !curLoc.equals(pastLocs[locIndex])) {
            boolean needsReset = false;
//            rc.setIndicatorString(locIndex + " " + Arrays.toString(pastLocs));
            locIndex = (locIndex+1) % numPastLocs;
            for (int i = 0; i < numPastLocs; ++i) {
                if (i == ((numPastLocs + locIndex - 2) % numPastLocs)) continue;
                MapLocation loc = pastLocs[i];
                if (loc != null && loc.equals(curLoc)) {
                    needsReset = true;
//                    rc.setIndicatorDot(curLoc, 0, 0, 256);
                    break;
                }
            }
            if(needsReset) reset();
            else pastLocs[locIndex] = curLoc;
        }

//        if(!dest.equals(curDest) || !curLoc.equals(assumedLoc)) {
        if(!dest.equals(curDest)) {
            reset();
            curDest = dest;
//            assumedLoc = curLoc;
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
//                assumedLoc = curLoc.add(dir);
                return dir;
            }
            if(rc.canSenseRobotAtLocation(curLoc.add(dir))) blockingRobot = curLoc.add(dir);
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
//            if(blockingRobot != null
//                    && curLoc.isWithinDistanceSquared(blockingRobot, 16)
//                    && !rc.canSenseRobotAtLocation(blockingRobot)) {
//                reset();
//                return getDir(rc, dest);
//            }

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
            if(traceLeft) nextDir = traceDir.rotateRight().rotateRight();
            else nextDir = traceDir.rotateLeft().rotateLeft();
        }
        for(int i = 0; i < 8; ++i) {
            if(isPassable(rc, nextDir)) {
                traceDir = nextDir;
//                assumedLoc = curLoc.add(traceDir);
                return traceDir;
            } else {
                if(traceLeft) nextDir = nextDir.rotateLeft();
                else nextDir = nextDir.rotateRight();
            }
        }
//        rc.setIndicatorString("cent");

        return Direction.CENTER;
    }
}