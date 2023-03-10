package v7;

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
            int encodedLoc = rc.readSharedArray(i);
            if(encodedLoc == 0) hqs[i] = null;
            else hqs[i] = decodeHQLoc(rc, encodedLoc);
        }
        return hqs;
    }

    public static void writeHQ(RobotController rc, MapLocation loc) throws GameActionException {
        int numHQs = getNumHQs(rc);
        for(int i = 0; i < numHQs; ++i) {
            int cur = rc.readSharedArray(i);
            if(cur == 0) {
                rc.writeSharedArray(i, encodeHQLoc(rc, loc));
                break;
            }
        }
//        rc.writeSharedArray(Constants.IDX_NUM_HQS, numHQs + 1);
        //wellsStartIdx++;
    }

    public static void writeNumHQs(RobotController rc, int num) throws GameActionException {
        rc.writeSharedArray(Constants.IDX_NUM_HQS, num);
    }

    public static void setTotalMana(RobotController rc, int total_mana) throws GameActionException{
        rc.writeSharedArray(Constants.IDX_TOTAL_MANA, total_mana);
    }

    public static void setTotalAd(RobotController rc, int total_ad) throws GameActionException {
        rc.writeSharedArray(Constants.IDX_TOTAL_AD, total_ad);
    }

    public static int getTotalMana(RobotController rc) throws GameActionException{
        return rc.readSharedArray(Constants.IDX_TOTAL_MANA);
    }

    public static int getTotalAd(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.IDX_TOTAL_AD);
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
        return decodeIslandLoc(rc, rc.readSharedArray(Constants.MAX_HQS_STORED + index));
    }

    public static MapLocation getManaWell(RobotController rc, int index) throws GameActionException {
        return decodeIslandLoc(rc, rc.readSharedArray(Constants.MAX_HQS_STORED + index + Constants.MAX_AD_WELLS_STORED));
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
        // if (type == ResourceType.ADAMANTIUM) {
        //     return;
        // }
        int offset = (type == ResourceType.ADAMANTIUM) ? 0 : Constants.MAX_AD_WELLS_STORED;
        for (int j = wellsStartIdx+offset; j < offset+wellsStartIdx + Constants.MAX_AD_WELLS_STORED; j++) {
            int val = rc.readSharedArray(j);
            if (val == loc) {
                break;
            } else if (val == 0) {
                rc.writeSharedArray(j, loc);
                int num_index = (type == ResourceType.ADAMANTIUM) ? Constants.IDX_NUM_AD_WELLS : Constants.IDX_NUM_MANA_WELLS;
                int num_wells = rc.readSharedArray(num_index)+1;
                rc.writeSharedArray(num_index, num_wells);
                MapLocation decoded = decodeWellLoc(rc, loc);
                // System.out.println("True index: " + j + " (discovered " + ((type==ResourceType.ADAMANTIUM) ? "adamantium) at (" : "mana) at (") + decoded.x + "," + decoded.y + ")");
                break;
            }
        }
    }

    public static MapLocation getWell(RobotController rc, int index) throws GameActionException{
        return decodeWellLoc(rc, rc.readSharedArray(index + wellsStartIdx));
    }

    public static boolean knowsWell(RobotController rc, MapLocation loc) throws GameActionException {
        int index = 0;
        int encoded = encodeWellLoc(rc, loc);
        for (int i = 0; i < getNumAdWells(rc); i++) {
            index = Comms.encodeWellLoc(rc,getAdWell(rc, i));
            if (encoded == index) {
                return true;
            }
        }
        for (int i = 0; i < getNumManaWells(rc); i++) {
            index = Comms.encodeWellLoc(rc,getManaWell(rc, i));
            if (encoded == index) {
                return true;
            }
        }
        return false;
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

    public static void writeIslandLoc(RobotController rc, int loc, int id) throws GameActionException {
        if (getNumIslands(rc) < Math.min(rc.getIslandCount(), Constants.MAX_ISLANDS_STORED)) {
            for (int j = islandsStartIdx; j < islandsStartIdx + Constants.MAX_ISLANDS_STORED; j++) {
                int val = rc.readSharedArray(2 * j - islandsStartIdx);
                if (val == id) {
                    break;
                } else if (val == 0) {
                    //  System.out.println("Storing island location.");
                    rc.writeSharedArray(2 * j - islandsStartIdx, id);
                    rc.writeSharedArray(2 * j - islandsStartIdx + 1, loc);
                    int num_islands = rc.readSharedArray(Constants.IDX_NUM_ISLANDS) + 1;
                    rc.writeSharedArray(Constants.IDX_NUM_ISLANDS, num_islands);
                    break;
                }
            }
        }
    }
    public static void writeIslandLocs(RobotController rc, HashMap<Integer, Integer> locs) throws GameActionException{
        //System.out.println("Writing locs");
        if (getNumIslands(rc) < Math.min(rc.getIslandCount(), Constants.MAX_ISLANDS_STORED)) {
            for (int i : locs.keySet()) {
                writeIslandLoc(rc, locs.get(i), i);
            }
        /*int next_index = islandsStartIdx;
        for (int i : locs.keySet()) {
            for (int j = next_index; j < islandsStartIdx + Constants.MAX_ISLANDS_STORED; j++) {
                int val = rc.readSharedArray(2 * j - islandsStartIdx);
                if (val == i) {
                    break;
                } else if (val == 0) {
                    next_index = j+1;
                    //  System.out.println("Storing island location.");
                    rc.writeSharedArray(2 * j - islandsStartIdx, i);
                    rc.writeSharedArray(2 * j - islandsStartIdx + 1, locs.get(i));
                    int num_islands = rc.readSharedArray(Constants.IDX_NUM_ISLANDS) + 1;
                    rc.writeSharedArray(Constants.IDX_NUM_ISLANDS, num_islands);
                    break;
                }
            }
        }*/
        }
    }

    // *****************************************************************************************************************
    //
    // MISC
    //
    // *****************************************************************************************************************

    public static void setDistressSignal(RobotController rc, boolean set) throws GameActionException {
        int curSignal = rc.readSharedArray(Constants.IDX_DISTRESS_SIGNAL);
        MapLocation curLoc = rc.getLocation();
        MapLocation[] hqs = getHQs(rc);
        int hqIdx = 0;
        for(; hqIdx < hqs.length; ++hqIdx) {
            if(curLoc.equals(hqs[hqIdx])) break;
        }
        int bit = 1 << hqIdx;
        boolean curSet = (curSignal & bit) != 0;
        if(set == curSet) return;
        if(set) curSignal += bit;
        else curSignal -= bit;
        rc.writeSharedArray(Constants.IDX_DISTRESS_SIGNAL, curSignal);
    }

    public static MapLocation[] getDistressLocations(RobotController rc) throws GameActionException {
        int curSignal = rc.readSharedArray(Constants.IDX_DISTRESS_SIGNAL);
        MapLocation [] hqLocs = getHQs(rc);
        int numSigs = 0;
        for(int i = 0; i < 4; ++i) {
            if((curSignal & (1 << i)) != 0) ++numSigs;
        }
        MapLocation [] distressLocs = new MapLocation[numSigs];
        int ind = 0;
        for(int i = 0; i < 4; ++i) {
            if((curSignal & (1 << i)) != 0) distressLocs[ind++] = hqLocs[i];
        }
        return distressLocs;
    }


    private static final int enemyHqStartIdx = islandsStartIdx;
    public static void writeEnemyHqLoc(RobotController rc, MapLocation loc) throws GameActionException {
        int numEnemyHqs = rc.readSharedArray(Constants.IDX_NUM_ISLANDS);
        rc.writeSharedArray(enemyHqStartIdx + numEnemyHqs, encodeLoc(rc, loc));
        rc.writeSharedArray(Constants.IDX_NUM_ISLANDS, numEnemyHqs+1);
    }

    public static MapLocation[] getEnemyHqLocs(RobotController rc) throws GameActionException {
        int numEnemyHqs = rc.readSharedArray(Constants.IDX_NUM_ISLANDS);
        MapLocation[] enemyHqLocs = new MapLocation[numEnemyHqs];
        for(int i = 0; i < numEnemyHqs; ++i) {
            enemyHqLocs[i] = decodeLoc(rc, rc.readSharedArray(enemyHqStartIdx + i));
        }
        return enemyHqLocs;
    }

    public static void wipeEnemyHqLocs(RobotController rc) throws GameActionException {
        int numEnemyHqs = rc.readSharedArray(Constants.IDX_NUM_ISLANDS);
        for(int i = 0; i < numEnemyHqs; ++i) rc.writeSharedArray(enemyHqStartIdx + i, 0);
        rc.writeSharedArray(Constants.IDX_NUM_ISLANDS, 0);
    }

    public static void writePossibleSyms(RobotController rc, int syms) throws GameActionException {
        rc.writeSharedArray(Constants.IDX_POSSIBLE_SYMS, syms);
    }

    /**
     *     first bit - Horizontal symmetry, second bit - Vertical, third bit - Rotational
     */
    public static int getPossibleSyms(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.IDX_POSSIBLE_SYMS);
    }
}