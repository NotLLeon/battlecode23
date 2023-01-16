package v3_1;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.ResourceType;
import battlecode.common.RobotController;

import java.util.HashSet;

public class Comms {

    // HQ

    static int getNumHQs(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.IDX_NUM_HQS);
    }

    public static int encodeHQLoc(RobotController rc, MapLocation loc) {
        return loc.x + loc.y * rc.getMapWidth() + 1;
    }

    public static MapLocation decodeHQLoc(RobotController rc, int encodedLoc) {
        encodedLoc -= 1;
        int x = encodedLoc % rc.getMapWidth();
        int y = encodedLoc / rc.getMapWidth();
        return new MapLocation(x, y);
    }

    public static MapLocation[] getHQs(RobotController rc) throws GameActionException {
        int numHqs = getNumHQs(rc);
        MapLocation [] hqs = new MapLocation[numHqs];
        for (int i = 0; i < numHqs; ++i) {
            hqs[i] = decodeHQLoc(rc, rc.readSharedArray(i));
        }
        return hqs;
    }

    public static void writeHQ(RobotController rc, MapLocation loc) throws GameActionException {
        int numHQs = getNumHQs(rc);
        rc.writeSharedArray(numHQs, encodeHQLoc(rc, loc));
        rc.writeSharedArray(Constants.IDX_NUM_HQS, rc.readSharedArray(Constants.IDX_NUM_HQS) + 1);
        //wellsStartIdx++;
    }

    // WELLS

    static int wellsStartIdx = Constants.MAX_HQS_STORED;
    static int islandsStartIdx = wellsStartIdx + Constants.MAX_WELLS_STORED;

    public static int getNumWells(RobotController rc) throws GameActionException{
        return rc.readSharedArray(Constants.IDX_NUM_WELLS);
    }

    public static MapLocation getWell(RobotController rc, int index) throws GameActionException {
        return decodeIslandLoc(rc, rc.readSharedArray(wellsStartIdx + index));
    }
    public static int encodeWellLoc(RobotController rc, MapLocation loc) {
        return loc.x + loc.y * rc.getMapWidth() + 1;
    }

    public static MapLocation decodeWellLoc(RobotController rc, int encodedLoc) {
        encodedLoc -= 1;
        int x = encodedLoc % rc.getMapWidth();
        int y = encodedLoc / rc.getMapWidth();
        return new MapLocation(x, y);
    }

    public static int getNumIslands(RobotController rc) throws GameActionException{
        return rc.readSharedArray(Constants.IDX_NUM_ISLANDS);
    }

    public static MapLocation getIsland(RobotController rc, int index) throws GameActionException {
        return decodeIslandLoc(rc, rc.readSharedArray(2*index + islandsStartIdx+1));
    }

    public static int getIslandID(RobotController rc, int index) throws GameActionException {
        return rc.readSharedArray(2*index + islandsStartIdx);
    }

    public static int encodeIslandLoc(RobotController rc, MapLocation loc) {
        return loc.x + loc.y * rc.getMapWidth() + 1;
    }

    public static MapLocation decodeIslandLoc(RobotController rc, int encodedLoc) {
        encodedLoc -= 1;
        int x = encodedLoc % rc.getMapWidth();
        int y = encodedLoc / rc.getMapWidth();
        return new MapLocation(x, y);
    }

    //Keep separate in case we use the first base 10 digit to represent a blacklist.
    public static void writeWellLoc(RobotController rc, int loc, ResourceType type) throws GameActionException{
        for (int j = wellsStartIdx; j < wellsStartIdx + Constants.MAX_WELLS_STORED; j++) {
            int val = rc.readSharedArray(j);
            if (val == loc) {
                break;
            } else if (val == 0) {
                rc.writeSharedArray(j, loc);
                int num_wells = rc.readSharedArray(Constants.IDX_NUM_WELLS)+1;
                rc.writeSharedArray(Constants.IDX_NUM_WELLS, num_wells);
                break;
            }
        }
    }
    public static void writeWellLocs(RobotController rc, HashSet<Integer> locs, ResourceType type) throws GameActionException {
        for (int i : locs) {
            writeWellLoc(rc, i, type);
        }
    }
    public static void writeIslandLoc(RobotController rc, int loc, int id) throws GameActionException{
        for (int j = islandsStartIdx; j < islandsStartIdx + Constants.MAX_ISLANDS_STORED; j++) {
            int val = rc.readSharedArray(2*j-islandsStartIdx);
            if (val == id) {
                break;
            } else if (val == 0) {
              //  System.out.println("Storing island location.");
                rc.writeSharedArray(2*j - islandsStartIdx, id);
                rc.writeSharedArray(2*j - islandsStartIdx + 1, loc);
                int num_islands = rc.readSharedArray(Constants.IDX_NUM_ISLANDS)+1;
                rc.writeSharedArray(Constants.IDX_NUM_ISLANDS, num_islands);
                break;
            }
        }
    }
}
