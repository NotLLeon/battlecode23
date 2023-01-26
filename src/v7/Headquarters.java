package v7;

import battlecode.common.*;


public class Headquarters extends Robot {

    static int hqCount = 0;
    // static int amplifiers = 5;
    static boolean smallMap = false;
    static MapLocation closeEnemyHqLoc = null;
    static MapLocation closeWellLoc = null;
    static boolean tryingToBuildAmp = false;
    static boolean tryingtoBuildAnchor = false;
    static int saveMn = 0;
    static int saveAd = 0;

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
                if(well.getResourceType() == ResourceType.MANA) closeWellLoc = well.getMapLocation();
                Comms.writeWellLoc(rc, well);
            }

            RobotInfo[] robots = rc.senseNearbyRobots();
            for(RobotInfo robot: robots) {
                if(robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() != rc.getTeam()) {
                    closeEnemyHqLoc = robot.getLocation();
                    break;
                }
            }
        }
//        else if (turnCount == 2) {
        // unused for now
//            Symmetry[] possibleSyms = getSymmetry(rc);
//        }

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
        RobotInfo[] nearby_robots = rc.senseNearbyRobots();
        int enemyLaunchers = 0;
        for(RobotInfo robot : nearby_robots) {
            if(robot.getType() == RobotType.LAUNCHER && robot.getTeam() != rc.getTeam()) {
                enemyLaunchers++;
            }
        }

        Comms.setDistressSignal(rc, enemyLaunchers > 0);

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

        if (currRobotCount > 20*hqCount
                && turnCount >= 750
//                && spawnInd < spawnSpaces
//                && rc.canBuildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd])
                && turnCount % 100 == 0) {
//            rc.buildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd++]);
            tryingToBuildAmp = true;
            saveAd += RobotType.AMPLIFIER.buildCostAdamantium;
            saveMn += RobotType.AMPLIFIER.buildCostMana;
        }

        if (currRobotCount > 20*hqCount
                && turnCount >= 750
                && rc.getNumAnchors(Anchor.STANDARD) < 1
                && turnCount % 150 == 0) {
            tryingtoBuildAnchor = true;
            saveAd += Anchor.STANDARD.adamantiumCost;
            saveMn += Anchor.STANDARD.manaCost;
        }

        if(tryingToBuildAmp && buildInDir(rc, RobotType.AMPLIFIER, dirToCent)) {
            tryingToBuildAmp = false;
            saveAd -= RobotType.AMPLIFIER.buildCostAdamantium;
            saveMn -= RobotType.AMPLIFIER.buildCostMana;
        }

        if(tryingtoBuildAnchor && rc.canBuildAnchor(Anchor.STANDARD)) {
            tryingtoBuildAnchor = false;
            saveAd -= Anchor.STANDARD.adamantiumCost;
            saveMn -= Anchor.STANDARD.manaCost;
            rc.buildAnchor(Anchor.STANDARD);
        }

        if(currRobotCount > (rc.getMapHeight()*rc.getMapWidth())/5) return;

        Direction launcherDir = closeEnemyHqLoc == null ? dirToCent : curLoc.directionTo(closeEnemyHqLoc);
        while(buildInDir(rc, RobotType.LAUNCHER, launcherDir));
        boolean keepBuilding = true;
        while(keepBuilding) {
            if(closeWellLoc == null || turnCount >= 15) keepBuilding = buildInDir(rc, RobotType.CARRIER, Random.nextDir());
            else keepBuilding = buildInDir(rc, RobotType.CARRIER, curLoc.directionTo(closeWellLoc));
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
        if(!rc.isActionReady()
                || rc.getResourceAmount(ResourceType.ADAMANTIUM) - saveAd < type.buildCostAdamantium
                || rc.getResourceAmount(ResourceType.MANA) - saveMn < type.buildCostMana) return false;
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
