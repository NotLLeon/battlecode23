package testbot1;

import battlecode.common.*;

public class Launcher {

    static MapLocation target;
    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {

        // Get target location
        if(turnCount == 1) {
            MapLocation curLoc = rc.getLocation();

            // FIXME: assumes 180 deg rotation
            target = new MapLocation(rc.getMapWidth() - curLoc.x, rc.getMapHeight()-curLoc.y);
        }

        // Move if possible
        if(rc.isMovementReady()) rc.move(Pathfind.getDir(rc, target));

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
