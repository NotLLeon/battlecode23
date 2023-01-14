package v1;

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
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }
    }
}
