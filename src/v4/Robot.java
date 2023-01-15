package v4;

import battlecode.common.*;

public abstract class Robot {

    static boolean obstacle = false;
    static int dis = 0;
    static boolean infSlope = false;
    static double slope = 0;
    static Direction traceDir = null;
    static MapLocation collisionLoc = null;

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
    public static boolean moveTo(RobotController rc, MapLocation dest) throws GameActionException {
        // Bug2
        MapLocation curLoc = rc.getLocation();
        if(collisionLoc != null) rc.setIndicatorString("" + slope + " "+ collisionLoc + " " + curLoc + " " +lineEval(curLoc.x)+ " "+ (lineEval(curLoc.x) - curLoc.y));
        else rc.setIndicatorString("" + slope + " "+ collisionLoc + " " + curLoc);
        Direction dir = curLoc.directionTo(dest);
        if (!rc.isMovementReady() || curLoc.equals(dest)) {
            return true;
        }

        Direction nextDir = null;
        if(!obstacle) {
            if (rc.canMove(dir)) {
                rc.move(dir);
                return true;
            }

            obstacle = true;
            computeSlope(curLoc, dest);
            rc.setIndicatorString("calc line");
            traceDir = dir;
            dis = curLoc.distanceSquaredTo(dest);
            collisionLoc = curLoc;
            nextDir = dir.rotateLeft();
        } else {
            int curDis = curLoc.distanceSquaredTo(dest);
//            rc.setIndicatorString(dis + " " + curDis + " " +onLine(curLoc) + " " + (lineEval(curLoc.x) - curLoc.y) + " " + collisionLoc);
//            if(onLine(curLoc) && curDis < dis && rc.canMove(dir)) {
            if(onLine(curLoc) && rc.canMove(dir)) {

//                rc.setIndicatorString("obstacle cleared " + dir);
                obstacle = false;
                rc.move(dir);
                return true;
            }

            if(curLoc == collisionLoc) return false;
            nextDir = traceDir.rotateRight().rotateRight();
        }
        for(int i = 0; i < 8; ++i) {
            if(rc.canMove(nextDir)) {
                traceDir = nextDir;
                rc.move(traceDir);
                return true;
            } else {
                nextDir = nextDir.rotateLeft();
            }
        }
        return false;
    }
}
