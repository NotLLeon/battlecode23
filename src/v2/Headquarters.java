package v2;

import battlecode.common.*;

import java.util.Random;

public class Headquarters extends Robot {

    static final Random rng = new Random(6147);

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static int index = 0;

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation curLoc = rc.getLocation();
        MapLocation newLoc = curLoc.add(dir);
//        if (rc.canBuildAnchor(Anchor.STANDARD)) {
//            // If we can build an anchor do it!
//            rc.buildAnchor(Anchor.STANDARD);
//            rc.setIndicatorString("Building anchor! " + rc.getAnchor());
//        }

        if(turnCount == 1) {
          //  int mapHeight = rc.getMapHeight();
           int mapWidth = rc.getMapWidth();
           // rc.writeSharedArray(0, mapWidth);
           // rc.writeSharedArray(1, mapHeight);
         //  int index = 0;
           for (int i = 0; i < 8; i++) {
                if (rc.readSharedArray(i) == 0) {
                    index = i;
                    break;
                }
           }
           rc.writeSharedArray(index, curLoc.x + mapWidth * curLoc.y + 1);
        }

        int raw = rc.readSharedArray(index)-1;
        rc.setIndicatorString("Index: " + index + " Location: (" + (raw%rc.getMapWidth()) + "," + (raw/rc.getMapWidth()));

        if (rng.nextBoolean()) {
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
}
