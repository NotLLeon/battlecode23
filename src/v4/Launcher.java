package v4;

import battlecode.common.*;

public class Launcher extends Robot {

    static MapLocation originHq = null;
    static MapLocation targetHq = null;

    static boolean delayRush = false;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        // Get target location
        // FIXME: assumes 180 deg rotation

        if(turnCount == 1) {

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

            targetHq = new MapLocation(
                    mapW-originHq.x-1,
                    mapH-originHq.y-1
            );
        }
        rc.setIndicatorString("Target:" + targetHq);

        // Attack if possible
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
        if (target != null){
            if (rc.canAttack(target.getLocation()))
                rc.attack(target.getLocation());
        } else {
            if(delayRush && rc.getRoundNum() < 250) {
                Direction randomDir = Random.nextDir();
                MapLocation curLoc = rc.getLocation();
                for(int i = 0; i < 8; ++i) {
                    MapLocation nextLoc = curLoc.add(randomDir);
                    if(rc.canMove(randomDir) && nextLoc.isWithinDistanceSquared(originHq, 16)) {
                        rc.move(randomDir);
                        break;
                    }
                }
            } else {
                moveToRadius(rc, targetHq, 2);
                if(!isReachable(rc, targetHq)) {
//                    rc.disintegrate();
                    System.out.println("cant reach" + targetHq);
                }
            }
        }
    }
}
