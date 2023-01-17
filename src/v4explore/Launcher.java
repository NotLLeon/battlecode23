package v4explore;

import battlecode.common.*;

public class Launcher extends Robot {

    private static MapLocation originHq = null;
    private static MapLocation[] targets;
    private static int targetInd = 0;
    private static boolean onTarget = false;
    private static int roundsNearTarget = 0;
    private static boolean delayRush = false;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        if(turnCount == 1) {
            generateTargets(rc);
        }
        MapLocation curLoc = rc.getLocation();
        if(tryToShoot(rc)) return;
        if(!onTarget){
            if(delayRush && rc.getRoundNum() < 250) {
                if(!curLoc.isWithinDistanceSquared(originHq, 16)) moveTo(rc, originHq);
                else {
                    Direction randomDir = Random.nextDir();
                    for (int i = 0; i < 8; ++i) {
                        MapLocation nextLoc = curLoc.add(randomDir);
                        if (rc.canMove(randomDir) && nextLoc.isWithinDistanceSquared(originHq, 16)) {
                            rc.move(randomDir);
                            break;
                        }
                    }
                }
            } else {
                MapLocation curTarget = targets[targetInd];
                moveToRadius(rc, curTarget, 4);
                if(curLoc.isWithinDistanceSquared(curTarget, 4)) {
                    if(!canSeeHq(rc)) nextTarget();
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
        tryToShoot(rc);
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

    private static boolean tryToShoot(RobotController rc) throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 100;
        int smallestDistance = 100;
        RobotInfo target = null;

        for (RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) continue;
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
        if (target != null && rc.canAttack(target.getLocation())) {
            rc.attack(target.getLocation());
            return true;
        }
        return false;
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
