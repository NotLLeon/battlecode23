package v6_explore;

import battlecode.common.*;
import java.util.ArrayList;

public class Launcher extends Robot {

    private static MapLocation originHq = null;
    private static MapLocation[] targets;
    private static int targetInd = 0;
    private static boolean onTarget = false;
    private static int roundsNearTarget = 0;
    private static MapLocation defendPoint = null;

    static MapLocation meet = null;

    static enum LAUNCHER_STATE {
        GOTO_LOCATION, PATROL, DEFEND
    };

    static LAUNCHER_STATE state = LAUNCHER_STATE.GOTO_LOCATION;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        if(turnCount == 1) {
            generateTargets(rc);

            MapLocation spawn_hq = originHq;
            // int x = spawn_hq.x;
            // int y = spawn_hq.y;
            // double centerx = rc.getMapWidth() / 2.0;
            // double centery = rc.getMapHeight() / 2.0;

            MapLocation[] hqs = Comms.getHQs(rc);
            // // TODO: take other hq positions into account
            // if (Math.abs(centerx - x) < rc.getMapWidth() / 6.0) {
            //     meet = new MapLocation(x, y + 7 * (((centery-y) < 0) ? -1 : 1));
            // } else if (Math.abs(centery - y) < rc.getMapWidth() / 6.0) {
            //     meet = new MapLocation(x + 7 * (((centerx-x) < 0) ? -1 : 1), y);
            // } else {
            //     meet = new MapLocation(x + 5 * (((centerx-x) < 0) ? -1 : 1), y + 5 * (((centery-y) < 0) ? -1 : 1));
            // }
            // int xtot = 0;
            // int ytot = 0;
            // for (MapLocation hq : hqs) {
            //     xtot += hq.x;
            //     ytot += hq.y;
            // }
            // meet = new MapLocation((xtot + rc.getMapWidth()) / (hqs.length + 2), (ytot + rc.getMapHeight()) / (hqs.length + 2));
            
            meet = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);

        }
        MapLocation curLoc = rc.getLocation();
        MapLocation shot = tryToShoot(rc);
        if(shot != null) {
            Direction moveBack = curLoc.directionTo(shot).opposite();
            if(rc.canMove(moveBack)) rc.move(moveBack);
            else if(rc.canMove(moveBack.rotateLeft())) rc.move(moveBack.rotateLeft());
            else if(rc.canMove(moveBack.rotateRight())) rc.move(moveBack.rotateRight());
            onTarget = false;
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(4, rc.getTeam());
        RobotInfo[] adjRobots = rc.senseNearbyRobots(2, rc.getTeam());

        int numLaunchers = 0;
        for (RobotInfo rob : nearbyRobots) {
            if (rob.getType() == RobotType.LAUNCHER) numLaunchers++;
        }
        int adjLaunchers = 0;
        for (RobotInfo rob : adjRobots) {
            if (rob.getType() == RobotType.LAUNCHER) adjLaunchers++;
        }

        MapLocation[] distress = Comms.getDistressLocations(rc);
        if (distress.length > 0 && state != LAUNCHER_STATE.DEFEND) {
            state = LAUNCHER_STATE.DEFEND;
            defendPoint = distress[Random.nextInt(distress.length)];
        } else if (distress.length == 0 && state == LAUNCHER_STATE.DEFEND) {
            state = LAUNCHER_STATE.GOTO_LOCATION;
        } else if (state == LAUNCHER_STATE.PATROL && numLaunchers < 2) {
            state = LAUNCHER_STATE.GOTO_LOCATION;
        }

        switch (state) {
            case GOTO_LOCATION:
//                rc.setIndicatorString("GOTO_LOCATION");

                moveToOutsideRadius(rc, meet, 0);
                // moveToRadius(rc, meet, 4);

//                rc.setIndicatorString("NUMLAUNCHERS: " + numLaunchers + " ADJLAUNCH: " + adjLaunchers);
                if (numLaunchers >= 3 && adjLaunchers > 0) state = LAUNCHER_STATE.PATROL;
                break;
            case DEFEND:
                moveToRadius(rc, defendPoint, 3);
            break;
            case PATROL:
            default:
//                rc.setIndicatorString("PATROL: " + targets[targetInd]);
                if(!onTarget){
                    MapLocation curTarget = targets[targetInd];
                    moveToOutsideRadius(rc, curTarget, 9);
                    if(curLoc.isWithinDistanceSquared(curTarget, 16)) {
                        if(!canSeeHq(rc)) nextTarget();
                        else onTarget = true;
                    } else if(curLoc.isWithinDistanceSquared(curTarget, 16)) {
                        roundsNearTarget++;
                    }
                    if(!isReachable(rc, curTarget) || roundsNearTarget > 20) {
                        nextTarget();
                    }
                }
                break;
        }
        if(shot == null) shot = tryToShoot(rc);
        if(shot == null) takePotshot(rc);
    }

    private static ArrayList<MapLocation> shotLocs = new ArrayList<MapLocation>();
    private static void takePotshot(RobotController rc) throws GameActionException {
        if(!rc.isActionReady()) return;

        MapLocation curLoc = rc.getLocation();
        // shoot randomly at a position that is out of vision
        if(!rc.senseMapInfo(curLoc).hasCloud()) {
            shotLocs.clear();

            MapLocation[] cloudLocs = rc.senseNearbyCloudLocations();
            for (MapLocation cloudLoc : cloudLocs) {
//                System.out.println(loc.isWithinDistanceSquared(curLoc, 4) + " " + info.hasCloud());
                if (!cloudLoc.isWithinDistanceSquared(curLoc, 4) && rc.canActLocation(cloudLoc)) {
                    shotLocs.add(cloudLoc);
                }
            }
            if(!shotLocs.isEmpty()) rc.attack(shotLocs.get(Random.nextInt(shotLocs.size())));

        } else {

            // FIXME: make less jank
            Direction randomDir = Random.nextDir();
            int dis = Random.nextInt(2) + 3;
            MapLocation shootLoc = curLoc;
            while(dis-- > 0) shootLoc = shootLoc.add(randomDir);
            if(rc.canAttack(shootLoc)) {
                rc.attack(shootLoc);
                return;
            }

            randomDir = randomDir.rotateLeft();
            dis = Random.nextInt(2) + 3;
            shootLoc = curLoc;
            while(dis-- > 0) shootLoc = shootLoc.add(randomDir);
            if(rc.canAttack(shootLoc)) {
                rc.attack(shootLoc);
                return;
            }

            randomDir = randomDir.rotateLeft();
            dis = Random.nextInt(2) + 3;
            shootLoc = curLoc;
            while(dis-- > 0) shootLoc = shootLoc.add(randomDir);
            if(rc.canAttack(shootLoc)) {
                rc.attack(shootLoc);
            }
        }
//        rc.setIndicatorString(""+shotLocs.size());
    }

    private static boolean canSeeHq(RobotController rc) throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        for (RobotInfo enemy : enemies) {
            if (enemy.getType() == RobotType.HEADQUARTERS) return true;
        }
        return false;
    }

    private static MapLocation tryToShoot(RobotController rc) throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 1000;
        int smallestDistance = 100;
        RobotInfo target = null;

        for (RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) continue;
            int enemyHealth = enemy.getHealth();
            if (enemy.getType() == RobotType.LAUNCHER) {
                enemyHealth -= 2000;
            }
            int enemyDistance = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (enemyHealth < lowestHealth) {
                target = enemy;
                lowestHealth = enemyHealth;
                smallestDistance = enemyDistance;
            } else if (enemyHealth == lowestHealth) {
                if (enemyDistance < smallestDistance) {
                    target = enemy;
                    smallestDistance = enemyDistance;
                }
            }
        }
        if (target != null && rc.canAttack(target.getLocation())) {
            rc.attack(target.getLocation());
            return target.getLocation();
        }
        return null;
    }

    private static void nextTarget() {
        targetInd = (targetInd+1)%targets.length;
        roundsNearTarget = 0;
    }

    private static void generateTargets(RobotController rc) throws GameActionException {
        MapLocation [] Hqs = Comms.getHQs(rc);
        MapLocation curLoc = rc.getLocation();

        int mapW = rc.getMapWidth();
        int mapH = rc.getMapHeight();
        int closestDis = 999999;

        for(MapLocation hq : Hqs) {
            int dis = curLoc.distanceSquaredTo(hq);
            if(dis < closestDis) {
                closestDis = dis;
                originHq = hq;
            }
        }
        targets = new MapLocation[4];
        targets[0] = new MapLocation(
                mapW-originHq.x-1,
                mapH-originHq.y-1
        );
        targets[1] = new MapLocation(
                mapW-originHq.x-1,
                originHq.y
        );
        targets[2] = new MapLocation(
                originHq.x,
                mapH-originHq.y-1
        );
        targets[3] = originHq;
    }


}
