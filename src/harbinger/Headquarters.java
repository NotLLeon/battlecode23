package harbinger;

import battlecode.common.*;

public class Headquarters extends Robot {
    static int index = 0;

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = Random.nextDir();
        MapLocation curLoc = rc.getLocation();
        MapLocation newLoc = curLoc.add(dir);
//        if (rc.canBuildAnchor(Anchor.STANDARD)) {
//            // If we can build an anchor do it!
//            rc.buildAnchor(Anchor.STANDARD);
//            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
//        }

        if(turnCount == 1) {
           Comms.writeHQ(rc, rc.getLocation());
        }

        int raw = rc.readSharedArray(index)-1;
        /*rc.setIndicatorString("Index: " + index + " Location: (" + (raw%rc.getMapWidth()) + "," + (raw/rc.getMapWidth()));

        System.out.println("There are " + Comms.getNumHQs(rc) + " HQs and " + Comms.getNumWells(rc) + " wells.");
        for (int i = 0; i < Comms.getNumWells(rc); i++) {
            MapLocation wellLoc = Comms.getWell(rc, i);
            System.out.println("Well at (" + wellLoc.x + "," + wellLoc.y + ")");
        }*/
        int num_islands = Comms.getNumIslands(rc);
        //System.out.println("There are " + num_islands + " islands.");
        for (int i = 0; i < num_islands; i++) {
            MapLocation island = Comms.getIsland(rc, i);
           // System.out.println("Island at (" + island.x + "," + island.y + ")");
        }

        if (rc.getRobotCount() <= (rc.getMapWidth() * rc.getMapHeight() / 10)) {
            if (Random.nextBoolean()) {
                // Let's try to build a carrier.
                // rc.setIndicatorString("Trying to build a carrier");
                if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                    rc.buildRobot(RobotType.CARRIER, newLoc);
                }
            } else {
                // Let's try to build a launcher.
                rc.setIndicatorString("Trying to build a launcher");
                if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                    rc.buildRobot(RobotType.LAUNCHER, newLoc);
                }
            }
        }
        if (rc.getRobotCount() > (rc.getMapWidth() * rc.getMapHeight() / 10) - 10) {
            if (Random.nextInt(5) < 2 && rc.getRobotCount() <= (10 + (rc.getMapWidth() * rc.getMapHeight() / 10))) {
                if (rc.canBuildRobot(RobotType.AMPLIFIER, newLoc)) {
                    rc.buildRobot(RobotType.AMPLIFIER, newLoc);
                }
            } else {
                if (rc.canBuildAnchor(Anchor.STANDARD)) {
                    rc.buildAnchor(Anchor.STANDARD);
                }
            }
        }
    }
}
