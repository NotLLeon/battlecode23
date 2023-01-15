package v2;

import battlecode.common.*;

public class Comms {

    static int getNumHQs(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.IDX_NUM_HQS);
    }

    private static int encodeHQLoc(RobotController rc, MapLocation loc) {
        return loc.x + loc.y * rc.getMapWidth() + 1;
    }

    private static MapLocation decodeHQLoc(RobotController rc, int encodedLoc) {
        encodedLoc -= 1;
        int x = encodedLoc % rc.getMapWidth();
        int y = encodedLoc / rc.getMapWidth();
        return new MapLocation(x, y);
    }

    static MapLocation[] getHQs(RobotController rc) throws GameActionException {
        int numHqs = getNumHQs(rc);
        MapLocation [] hqs = new MapLocation[numHqs];
        for (int i = 0; i < numHqs; ++i) {
            hqs[i] = decodeHQLoc(rc, rc.readSharedArray(i));
        }
        return hqs;
    }

    static void writeHQ(RobotController rc, MapLocation loc) throws GameActionException {
        int numHQs = getNumHQs(rc);
        rc.writeSharedArray(numHQs, encodeHQLoc(rc, loc));
        rc.writeSharedArray(Constants.IDX_NUM_HQS, rc.readSharedArray(Constants.IDX_NUM_HQS) + 1);
    }
}
