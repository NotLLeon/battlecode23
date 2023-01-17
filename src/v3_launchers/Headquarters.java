package v3_launchers;

import java.util.LinkedList;

import battlecode.common.*;

public class Headquarters extends Robot {
    static int index = 0;
    static int hqCount = 0;
    static MapLocation opposite;
    static LinkedList<MapLocation> openSpawns;

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = Random.nextDir();
        MapLocation curLoc = rc.getLocation();
        MapLocation newLoc = curLoc.add(dir);
        

        if(turnCount == 1) {
           Comms.writeHQ(rc, rc.getLocation());
           opposite = new MapLocation(rc.getMapWidth() - rc.getLocation().x, rc.getMapHeight() - rc.getLocation().y);
           MapLocation[] allTilesInRange = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 9);
           for (MapLocation testTile : allTilesInRange) {
            if (rc.senseMapInfo(testTile).isPassable()) {
                openSpawns.add(testTile);
            }
           }
        }

        int currRobotCount = rc.getRobotCount();
        if (currRobotCount <= 30*hqCount) {
            RobotInfo[] nearby_enemies = rc.senseNearbyRobots(16, rc.getTeam().opponent());
            int enemyLaunchers = 0;
            for (int i = 0; i < nearby_enemies.length; i++) {
                if (nearby_enemies[i].type == RobotType.LAUNCHER) {
                    enemyLaunchers++;
                }
            }
            
            if (Random.nextBoolean() && enemyLaunchers == 0) {
                // Let's try to build a carrier.
                // rc.setIndicatorString("Trying to build a carrier");
                if (rc.canBuildRobot(RobotType.CARRIER, newLoc)) {
                    rc.buildRobot(RobotType.CARRIER, newLoc);
                }
                if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                    rc.buildRobot(RobotType.LAUNCHER, newLoc);
                }
            } else if (enemyLaunchers < 2){
                // Let's try to build a launcher.
                rc.setIndicatorString("Trying to build a launcher");
                if (rc.canBuildRobot(RobotType.LAUNCHER, newLoc)) {
                    rc.buildRobot(RobotType.LAUNCHER, newLoc);
                }
            }
        } else if (rc.canBuildRobot(RobotType.AMPLIFIER, newLoc) && turnCount % 100 == 0 && turnCount > 500) {
            rc.buildRobot(RobotType.AMPLIFIER, newLoc);
        } else if (rc.canBuildAnchor(Anchor.STANDARD) && rc.getNumAnchors(Anchor.STANDARD) < 1) {
            rc.buildAnchor(Anchor.STANDARD);
        } 

    }
}
