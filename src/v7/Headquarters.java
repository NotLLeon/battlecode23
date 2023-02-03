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
    static boolean shouldSave = false;

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        MapLocation curLoc = rc.getLocation();

        if(turnCount == 1) firstTurn(rc);
//        else if (turnCount == 2) {
        // unused for now
//            Symmetry[] possibleSyms = getSymmetry(rc);
//        }

        int anchorRate = 200;
        if (turnCount <= 750) anchorRate = 350;
        if(turnCount >= 1500) anchorRate = 100;

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

        RobotInfo enemyLauncher = null;
        int enemyLaunchers = 0;
        for(RobotInfo robot : nearby_robots) {
            if(robot.getType() == RobotType.LAUNCHER && robot.getTeam() != rc.getTeam()) {
                enemyLaunchers++;
                enemyLauncher = robot;
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
                && turnCount >= 1000
//                && spawnInd < spawnSpaces
//                && rc.canBuildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd])
                && turnCount % 150 == 0) {
//            rc.buildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd++]);
            tryingToBuildAmp = true;
            saveAd += RobotType.AMPLIFIER.buildCostAdamantium;
            saveMn += RobotType.AMPLIFIER.buildCostMana;
        }

        if (currRobotCount > 20*hqCount
                && turnCount >= 300
                && rc.getNumAnchors(Anchor.STANDARD) < 1
                && turnCount % anchorRate == 0
                && Comms.getNumIslands(rc) > 0) {
            tryingtoBuildAnchor = true;
            saveAd += Anchor.STANDARD.adamantiumCost;
            saveMn += Anchor.STANDARD.manaCost;
        }

        if (enemyLaunchers > 0 && turnCount > 20) {
            tryingToBuildAmp = false;
            shouldSave = true;
        }

        if (rc.getResourceAmount(ResourceType.MANA) >= Math.min(enemyLaunchers * 2, 5) * RobotType.LAUNCHER.buildCostMana) {
            shouldSave = false;
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
        if (enemyLauncher != null) {
            launcherDir = curLoc.directionTo(enemyLauncher.getLocation()).opposite();
        }
        while(!shouldSave && buildInDir(rc, RobotType.LAUNCHER, launcherDir));
        boolean keepBuilding = true;
        while(!shouldSave && keepBuilding) {
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
    static void firstTurn(RobotController rc) throws GameActionException {
        if(rc.getMapHeight() < 30 && rc.getMapWidth() < 30) smallMap = true;
        int numHqs = Comms.getNumHQs(rc);
        if(numHqs == 0) {
            numHqs = rc.getRobotCount();
            Comms.writeNumHQs(rc, numHqs);
        }
        Comms.writeHQ(rc, rc.getLocation());

        WellInfo[] wells = rc.senseNearbyWells();
        for (WellInfo well : wells) {
            if(well.getResourceType() == ResourceType.MANA) closeWellLoc = well.getMapLocation();
            Comms.writeWellLoc(rc, well);
        }

        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo robot: robots) {
            if(robot.getType() == RobotType.HEADQUARTERS && robot.getTeam() != rc.getTeam()) {
                closeEnemyHqLoc = robot.getLocation();
                Comms.writeEnemyHqLoc(rc, closeEnemyHqLoc);
            }
        }

        // Get current hq idx
        MapLocation[] friendlyHqs = Comms.getHQs(rc);
        int hqIdx = 0;
        for(int i = 0; i < Constants.MAX_HQS_STORED; ++i) {
            MapLocation curHqLoc = friendlyHqs[i];
            if(curHqLoc.equals(rc.getLocation())) {
                hqIdx = i;
                break;
            }
        }
        // For the last HQ, start ruling out symmetries
        if(hqIdx == numHqs - 1) {
            ruleOutSymmetries(rc);
            Comms.wipeEnemyHqLocs(rc);
//            rc.setIndicatorString("syms: "+Comms.getPossibleSyms(rc));
        }
    }

    private static void ruleOutSymmetries(RobotController rc) throws GameActionException {
        MapLocation[] enemyHqs = Comms.getEnemyHqLocs(rc);
        MapLocation[] allyHqs = Comms.getHQs(rc);
        int mapH = rc.getMapHeight();
        int mapW = rc.getMapWidth();
        // first bit - H, second bit - V, third bit - R
        int possibleSyms = 7;
        int radius = RobotType.HEADQUARTERS.visionRadiusSquared;

        // check H
        for(MapLocation cur : allyHqs) {
            MapLocation[] images = {
                    new MapLocation(cur.x, mapH-cur.y-1),
                    new MapLocation(mapW-cur.x-1, cur.y),
                    new MapLocation(mapW-cur.x-1, mapH-cur.y-1)
            };
            for(int i = 0; i < 3; ++i) {
                for (MapLocation ally : allyHqs) {
                    if (ally.isWithinDistanceSquared(images[i], radius)) {
                        boolean canSee = false;
                        for (MapLocation enemy : enemyHqs) {
                            if (enemy.equals(images[i])) {
                                canSee = true;
                                break;
                            }
                        }
                        if(!canSee) possibleSyms &= (7-(1<<i));
                    }
                }
            }
        }
        Comms.writePossibleSyms(rc, possibleSyms);
    }

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
}
