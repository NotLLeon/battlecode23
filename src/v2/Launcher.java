package v2;

import battlecode.common.*;

public class Launcher extends Robot{

    static MapLocation target;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        // Get target location
        if(turnCount == 1) {
            MapLocation curLoc = rc.getLocation();

            // FIXME: assumes 180 deg rotation
            target = new MapLocation(rc.getMapWidth()-curLoc.x-1, rc.getMapHeight()-curLoc.y-1);
        }
        rc.setIndicatorString("Target:" + target);
        // Move if possible
        moveTo(rc, target);

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
    }
}
