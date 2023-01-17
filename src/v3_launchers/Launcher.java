package v3_launchers;

import battlecode.common.*;

public class Launcher extends Robot{

    static MapLocation targetHq = null;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        // Get target location
        // FIXME: assumes 180 deg rotation
        if(turnCount == 1) {

            MapLocation [] Hqs = Comms.getHQs(rc);
            MapLocation curLoc = rc.getLocation();

            int mapW = rc.getMapWidth();
            int mapH = rc.getMapHeight();
            int closestDis = 999999;

            for(MapLocation hq : Hqs) {
                MapLocation enemyHq = new MapLocation(
                        mapW-hq.x-1,
                        mapH-hq.y-1
                );
                int dis = curLoc.distanceSquaredTo(enemyHq);
                if(dis < closestDis) {
                    closestDis = dis;
                    targetHq = enemyHq;
                }
            }
        }
        rc.setIndicatorString("Target:" + targetHq);

        // Attack if possible
        int lowestHealth = 100;
        int smallestDistance = 100;
        RobotInfo target = null;

        RobotInfo[] enemies = rc.senseNearbyRobots(16, rc.getTeam().opponent());

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
        }
        // Move if possible
        if (enemies.length == 0) {
            moveTo(rc, targetHq);
        }

        // // Attack if possible
        // int lowestHealth = 100;
        // int smallestDistance = 100;
        // RobotInfo target = null;

        // for (RobotInfo enemy : enemies) {
        //     if(enemy.getType() == RobotType.HEADQUARTERS) continue;
        //     int enemyHealth = enemy.getHealth();
        //     if (enemy.type == RobotType.LAUNCHER) {
        //         enemyHealth -= 200;
        //     }
        //     int enemyDistance = enemy.getLocation().distanceSquaredTo(rc.getLocation());
        //     if (enemyHealth < lowestHealth) {
        //         target = enemy;
        //         lowestHealth = enemyHealth;
        //         smallestDistance = enemyDistance;
        //     } else if (enemyHealth == lowestHealth) {
        //         if (enemyDistance < smallestDistance) {
        //             target = enemy;
        //             smallestDistance = enemyDistance;
        //         }
        //     }
        // }
        // if (target != null){
        //     if (rc.canAttack(target.getLocation()))
        //         rc.attack(target.getLocation());
        // }
    }
}
