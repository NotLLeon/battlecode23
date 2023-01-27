package v7;

import battlecode.common.*;

import java.util.ArrayList;

public class Launcher extends Robot {

    private static MapLocation originHq = null;
    private static MapLocation[] targets;
    private static int targetInd = 0;
    private static int roundsNearTarget = 0;
    private static MapLocation defendPoint = null;

    static MapLocation meet = null;

    enum LAUNCHER_STATE {
        GOTO_LOCATION, PATROL, DEFEND, ON_TARGET
    };

    static LAUNCHER_STATE state = LAUNCHER_STATE.PATROL;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        rc.setIndicatorString("" + state);
        if(turnCount == 1) {
            originHq = getClosestHQ(rc);
            generateTargets(rc);
        }
        setMeetLocation(rc, turnCount);
        MapLocation curLoc = rc.getLocation();
        MapLocation shot = tryToShoot(rc);
        if(shot != null && state != LAUNCHER_STATE.ON_TARGET) {
            Direction moveBack = curLoc.directionTo(shot).opposite();
            if(rc.canMove(moveBack)) rc.move(moveBack);
            else if(rc.canMove(moveBack.rotateLeft())) rc.move(moveBack.rotateLeft());
            else if(rc.canMove(moveBack.rotateRight())) rc.move(moveBack.rotateRight());
            state = LAUNCHER_STATE.PATROL;
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(4, rc.getTeam());

        int numLaunchers = 0;

        for (RobotInfo rob : nearbyRobots) {
            if (rob.getType() == RobotType.LAUNCHER) numLaunchers++;
        }

        if(state != LAUNCHER_STATE.ON_TARGET) {
            MapLocation[] distress = Comms.getDistressLocations(rc);
            if (distress.length > 0 && state != LAUNCHER_STATE.DEFEND) {
                if(rc.getRoundNum() > 200) {
                    state = LAUNCHER_STATE.DEFEND;
                    defendPoint = distress[Random.nextInt(distress.length)];
                } else {
                    for(MapLocation disLoc : distress) {
                        if(disLoc.equals(originHq)) {
                            state = LAUNCHER_STATE.DEFEND;
                            defendPoint = disLoc;
                            break;
                        }
                    }
                }
            }
            else if (distress.length == 0 && state == LAUNCHER_STATE.DEFEND) {
                // state = LAUNCHER_STATE.GOTO_LOCATION;
                state = LAUNCHER_STATE.PATROL;
            }
            // else if (state == LAUNCHER_STATE.PATROL && numLaunchers < 2) {
            //     state = LAUNCHER_STATE.GOTO_LOCATION;
            // }
        }

        runLauncherState(rc);
        tryToShoot(rc);
        takePotshot(rc);
    }
    private static void runLauncherState(RobotController rc) throws GameActionException {
        switch (state) {
            case GOTO_LOCATION: runLauncherGoto(rc); break;
            case DEFEND:        runLauncherDefend(rc); break;
            case PATROL:        runLauncherPatrol(rc); break;
            case ON_TARGET:     runLauncherOnTarget(rc); break;
        }
    }

    private static void runLauncherGoto(RobotController rc) throws GameActionException {
//        rc.setIndicatorString("GOTO_LOCATION: " + meet);
        moveToOutsideRadius(rc, meet, 0);
        MapLocation curLoc = rc.getLocation();

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(4, rc.getTeam());

        int numLaunchers = 0;
        int adjLaunchers = 0;

        for (RobotInfo rob : nearbyRobots) {
            if (rob.getType() == RobotType.LAUNCHER) {
                if(rob.getLocation().isAdjacentTo(curLoc)) adjLaunchers++;
                numLaunchers++;
            }
        }
//                rc.setIndicatorString("NUMLAUNCHERS: " + numLaunchers + " ADJLAUNCH: " + adjLaunchers);
        if (numLaunchers >= 3 && adjLaunchers > 2) {
            state = LAUNCHER_STATE.PATROL;
            runLauncherState(rc);
        }
    }

    private static void runLauncherDefend(RobotController rc) throws GameActionException {
        moveToOutsideRadius(rc, defendPoint, 9);
        if (rc.isMovementReady()) {
            Direction rdmMove = Random.nextDir();
            if (rc.canMove(rdmMove)) {
                rc.move(rdmMove);
            }
        }
    }

    private static void runLauncherPatrol(RobotController rc) throws GameActionException {
//        rc.setIndicatorString(""+onTarget);
        MapLocation curLoc = rc.getLocation();
        if(state != LAUNCHER_STATE.ON_TARGET){
            MapLocation curTarget = targets[targetInd];
            moveToOutsideRadius(rc, curTarget, 10);
            if(rc.canSenseLocation(curTarget)) {
                if(canSeeHq(rc)) {
                    state = LAUNCHER_STATE.ON_TARGET;
                    runLauncherState(rc);
                } else {
                    nextTarget();
                }
            } else if(curLoc.isWithinDistanceSquared(curTarget, 16)) {
                roundsNearTarget++;
            }
            if(!isReachable(rc, curTarget) || roundsNearTarget > 15) {
                nextTarget();
            }
        }
    }
    private static void runLauncherOnTarget(RobotController rc) throws GameActionException {
        MapLocation curTarget = targets[targetInd];
        if(curTarget.distanceSquaredTo(rc.getLocation()) == 25) return;
        moveToOutsideRadius(rc, curTarget, 16);
    }


