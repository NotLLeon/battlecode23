package v4_1;

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
            //Comms.writeWellLoc(rc, 0, ResourceType.ADAMANTIUM);
            //Comms.writeWellLoc(rc, 1, ResourceType.ADAMANTIUM);
            //Comms.writeWellLoc(rc, 2, ResourceType.MANA);
           // Comms.writeWellLoc(rc, 3, ResourceType.MANA);
            WellInfo[] wells = rc.senseNearbyWells();
            for (WellInfo well : wells) Comms.writeWellLoc(rc, well);


        }

        MapLocation spawnLoc = getBuildLoc(rc);

        if(turnCount <= 3) {
            if(smallMap) rc.buildRobot(RobotType.LAUNCHER, spawnLoc);
            else rc.buildRobot(RobotType.CARRIER, spawnLoc);
            return;
        }

       // rc.setIndicatorDot(new MapLocation(0, 0), 0, 0, 0);

        for (int i = 0; i < Comms.getNumAdWells(rc); i++) {
           // MapLocation loc = Comms.decodeWellLoc(rc, rc.readSharedArray(4 + i));
            MapLocation loc = Comms.getAdWell(rc,i);
            rc.setIndicatorDot(loc, 255, 0, 0);
        }

        for (int i = 0; i < Comms.getNumManaWells(rc); i++) {
          //  MapLocation loc = Comms.decodeWellLoc(rc, rc.readSharedArray(14+i));
            MapLocation loc = Comms.getManaWell(rc, i);
            rc.setIndicatorDot(loc, 0, 0, 255);
        }

        rc.setIndicatorString(Comms.getNumAdWells(rc) + " ad wells, " + Comms.getNumManaWells(rc) + " mana wells.");

      //  for (int i = 0; i < Comms.getNumWells(rc); i++) {
      //      rc.setIndicatorDot(Comms.getWell(rc, i), 255, 0, 255);
      //  }

        for (int i = 0; i < Comms.getNumIslands(rc); i++) {
            MapLocation loc = Comms.getIsland(rc, i);
            rc.setIndicatorDot(loc, 0, 255, 0);
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
