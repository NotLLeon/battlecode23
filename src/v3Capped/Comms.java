package v3Capped;

import battlecode.common.*;

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
        wellsStartIdx++;
    }

    // WELLS

    static int wellsStartIdx = 1;

    public static int encodeWellLoc(RobotController rc, MapLocation loc) {
        return loc.x + loc.y * rc.getMapWidth() + 1;
    }

    public static MapLocation decodeWellLoc(RobotController rc, int encodedLoc) {
        encodedLoc -= 1;
        int x = encodedLoc % rc.getMapWidth();
        int y = encodedLoc / rc.getMapWidth();
        return new MapLocation(x, y);
    }

    public static void writeWellLocs(RobotController rc, HashSet<Integer> locs) throws GameActionException {
        for (int i : locs) {
            for (int j = wellsStartIdx; j < wellsStartIdx + Constants.NUM_WELLS_STORED; j++) {
                int val = rc.readSharedArray(j);
                if (val == i) {
                    break;
                } else if (val == 0) {
                    rc.writeSharedArray(j, i);
                    break;
                }
            }
        }
    }
}
