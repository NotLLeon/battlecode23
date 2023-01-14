package smartCollectors;

import battlecode.common.*;

import java.util.Random;

public class Headquarters {

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
            int mapHeight = rc.getMapHeight();
            int mapWidth = rc.getMapWidth();
           // rc.writeSharedArray(0, mapWidth);
           // rc.writeSharedArray(1, mapHeight);
           rc.writeSharedArray(0, curLoc.x + mapWidth * curLoc.y);
        }

        if (rng.nextBoolean()) {
            // Let's try to build a carrier.
            rc.setIndicatorString("Trying to build a carrier");
            if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                rc.buildRobot(RobotType.CARRIER, newLoc);
            }
        // } else {
        //     // Let's try to build a launcher.
        //     rc.setIndicatorString("Trying to build a launcher");
        //     if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
        //         rc.buildRobot(RobotType.LAUNCHER, newLoc);
        //     }
        // }
    }
}
}
