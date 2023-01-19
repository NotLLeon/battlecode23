package v5;

import battlecode.common.*;

import java.util.Arrays;

public class Headquarters extends Robot {

    static int index = 0;
    static int hqCount = 0;
    // static int amplifiers = 5;
    static boolean smallMap = false;
    static boolean canSeeEnemyHq = false;

    public enum Symmetry {
        ROTATIONAL,
        HORIZONTAL,
        VERTICAL
    }

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        MapLocation curLoc = rc.getLocation();

        if(turnCount == 1) {
            if(rc.getMapHeight() < 30 && rc.getMapWidth() < 30) smallMap = true;
            Comms.writeHQ(rc, rc.getLocation());
            hqCount = rc.getRobotCount();

            WellInfo[] wells = rc.senseNearbyWells();
            for (WellInfo well : wells) Comms.writeWellLoc(rc, well);

            RobotInfo[] robots = rc.senseNearbyRobots();
            for(RobotInfo robot: robots) {
                if(robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() != rc.getTeam()) {
                    canSeeEnemyHq = true;
                    break;
                }
            }
        } else if (turnCount == 2) {
            // unused for now
//            Symmetry[] possibleSyms = getSymmetry(rc);
        }

        MapLocation spawnLoc = getBuildLoc(rc);

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
                && turnCount >= 750
                && rc.canBuildRobot(RobotType.AMPLIFIER, spawnLoc)
                && turnCount % 100 == 0) {
            rc.buildRobot(RobotType.AMPLIFIER, spawnLoc);
        }
        if (currRobotCount > 20*hqCount
                && turnCount >= 1000
                && rc.canBuildAnchor(Anchor.STANDARD)
                && rc.getNumAnchors(Anchor.STANDARD) < 1
                && turnCount % 75 == 0) {
            rc.buildAnchor(Anchor.STANDARD);
        }

        if(currRobotCount > (rc.getMapHeight()*rc.getMapWidth())/5) return;


        int weight = 2;
        if(turnCount < 250) weight = 4;
        RobotType tryFirst = null;
        RobotType trySecond = null;

        if((turnCount > 3 && Random.nextInt(weight) == 0) || (turnCount <= 3 && !smallMap && !canSeeEnemyHq)) {
            tryFirst = RobotType.CARRIER;
            trySecond = RobotType.LAUNCHER;
        } else {
            tryFirst = RobotType.LAUNCHER;
            trySecond = RobotType.CARRIER;
        }
        while(rc.canBuildRobot(tryFirst, spawnLoc)) {
            rc.buildRobot(tryFirst, spawnLoc);
            spawnLoc = getBuildLoc(rc);
        }
        while(rc.canBuildRobot(trySecond, spawnLoc)) {
            rc.buildRobot(trySecond, spawnLoc);
            spawnLoc = getBuildLoc(rc);
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

    static Symmetry[] getSymmetry(RobotController rc) throws GameActionException {
        int mapH = rc.getMapHeight();
        int mapW = rc.getMapWidth();
        MapLocation[] hqs = Comms.getHQs(rc);
        int canRot = 1;
        int canHor = 1;
        int canVer= 1;
        for(MapLocation hq1 : hqs) {
            for(MapLocation hq2 : hqs) {
                boolean oppX = (mapW-hq1.x-1) == hq2.x;
                boolean oppY = (mapH-hq1.y-1) == hq2.y;
                if(oppX && oppY) canRot = 0;
                if(oppX) canHor = 0;
                if(oppY) canVer = 0;
            }
        }
        int numSyms = canRot + canHor + canVer;
        Symmetry[] possibleSyms = new Symmetry[numSyms];
        int ind = 0;
        if(canRot == 1) possibleSyms[ind++] = Symmetry.ROTATIONAL;
        if(canHor == 1) possibleSyms[ind++] = Symmetry.HORIZONTAL;
        if(canVer == 1) possibleSyms[ind] = Symmetry.VERTICAL;
        return possibleSyms;
    }
}
