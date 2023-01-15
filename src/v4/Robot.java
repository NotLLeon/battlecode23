package v4;

import battlecode.common.*;

public abstract class Robot {

    static Direction currentDirection = null;

    public static void moveTo(RobotController rc, MapLocation dest) throws GameActionException {

        if (rc.getLocation().equals(dest)) {
            return;
        }
//        if (!rc.isActionReady()) {
//            return;
//        }
        Direction d = rc.getLocation().directionTo(dest);
        if (rc.canMove(d)) {
            rc.move(d);
            currentDirection = null; // there is no obstacle we're going around
        } else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
    }
}
