package harbinger;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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
    static MapLocation getClosestHQ(RobotController rc) throws GameActionException {
        MapLocation current_location = rc.getLocation();
        int t = 0;
        int min_loc = 0;
        int min_dist = 10000;
        for (int i = 0; i < 8 && (t = rc.readSharedArray(i)) != 0; i++) {
            int x = (t-1)%rc.getMapWidth();
            int y = (t-1)/rc.getMapWidth();
            int cur_dist = (x-current_location.x) * (x-current_location.x) + (y-current_location.y)*(y-current_location.y);
            if (cur_dist < min_dist) {
                min_dist = cur_dist;
                min_loc = (t-1);
            }
        }
        return new MapLocation(min_loc%rc.getMapWidth(), min_loc/rc.getMapWidth());
    }
}
