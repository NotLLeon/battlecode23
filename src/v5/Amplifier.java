package v5;

import battlecode.common.*;

public class Amplifier extends Robot {

    static MapLocation baseHQ;
    public static void runAmplifier(RobotController rc, int turnCount) throws GameActionException {
        if (turnCount == 1) {
            baseHQ = getClosestHQ(rc);
        }
        exploreNewArea(rc);

        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) Comms.writeWellLoc(rc, well);

        int[] island_index_info = rc.senseNearbyIslands();

        //Look for islands and record
        //TODO: Allow for overwriting, starting at the beginning. As of right now, this will lose to maps with too many islands
        for (int i = 0; i < island_index_info.length; i++) {
            //System.out.println("Spotted island");
            MapLocation[] island_locs = rc.senseNearbyIslandLocations(island_index_info[i]);
            Comms.writeIslandLoc(rc, Comms.encodeIslandLoc(rc, island_locs[0]), island_index_info[i]);
        }
    }
}
