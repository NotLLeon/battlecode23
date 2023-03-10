package v6;

import battlecode.common.*;

import java.util.LinkedList;

public class Headquarters extends Robot {

    static int index = 0;
    static int hqCount = 0;
    // static int amplifiers = 5;
    static boolean smallMap = false;
    static MapLocation closeEnemyHqLoc = null;
    static MapLocation closeWellLoc = null;
    static boolean tryingtoBuildAnchor = false;

    static LinkedList<Integer> prev_mana = new LinkedList<Integer>();
    static LinkedList<Integer> prev_ad = new LinkedList<Integer>();
    static int prev_mana_sum = 0;
    static int prev_ad_sum = 0;
    static int num_rounds_to_average = 60;

    public enum Symmetry {
        ROTATIONAL,
        HORIZONTAL,
        VERTICAL
    }

    static void runHeadquarters(RobotController rc, int turnCount) throws GameActionException {
        // Pick a direction to build in.
        MapLocation curLoc = rc.getLocation();

        rc.setIndicatorString("Ad revenue: " + Comms.getAverageAdRevenue(rc) + " Mana revenue: " + Comms.getAverageManaRevenue(rc));

        int spawned_carriers = 0;
        int spawned_launchers = 0;
        int spawned_standard_anchors = 0;
        int spawned_amplifiers = 0;
        int owned_ad = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        int owned_mana = rc.getResourceAmount(ResourceType.MANA);

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

        // TODO: rewrite
        if (currRobotCount > 20*hqCount
                && turnCount >= 750
//                && spawnInd < spawnSpaces
//                && rc.canBuildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd])
                && turnCount % 100 == 0) {
//            rc.buildRobot(RobotType.AMPLIFIER, spawnLocs[spawnInd++]);
            buildInDir(rc, RobotType.AMPLIFIER, dirToCent);
            spawned_amplifiers++;
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
            spawned_standard_anchors++;
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

            if (!stop1) {
                spawned_carriers += (tryFirst == RobotType.CARRIER) ? 1 : 0;
                spawned_launchers += (tryFirst == RobotType.LAUNCHER) ? 1 : 0;
            }
        }
        boolean stop2 = false;
        while(!stop2) {
            if(buildDir2 == Direction.CENTER) stop2 = !buildInDir(rc, trySecond, Random.nextDir());
            else stop2 = !buildInDir(rc, trySecond, buildDir2);

            if (!stop2) {
                spawned_carriers += (trySecond == RobotType.CARRIER) ? 1 : 0;
                spawned_launchers += (trySecond == RobotType.LAUNCHER) ? 1 : 0;
            }
        }

        //Putting the check here just in case.
        //TODO:Fix this bugged code
        if (Comms.getNumHQs(rc) > 0) {
            int old_ad = Comms.getTotalAd(rc);
            int old_mana = Comms.getTotalMana(rc);
            int ad_revenue = Math.max(0, rc.getResourceAmount(ResourceType.ADAMANTIUM)
                    + spawned_carriers * RobotType.CARRIER.getBuildCost(ResourceType.ADAMANTIUM)
                    + spawned_launchers * RobotType.LAUNCHER.getBuildCost(ResourceType.ADAMANTIUM)
                    + spawned_amplifiers * RobotType.AMPLIFIER.getBuildCost(ResourceType.ADAMANTIUM)
                    + spawned_standard_anchors * Anchor.STANDARD.getBuildCost(ResourceType.ADAMANTIUM)
                     + 2);
            int mana_revenue = Math.max(0, rc.getResourceAmount(ResourceType.MANA)
                    + spawned_carriers * RobotType.CARRIER.getBuildCost(ResourceType.MANA)
                    + spawned_launchers * RobotType.LAUNCHER.getBuildCost(ResourceType.MANA)
                    + spawned_amplifiers * RobotType.AMPLIFIER.getBuildCost(ResourceType.MANA)
                    + spawned_standard_anchors * Anchor.STANDARD.getBuildCost(ResourceType.MANA)
                     + 2);
            if (Comms.getHQs(rc)[0].equals(curLoc)) {
                Comms.setTotalAd(rc,ad_revenue);
                Comms.setTotalMana(rc,mana_revenue);
            } else {
                Comms.setTotalAd(rc,old_ad + ad_revenue);
                Comms.setTotalMana(rc,old_mana + mana_revenue);
            }
            if (Comms.getHQs(rc)[Comms.getNumHQs(rc)-1].equals(curLoc)) {
                prev_mana.addLast(old_mana + mana_revenue);
                prev_ad.addLast(old_ad + ad_revenue);
                prev_mana_sum += prev_mana.getLast();
                prev_ad_sum += prev_ad.getLast();

                if (prev_mana.size() > num_rounds_to_average) {
                    prev_mana_sum -= prev_mana.getFirst();
                    prev_mana.removeFirst();
                }
                if (prev_ad.size() > num_rounds_to_average) {
                    prev_ad_sum -= prev_ad.getFirst();
                    prev_ad.removeFirst();
                }
                int average_ad_revenue = prev_ad_sum;// / prev_ad.size();
                int average_mana_revenue = prev_mana_sum;// / prev_mana.size();
                Comms.setAverageAdRevenue(rc, average_ad_revenue);
                Comms.setAverageManaRevenue(rc, average_mana_revenue);
            }
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
