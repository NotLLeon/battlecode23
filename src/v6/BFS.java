package v6;

import battlecode.common.*;

import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;

public class BFS {

    static MapLocation[] path;

    // Assumes hasDir is true
    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
        boolean works = getPath(rc, dest);
        if(!works) return Direction.CENTER;
        return path[0].directionTo(path[1]);
    }

    public static boolean getPath(RobotController rc, MapLocation dest) throws GameActionException {
//        int visionRadius = Math.min(16, getVisionRadius(rc));
//        HashSet<MapLocation> opts = new HashSet<MapLocation>();
        if(!rc.canSenseLocation(dest)) return false;

        HashMap<MapLocation, MapLocation> prev = new HashMap<MapLocation, MapLocation>();
        LinkedList<MapLocation> q = new LinkedList<MapLocation>();
        MapLocation curLoc = rc.getLocation();
        Direction dirToDest = curLoc.directionTo(dest);
        Direction[] searchDirs = {
                dirToDest,
                dirToDest.rotateRight(),
                dirToDest.rotateLeft(),
                dirToDest.rotateRight().rotateRight(),
                dirToDest.rotateLeft().rotateLeft()
        };
        q.add(curLoc);
        prev.put(curLoc, null);
//        MapLocation loc = curLoc;
//        for(int i = 0; i*i < visionRadius; ++i) {
//            loc = loc.add(loc.directionTo(dest));
//            opts.add(loc);
//        }

        MapLocation best = null;
        boolean first = true;
        loop1:
        while(!q.isEmpty()) {
            MapLocation cur = q.poll();
            for(Direction searchDir : searchDirs) {
                MapLocation nextLoc = cur.add(searchDir);
                MapInfo info = rc.senseMapInfo(nextLoc);
                if(rc.canSenseLocation(nextLoc)
                        && (!first || rc.canMove(searchDir))
                        && rc.onTheMap(nextLoc)
                        && info.isPassable()
                        && goodCurrent(info.getCurrentDirection(), searchDir)
                        && !prev.containsKey(nextLoc)) {
                    prev.put(nextLoc, cur);
                    if(dest.equals(nextLoc)) {
                        best = nextLoc;
                        break loop1;
                    }
//                    if(opts.contains(nextLoc)) {
//                        best = nextLoc;
//                        break loop1;
//                    }
                    q.add(nextLoc);
                }
            }
            first = false;
        }
        if(best == null) return false;
        LinkedList<MapLocation> prevs = new LinkedList<>();
        MapLocation pp = best;
        while(pp != null) {
            prevs.addFirst(pp);
            pp = prev.get(pp);
        }
        int size = prevs.size();
        path = prevs.toArray(new MapLocation[size]);
//        rc.setIndicatorString(Arrays.toString(path));
        return true;
    }

    private static boolean goodCurrent(Direction current, Direction dir) {
        return current == Direction.CENTER
                || current == dir
                || current == dir.rotateLeft()
                || current == dir.rotateRight();
    }
    private static int getVisionRadius(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        if(rc.senseCloud(loc)) return GameConstants.CLOUD_VISION_RADIUS_SQUARED;
        return rc.getType().visionRadiusSquared;
    }
}