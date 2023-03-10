package v4_launchers;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;

public class Comms {

    private static int encodeLoc(RobotController rc, MapLocation loc) {
        return loc.x + loc.y * rc.getMapWidth() + 1;
    }

    private static MapLocation decodeLoc(RobotController rc, int encodedLoc) {
        encodedLoc -= 1;
        int x = encodedLoc % rc.getMapWidth();
        int y = encodedLoc / rc.getMapWidth();
        return new MapLocation(x, y);
    }


    // *****************************************************************************************************************
    //
    // HQS
    //
    // *****************************************************************************************************************

    static int getNumHQs(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.IDX_NUM_HQS);
    }

    public static int encodeHQLoc(RobotController rc, MapLocation loc) {
       return encodeLoc(rc, loc);
    }

    public static MapLocation decodeHQLoc(RobotController rc, int encodedLoc) { return decodeLoc(rc, encodedLoc); }

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
        rc.writeSharedArray(Constants.IDX_NUM_HQS, numHQs + 1);
        //wellsStartIdx++;
    }

    // *****************************************************************************************************************
    //
    // WELLS
    //
    // *****************************************************************************************************************

    private static final int wellsStartIdx = Constants.MAX_HQS_STORED;

    public static int getNumAdWells(RobotController rc) throws GameActionException{
        return rc.readSharedArray(Constants.IDX_NUM_AD_WELLS);
    }
    public static int getNumManaWells(RobotController rc) throws GameActionException{
        return rc.readSharedArray(Constants.IDX_NUM_MANA_WELLS);
    }

    public static int getNumWells(RobotController rc) throws GameActionException{
        return getNumAdWells(rc) + getNumManaWells(rc);
    }

    public static MapLocation getAdWell(RobotController rc, int index) throws GameActionException {
        return decodeIslandLoc(rc, rc.readSharedArray(wellsStartIdx + index));
    }

    public static MapLocation getManaWell(RobotController rc, int index) throws GameActionException {
        return decodeIslandLoc(rc, rc.readSharedArray(wellsStartIdx + index + Constants.MAX_AD_WELLS_STORED));
    }

    public static int encodeWellLoc(RobotController rc, MapLocation loc) {
        return encodeLoc(rc, loc);
    }

    public static MapLocation decodeWellLoc(RobotController rc, int encodedLoc) {
        return decodeLoc(rc, encodedLoc);
    }


    public static void writeWellLoc(RobotController rc, WellInfo well) throws GameActionException {
        writeWellLoc(rc, Comms.encodeWellLoc(rc, well.getMapLocation()), well.getResourceType());
    }

    public static void writeWellLocs(RobotController rc, HashSet<Integer> locs, ResourceType type) throws GameActionException {
        for (int i : locs) {
            writeWellLoc(rc, i, type);
        }
    }

    //Keep separate in case we use the first base 10 digit to represent a blacklist.
    public static void writeWellLoc(RobotController rc, int loc, ResourceType type) throws GameActionException {
        int offset = (type == ResourceType.ADAMANTIUM) ? 0 : Constants.MAX_AD_WELLS_STORED;
        for (int j = wellsStartIdx+offset; j < offset+wellsStartIdx + Constants.MAX_WELLS_STORED; j++) {
            int val = rc.readSharedArray(j);
            if (val == loc) {
                break;
            } else if (val == 0) {
                rc.writeSharedArray(j, loc);
                int num_index = (offset == 0) ? Constants.IDX_NUM_AD_WELLS : Constants.IDX_NUM_MANA_WELLS;
                int num_wells = rc.readSharedArray(num_index)+1;
                rc.writeSharedArray(num_index, num_wells);
                break;
            }
        }
    }

    // *****************************************************************************************************************
    //
    // ISLANDS
    //
    // *****************************************************************************************************************

    private static final int islandsStartIdx = wellsStartIdx + Constants.MAX_WELLS_STORED;

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
        return encodeLoc(rc, loc);
    }

    public static MapLocation decodeIslandLoc(RobotController rc, int encodedLoc) {
        return decodeLoc(rc, encodedLoc);
    }

    public static boolean knowsIsland(RobotController rc, int island_id) throws GameActionException {
        int index = 0;
        for (int i = 0; i < getNumIslands(rc); i++) {
            index = getIslandID(rc, i);
            if (island_id == index) {
                return true;
            }
        }
        return false;
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
    public static void writeIslandLocs(RobotController rc, HashMap<Integer, Integer> locs) throws GameActionException{
        //System.out.println("Writing locs");
        for (int i : locs.keySet()) {
            writeIslandLoc(rc, locs.get(i), i);
        }
    }
}
