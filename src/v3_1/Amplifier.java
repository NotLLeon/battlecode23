package v3_1;

import battlecode.common.*;

public class Amplifier extends Robot {

    static MapLocation baseHQ;
    public static void runAmplifier(RobotController rc, int turnCount) throws GameActionException {
        if (turnCount == 1) {
            baseHQ = getClosestHQ(rc);
        }
        Direction dir = Explore.exploreAwayFromHQ(rc, baseHQ);
        if (rc.canMove(dir)) {
            rc.move(dir);
        }

        WellInfo[] well_info = rc.senseNearbyWells();
        int[] island_index_info = rc.senseNearbyIslands();

        //Look for islands and record
        //TODO: Allow for overwriting, starting at the beginning. As of right now, this will lose to maps with too many islands
        for (int i = 0; i < island_index_info.length; i++) {
            //System.out.println("Spotted island");
            MapLocation[] island_info = rc.senseNearbyIslandLocations(island_index_info[i]);
            Comms.writeIslandLoc(rc,Comms.encodeIslandLoc(rc,island_info[0]),island_index_info[i]);
        }
        for (int i = 0; i < well_info.length; i++) {
            Comms.writeWellLoc(rc, Comms.encodeWellLoc(rc, well_info[i].getMapLocation()), well_info[i].getResourceType());
        }
    }
}
