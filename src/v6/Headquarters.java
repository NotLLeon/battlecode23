package v6;

import battlecode.common.*;

public class Headquarters extends Robot {

    static int index = 0;
    static int hqCount = 0;
    // static int amplifiers = 5;
    static boolean smallMap = false;
    static MapLocation closeEnemyHqLoc = null;
    static MapLocation closeWellLoc = null;
    static boolean tryingtoBuildAnchor = false;

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
            for (WellInfo well : wells) {
                if(well.getResourceType() == ResourceType.ADAMANTIUM) closeWellLoc = well.getMapLocation();
                Comms.writeWellLoc(rc, well);
            }

            RobotInfo[] robots = rc.senseNearbyRobots();
            for(RobotInfo robot: robots) {
                if(robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() != rc.getTeam()) {
                    closeEnemyHqLoc = robot.getLocation();
                    break;
                }
            }
        } else if (turnCount == 2) {
            // unused for now
//            Symmetry[] possibleSyms = getSymmetry(rc);
        }

        //Putting the check here just in case.
        if (Comms.getNumHQs(rc) > 0) {
            if (Comms.getHQs(rc)[0].equals(curLoc)) {
                Comms.setTotalAd(rc,rc.getResourceAmount(ResourceType.ADAMANTIUM));
                Comms.setTotalMana(rc,rc.getResourceAmount(ResourceType.MANA));
            } else {
                int old_ad = Comms.getTotalAd(rc);
                int old_mana = Comms.getTotalMana(rc);
                Comms.setTotalAd(rc, old_ad + rc.getResourceAmount(ResourceType.ADAMANTIUM));
                Comms.setTotalMana(rc, old_mana + rc.getResourceAmount(ResourceType.MANA));
            }
        }

//        findSpawnableLocs(rc);
//        int spawnInd = 0;
//        int spawnSpaces = spawnLocs.length;

//        int raw = rc.readSharedArray(index)-1;

//        int num_islands = Comms.getNumIslands(rc);
//         for (int i = 0; i < num_islands; i++) {
//             MapLocation island = Comms.getIsland(rc, i);
//         }
        int currRobotCount = rc.getRobotCount();
        RobotInfo[] nearby_enemies = rc.senseNearbyRobots(16, rc.getTeam().opponent());
        int enemyLaunchers = 0;
        for (int i = 0; i < nearby_enemies.length; i++) {
            if (nearby_enemies[i].type == RobotType.LAUNCHER) {
                enemyLaunchers++;
            }
        }
        if (enemyLaunchers > 0) {
            Comms.setDistressSignal(rc, true);
        } else {
            Comms.setDistressSignal(rc, false);
        }

        for (int i = 0; i < Comms.getNumAdWells(rc); i++) {
            rc.setIndicatorDot(Comms.getAdWell(rc, i), 255,0,0);
        }
        for (int i = 0; i < Comms.getNumManaWells(rc); i++) {
            rc.setIndicatorDot(Comms.getManaWell(rc, i), 0,0,255);
        }
        for (int i = 0; i < Comms.getNumIslands(rc); i++) {
            rc.setIndicatorDot(Comms.getIsland(rc, i), 0,255,0);
        }
        Direction dirToCent = curLoc.directionTo(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2));

        // TODO: rewrite
        if (currRobotCount > 20*hqCount
                && turnCount >= 750
//                && spawnInd < spawnSpaces
//                && rc.canBuildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd])
                && turnCount % 100 == 0) {
//            rc.buildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd++]);
            buildInDir(rc, RobotType.AMPLIFIER, dirToCent);
        }
        if (currRobotCount > 20*hqCount
                && turnCount >= 1000
                && rc.getNumAnchors(Anchor.STANDARD) < 1
                && turnCount % 150 == 0) {
            tryingtoBuildAnchor = true;
        }

        if(tryingtoBuildAnchor && rc.canBuildAnchor(Anchor.STANDARD)) {
            tryingtoBuildAnchor = false;
            rc.buildAnchor(Anchor.STANDARD);
        }

        if(tryingtoBuildAnchor || currRobotCount > (rc.getMapHeight()*rc.getMapWidth())/5) return;


        int weight = 2;
