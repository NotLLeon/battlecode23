package v4;

import battlecode.common.*;

public class BugNav {
    private static boolean obstacle = false;
    private static MapLocation curDest = null;
    private static int dis = 0;
    private static boolean infSlope = false;
    private static double slope = 0;
    private static Direction traceDir = null;
    private static MapLocation collisionLoc = null;
    private static boolean isReachable = true;

    public static void reset() {
        curDest = null;
        obstacle = false;
        isReachable = true;
    }

    public static boolean isReachable(MapLocation dest) {
        return dest != curDest || isReachable;
    }

    private static double lineEval(double x) {
        if(infSlope) return collisionLoc.x;
        return slope*(x-collisionLoc.x) + collisionLoc.y;
    }

    private static boolean onLine(MapLocation loc) {
        double delta = lineEval(loc.x) - loc.y;
//        if(infSlope) return delta == 0;
//        return delta*delta <= slope*slope;
        return delta*delta <= 4;

    }

    private static void computeSlope(MapLocation p1, MapLocation p2) {
        if(p1.x == p2.x) {
            infSlope = true;
            return;
        }
        slope = ((double)(p1.y-p2.y))/(p1.x-p2.x);
        infSlope = false;
    }

    private static boolean isPassable(RobotController rc, Direction dir) throws GameActionException {
        MapLocation loc = rc.getLocation().add(dir);
        return rc.onTheMap(loc) && rc.senseMapInfo(loc).isPassable();
    }

    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
        // Bug2
        if(dest != curDest) {
            reset();
            curDest = dest;
        }
        MapLocation curLoc = rc.getLocation();
        Direction dir = curLoc.directionTo(dest);

//        if(obstacle) rc.setIndicatorString(
//                "curLoc: " + curLoc
//                        + "dest: "+ dest
//                        + "collisionLoc: " + collisionLoc
//                        + "lineDiff: " + (lineEval(curLoc.x) - curLoc.y));

        Direction nextDir = null;
        if(!obstacle) {
            if (isPassable(rc, dir)) {
                rc.setIndicatorString("move: " + dir);
                return dir;
            }

            obstacle = true;
            computeSlope(curLoc, dest);
            rc.setIndicatorString("found obs at: " + curLoc);
            traceDir = dir;
            dis = curLoc.distanceSquaredTo(dest);
            collisionLoc = curLoc;
            nextDir = dir.rotateLeft();
        } else {
            int curDis = curLoc.distanceSquaredTo(dest);
//            rc.setIndicatorString(dis + " " + curDis + " " +onLine(curLoc) + " " + (lineEval(curLoc.x) - curLoc.y) + " " + collisionLoc);
            if(onLine(curLoc) && curDis < dis && isPassable(rc, dir)) {

//                rc.setIndicatorString("obstacle cleared " + dir);
                obstacle = false;
                return dir;
            }

            if(curLoc == collisionLoc) {
                isReachable = false;
                return Direction.CENTER;
            }
            nextDir = traceDir.rotateRight().rotateRight();
        }
        for(int i = 0; i < 8; ++i) {
            if(isPassable(rc, nextDir)) {
                traceDir = nextDir;
                return traceDir;
            } else {
                nextDir = nextDir.rotateLeft();
            }
        }
        return Direction.CENTER;
    }
}
