package v3_1;

import battlecode.common.*;

public class Headquarters extends Robot {
    static int index = 0;
    static int hqCount = 0;

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
           hqCount = rc.getRobotCount();
        }

        int raw = rc.readSharedArray(index)-1;
        rc.setIndicatorString("Index: " + index + " Location: (" + (raw%rc.getMapWidth()) + "," + (raw/rc.getMapWidth()));

        if (rc.getRobotCount() <= 30*hqCount) {
            if (Random.nextBoolean()) {
                // Let's try to build a carrier.
                // rc.setIndicatorString("Trying to build a carrier");
                if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                    rc.buildRobot(RobotType.CARRIER, newLoc);
                }
            } else {
                // Let's try to build a launcher.
                // rc.setIndicatorString("Trying to build a launcher");
                if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                    rc.buildRobot(RobotType.LAUNCHER, newLoc);
                }
            }
        } 
        else if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getNumAnchors(Anchor.STANDARD) < 1) {
            rc.buildAnchor(Anchor.STANDARD);
        }
    }
}
