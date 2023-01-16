package v3pathfind;

import battlecode.common.*;

import java.util.*;

public class BFS {

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };


    static MapLocation[] path;

    // Assumes hasDir is true
    public static Direction getDir(RobotController rc, MapLocation dest) throws GameActionException {
        boolean work = getPath(rc, dest);
        if(!work) return Direction.CENTER;
        return path[0].directionTo(path[1]);
    }

    public static boolean getPath(RobotController rc, MapLocation dest) throws GameActionException {
        int visionRadius = Math.min(16, getVisionRadius(rc));
        HashSet<MapLocation> opts= new HashSet<MapLocation>();
        HashSet<MapLocation> vis = new HashSet<MapLocation>();
        HashMap<MapLocation, MapLocation> prev = new HashMap<MapLocation, MapLocation>();
        Queue<MapLocation> q = new LinkedList<MapLocation>();

        MapLocation curLoc = rc.getLocation();
        q.add(curLoc);
        vis.add(curLoc);
        MapLocation loc = curLoc;
        for(int i = 0; i*i < visionRadius; ++i) {
            loc = loc.add(loc.directionTo(dest));
            opts.add(loc);
        }
        MapLocation best = null;
        loop1:
        while(!q.isEmpty()) {
            MapLocation cur = q.poll();
            MapLocation [] adjLocs = getAdjacentLocs(cur);
            for(MapLocation nextLoc : adjLocs) {
                if(rc.canSenseLocation(nextLoc)
                    && rc.onTheMap(nextLoc)
                    && rc.senseMapInfo(nextLoc).isPassable()
                    && !vis.contains(nextLoc)) {
                    prev.put(nextLoc, cur);
                    if(opts.contains(nextLoc)) {
                        best = nextLoc;
                        break loop1;
                    }
                    q.add(nextLoc);
                    vis.add(nextLoc);
                }
            }
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
        rc.setIndicatorString(Arrays.toString(path));
        return true;
    }

    private static MapLocation[] getAdjacentLocs(MapLocation loc) throws GameActionException {
        MapLocation [] adjLocs = new MapLocation[8];
        for(int i = 0; i < 8; ++i) {
            adjLocs[i] = loc.add(directions[i]);
        }
//            if(loc.distanceSquaredTo(newLoc) <= visionRadius
//                && rc.onTheMap(newLoc)
//                && rc.senseMapInfo(newLoc).isPassable()) {
//
//            }
        return adjLocs;
    }

    private static int getVisionRadius(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        if(rc.senseCloud(loc)) return GameConstants.CLOUD_VISION_RADIUS_SQUARED;
        return rc.getType().visionRadiusSquared;
    }
}