    private static ArrayList<MapLocation> shotLocs = new ArrayList<MapLocation>();
    private static void takePotshot(RobotController rc) throws GameActionException {
        if(!rc.isActionReady()) return;
        shotLocs.clear();
        MapLocation curLoc = rc.getLocation();
        // shoot randomly at a position that is out of vision
        if(!rc.senseMapInfo(curLoc).hasCloud()) {
            MapLocation[] cloudLocs = rc.senseNearbyCloudLocations();
            for (MapLocation cloudLoc : cloudLocs) {
//                System.out.println(loc.isWithinDistanceSquared(curLoc, 4) + " " + info.hasCloud());
                if (!cloudLoc.isWithinDistanceSquared(curLoc, 4) && rc.canActLocation(cloudLoc)) {
                    shotLocs.add(cloudLoc);
                }
            }
        } else {
            // TODO: shoot at some location outside vision radius (4)
        }
//        rc.setIndicatorString(""+shotLocs.size());
        if(!shotLocs.isEmpty()) rc.attack(shotLocs.get(Random.nextInt(shotLocs.size())));
    }

    private static boolean canSeeHq(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots();
        for (RobotInfo enemy : enemies) {
            if (enemy.getType() == RobotType.HEADQUARTERS && enemy.getTeam() != rc.getTeam()) return true;
        }
        return false;
    }

    private static MapLocation tryToShoot(RobotController rc) throws GameActionException {
        if(!rc.isActionReady()) return null;
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

        int mapW = rc.getMapWidth();
        int mapH = rc.getMapHeight();

        targets = new MapLocation[3];
        targets[0] = new MapLocation(
                mapW-originHq.x-1,
                originHq.y
        );
        targets[1] = new MapLocation(
                originHq.x,
                mapH-originHq.y-1
        );
        targets[2] = new MapLocation(
            mapW-originHq.x-1,
            mapH-originHq.y-1
        );

        int t1 = 60 * 60;
        int t2 = 60 * 60;
        for (MapLocation hq : Hqs) {
            int t1dist = hq.distanceSquaredTo(targets[0]);
            int t2dist = hq.distanceSquaredTo(targets[1]);
            if (t1dist < t1) {
                t1 = t1dist;
            }
            if (t2dist < t2) {
                t2 = t2dist;
            }
        }
        if (t2 > t1) {
            MapLocation temp = targets[0];
            targets[0] = targets[1];
            targets[1] = targets[2];
            targets[2] = temp;
        } else {
            MapLocation temp = targets[1];
            targets[1] = targets[2];
            targets[2] = temp;
        }

        if (Hqs.length == 1 && Math.abs(originHq.x - mapW/2) > mapW*3/8 && Math.abs(originHq.y - mapH/2) > mapH*3/8) {
            MapLocation temp = targets[1];
            targets[1] = targets[0];
            targets[0] = temp;
        }
    }

    private static void setMeetLocation(RobotController rc, int turnCount) throws GameActionException {
        MapLocation spawn_hq = originHq;
        int x = spawn_hq.x;
        int y = spawn_hq.y;
        double centerx = rc.getMapWidth() / 2.0;
        double centery = rc.getMapHeight() / 2.0;

        MapLocation[] hqs = Comms.getHQs(rc);
        // // TODO: take other hq positions into account
        // if (Math.abs(centerx - x) < rc.getMapWidth() / 6.0) {
        //     meet = new MapLocation(x, y + (4 + (turnCount / 30)) * (((centery-y) < 0) ? -1 : 1));
        // } else if (Math.abs(centery - y) < rc.getMapWidth() / 6.0) {
        //     meet = new MapLocation(x + (4 + (turnCount / 30)) * (((centerx-x) < 0) ? -1 : 1), y);
        // } else {
        //     meet = new MapLocation(x + (3 + turnCount / 30) * (((centerx-x) < 0) ? -1 : 1), y + (3 + (turnCount / 30)) * (((centery-y) < 0) ? -1 : 1));
        // }
        // MapLocation center = new MapLocation((int)centerx, (int)centery);
        // if (spawn_hq.distanceSquaredTo(meet) > spawn_hq.distanceSquaredTo(center)) {
        //     meet = center;
        // }
        if (Math.abs(centerx - x) < rc.getMapWidth() / 6.0) {
            meet = new MapLocation(x, y + 7 * (((centery-y) < 0) ? -1 : 1));
        } else if (Math.abs(centery - y) < rc.getMapWidth() / 6.0) {
            meet = new MapLocation(x + 7 * (((centerx-x) < 0) ? -1 : 1), y);
        } else {
            meet = new MapLocation(x + 5 * (((centerx-x) < 0) ? -1 : 1), y + 5 * (((centery-y) < 0) ? -1 : 1));
        }
        int xtot = 0;
        int ytot = 0;
        for (MapLocation hq : hqs) {
            xtot += hq.x;
            ytot += hq.y;
        }
        meet = new MapLocation((xtot + rc.getMapWidth()) / (hqs.length + 2), (ytot + rc.getMapHeight()) / (hqs.length + 2));
    }

}
