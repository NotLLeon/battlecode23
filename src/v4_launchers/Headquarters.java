package v4_launchers;

import battlecode.common.*;

public class Headquarters extends Robot {

    static int index = 0;
    static int hqCount = 0;
    // static int amplifiers = 5;

    static boolean smallMap = false;

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        MapLocation curLoc = rc.getLocation();

        if(turnCount == 1) {
            if(rc.getMapHeight() < 30 && rc.getMapWidth() < 30) smallMap = true;
            Comms.writeHQ(rc, rc.getLocation());
            hqCount = rc.getRobotCount();

            WellInfo[] wells = rc.senseNearbyWells();
            for (WellInfo well : wells) Comms.writeWellLoc(rc, well);
        }

        MapLocation spawnLoc = getBuildLoc(rc);

        if(turnCount <= 3) {
            if(smallMap) rc.buildRobot(RobotType.LAUNCHER, spawnLoc);
            else rc.buildRobot(RobotType.CARRIER, spawnLoc);
            return;
        }

//        int raw = rc.readSharedArray(index)-1;

//        int num_islands = Comms.getNumIslands(rc);
//         for (int i = 0; i < num_islands; i++) {
//             MapLocation island = Comms.getIsland(rc, i);
//         }
        int currRobotCount = rc.getRobotCount();
//        if (currRobotCount <= (rc.getMapWidth()*rc.getMapHeight()/5)) {
//        RobotInfo[] nearby_enemies = rc.senseNearbyRobots(16, rc.getTeam().opponent());
//        int enemyLaunchers = 0;
//        for (int i = 0; i < nearby_enemies.length; i++) {
//            if (nearby_enemies[i].type == RobotType.LAUNCHER) {
//                enemyLaunchers++;
//            }
//        }

        // TODO: rewrite
        if (currRobotCount > 20*hqCount
                && turnCount >= 1000
                && rc.canBuildRobot(RobotType.AMPLIFIER, spawnLoc)
                && turnCount % 100 == 0) {
            rc.buildRobot(RobotType.AMPLIFIER, spawnLoc);
        }
        if (currRobotCount > 20*hqCount
                && turnCount >= 1250
                && rc.canBuildAnchor(Anchor.STANDARD)
                && rc.getNumAnchors(Anchor.STANDARD) < 1
                && turnCount % 101 == 0) {
            rc.buildAnchor(Anchor.STANDARD);
        }

        if(currRobotCount > 30*hqCount) return;


        int weight = 2;
        if(turnCount < 250) weight = 4;
        RobotType tryFirst = null;
        RobotType trySecond = null;
        if(Random.nextInt(weight) == 0) {
            tryFirst = RobotType.CARRIER;
            trySecond = RobotType.LAUNCHER;
        } else {
            tryFirst = RobotType.LAUNCHER;
            trySecond = RobotType.CARRIER;
        }
        if(rc.canBuildRobot(tryFirst, spawnLoc)) {
            rc.buildRobot(tryFirst, spawnLoc);
        } else if(rc.canBuildRobot(trySecond, spawnLoc)) {
            rc.buildRobot(trySecond, spawnLoc);
        }
    }

    static MapLocation getBuildLoc(RobotController rc) throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        MapInfo[] locs = rc.senseNearbyMapInfos(radius);
        int spawnable = 0;

        // FIXME: jank, don't wanna make an arraylist
        for (MapInfo mapInfo : locs) {
            if (mapInfo.isPassable()
                    && !rc.isLocationOccupied(mapInfo.getMapLocation())) {
                spawnable++;
            }
        }
        if(spawnable == 0) return rc.getLocation();
        MapLocation[] spawnableLocs = new MapLocation[spawnable];
        int ind = 0;
        for (MapInfo mapInfo : locs) {
            MapLocation loc = mapInfo.getMapLocation();
            if (mapInfo.isPassable()
                    && !rc.isLocationOccupied(loc)) {
                spawnableLocs[ind++] = loc;
            }
        }
        return spawnableLocs[Random.nextInt(spawnable)];
    }
}
