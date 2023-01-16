package v3pathfind;

import battlecode.common.*;

public class BugNav {
    static boolean obstacle = false;
    static int dis = 0;
    static boolean infSlope = false;
    static double slope = 0;
    static Direction traceDir = null;
    static MapLocation collisionLoc = null;

    public static void reset() {
        obstacle = false;
    }

    private static double lineEval(double x) {
        if(infSlope) return collisionLoc.x;
        return slope*(x-collisionLoc.x) + collisionLoc.y;
    }

    private static boolean onLine(MapLocation loc) {
        double delta = lineEval(loc.x) - loc.y;
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
        return rc.senseMapInfo(rc.getLocation().add(dir)).isPassable();
    }

    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
        // Bug2
        MapLocation curLoc = rc.getLocation();
        Direction dir = curLoc.directionTo(dest);

        if(obstacle) rc.setIndicatorString(
                "curLoc: " + curLoc
                        + "dest: "+ dest
                        + "collisionLoc: " + collisionLoc
                        + "lineDiff: " + (lineEval(curLoc.x) - curLoc.y));

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

            if(curLoc == collisionLoc) return Direction.CENTER;
            nextDir = traceDir.rotateLeft().opposite();
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
