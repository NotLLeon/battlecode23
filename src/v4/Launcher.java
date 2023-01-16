package v4;

import battlecode.common.*;

public class Launcher extends Robot {

    private static MapLocation originHq = null;
    private static MapLocation[] targets;
    private static int targetInd = 0;
    private static boolean onTarget = false;
    private static int roundsNearTarget = 0;
    private static boolean delayRush = false;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        // Get target location
        // FIXME: assumes 180 deg rotation

        if(turnCount == 1) {
            generateTargets(rc);
        }
        // Attack if possible
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 100;
        int smallestDistance = 100;
        RobotInfo target = null;

        boolean canSeeHq = false;
        for (RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) {
                canSeeHq = true;
                continue;
            }
            int enemyHealth = enemy.getHealth();
            if (enemy.type == RobotType.LAUNCHER) {
                enemyHealth -= 200;
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
        MapLocation curLoc = rc.getLocation();
        if (target != null){
            if (rc.canAttack(target.getLocation()))
                rc.attack(target.getLocation());
        } else if(!onTarget){
            if(delayRush && rc.getRoundNum() < 250) {
                Direction randomDir = Random.nextDir();
                for(int i = 0; i < 8; ++i) {
                    MapLocation nextLoc = curLoc.add(randomDir);
                    if(rc.canMove(randomDir) && nextLoc.isWithinDistanceSquared(originHq, 16)) {
                        rc.move(randomDir);
                        break;
                    }
                }
            } else {
                MapLocation curTarget = targets[targetInd];
                moveToRadius(rc, curTarget, 4);
                if(curLoc.isWithinDistanceSquared(curTarget, 4)) {
                    if(!canSeeHq) nextTarget();
                    else onTarget = true;
                } else if(curLoc.isWithinDistanceSquared(curTarget, 16)) {
                    roundsNearTarget++;
                }
                if(!isReachable(rc, curTarget) || roundsNearTarget > 20) {
                    nextTarget();
                    rc.setIndicatorString("moving to next target");
                }
            }
        }
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
        if(mapW >= 30 && mapH >= 30) delayRush = true;
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
