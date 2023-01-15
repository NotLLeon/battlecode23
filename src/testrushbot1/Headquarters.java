package testrushbot1;

import battlecode.common.*;

public class Headquarters extends Robot {

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

        for (int i = 8; i < 18; i++) {
            int raw = rc.readSharedArray(i)-1;
            if (raw != -1) {
                System.out.println("Adamantium well at (" + (raw%rc.getMapWidth()) + ", " + (raw/rc.getMapWidth()) + ")");
            } else {
                System.out.println("Unfound ad well");
            }
        }

        for (int i = 18; i < 28; i++) {
            int raw = rc.readSharedArray(i)-1;
            if (raw != -1) {
                System.out.println("Mana well at (" + (raw%rc.getMapWidth()) + ", " + (raw/rc.getMapWidth()) + ")");
            } else {
                System.out.println("Unfound mana well");
            }
        }

        if (Random.nextBoolean()) {
            // Let's try to build a carrier.
            rc.setIndicatorString("Trying to build a carrier");
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
}
