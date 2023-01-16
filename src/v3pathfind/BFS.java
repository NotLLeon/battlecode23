package v3pathfind;

import battlecode.common.*;

import java.util.Queue;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;

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

    static boolean onPath = false;

    static MapLocation[] path;

    static int pathInd = 0;

    public static void reset(){
        onPath = false;
    }

    public static boolean getDir(RobotController rc, MapLocation dest) throws GameActionException {
        if(!onPath) {
            if(!getPath(rc, dest)) return true;
            onPath = true;
            return true;
        } else {
            if(pathInd == path.length) {
                onPath = false;
                return true;
            } else {
                Direction md = rc.getLocation().directionTo(path[pathInd++]);
                if(rc.canMove(md)) {
                    rc.move(md);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static boolean getPath(RobotController rc, MapLocation dest) throws GameActionException {
        int visionRadius = Math.min(16, getVisionRadius(rc));
        HashSet<MapLocation> opts= new HashSet<MapLocation>();
        HashSet<MapLocation> vis = new HashSet<MapLocation>();
        HashMap<MapLocation, MapLocation> prev = new HashMap<MapLocation, MapLocation>();
        Queue<MapLocation> q = new LinkedList<MapLocation>();

        MapLocation curLoc = rc.getLocation();
        q.add(curLoc);
        prev.put(curLoc, null);
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
        LinkedList<MapLocation> prevs = new LinkedList<MapLocation>();
        MapLocation pp = best;
        while(pp != null) {
            prevs.addFirst(pp);
            pp = prev.get(pp);
        }

        path = prevs.toArray(new MapLocation[0]);
        pathInd = 0;
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