//        if(turnCount < 250) weight = 4;
        RobotType tryFirst = null;
        RobotType trySecond = null;

        if((turnCount > 3 && Random.nextInt(weight) == 0)
                || (turnCount <= 3 && !smallMap && closeEnemyHqLoc == null)) {
            tryFirst = RobotType.CARRIER;
            trySecond = RobotType.LAUNCHER;
        } else {
            tryFirst = RobotType.LAUNCHER;
            trySecond = RobotType.CARRIER;
        }
//        while(spawnInd < spawnSpaces && rc.canBuildRobot(tryFirst, spawnLocs[spawnInd])) {
//            rc.buildRobot(tryFirst, spawnLocs[spawnInd++]);
//        }
//        while(spawnInd < spawnSpaces && rc.canBuildRobot(trySecond, spawnLocs[spawnInd])) {
//            rc.buildRobot(trySecond, spawnLocs[spawnInd++]);
//        }
        Direction launcherDir = closeEnemyHqLoc == null ? dirToCent : curLoc.directionTo(closeEnemyHqLoc);
        Direction carrierDir = closeWellLoc == null ? Direction.CENTER : curLoc.directionTo(closeWellLoc);
        Direction buildDir1, buildDir2;
        if(tryFirst == RobotType.LAUNCHER) {
            buildDir1 = launcherDir;
            buildDir2 = carrierDir;
        } else {
            buildDir1 = carrierDir;
            buildDir2 = launcherDir;
        }
        boolean stop1 = false;
        while(!stop1) {
            if(buildDir1 == Direction.CENTER) stop1 = !buildInDir(rc, tryFirst, Random.nextDir());
            else stop1 = !buildInDir(rc, tryFirst, buildDir1);
        }
        boolean stop2 = false;
        while(!stop2) {
            if(buildDir2 == Direction.CENTER) stop2 = !buildInDir(rc, trySecond, Random.nextDir());
            else stop2 = !buildInDir(rc, trySecond, buildDir2);
        }
    }

//    static void findSpawnableLocs(RobotController rc) throws GameActionException {
//        MapInfo[] actionMapInfo = rc.senseNearbyMapInfos(RobotType.HEADQUARTERS.actionRadiusSquared);
//        spawnableLocs.clear();
//
//        for (MapInfo mapInfo : actionMapInfo) {
//            if (mapInfo.isPassable()
//                    && !rc.isLocationOccupied(mapInfo.getMapLocation())) {
//                spawnableLocs.add(mapInfo.getMapLocation());
//            }
//        }
//    }

    static boolean buildInDir(RobotController rc, RobotType type, Direction dir) throws GameActionException {
        Direction[] tryDirs = {
                dir,
                dir.rotateRight(),
                dir.rotateLeft(),
                dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateLeft().opposite(),
                dir.rotateRight().opposite(),
                dir.opposite()
        };
        for(Direction tryDir : tryDirs) {
            MapLocation[] tryLocs = getDirBuildLocs(rc, tryDir);
            for(MapLocation tryLoc : tryLocs) {
                if(rc.canBuildRobot(type, tryLoc)) {
                    rc.buildRobot(type, tryLoc);
                    return true;
                }
            }
        }
        return false;
    }

    // TODO: turn this into a giant switch
    static MapLocation[] getDirBuildLocs(RobotController rc, Direction dir) {
        MapLocation curLoc = rc.getLocation();
        if(dir == Direction.NORTH
                || dir == Direction.EAST
                || dir == Direction.SOUTH
                || dir == Direction.WEST) {
            return new MapLocation[]{
                    curLoc.add(dir).add(dir).add(dir),
                    curLoc.add(dir).add(dir.rotateLeft()),
                    curLoc.add(dir).add(dir.rotateRight()),
                    curLoc.add(dir).add(dir),
                    curLoc.add(dir)
            };
        }
        return new MapLocation[] {
                curLoc.add(dir).add(dir),
                curLoc.add(dir).add(dir.rotateLeft()),
                curLoc.add(dir).add(dir.rotateRight()),
                curLoc.add(dir)
        };
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
