package testrushexplorebot1;

import battlecode.common.*;

public class Explore {
    static Direction exploreAwayFromHQ(RobotController rc) throws GameActionException {
        MapLocation hqLoc = new MapLocation(rc.readSharedArray(0) % rc.getMapWidth(), rc.readSharedArray(0) / rc.getMapHeight());
        Direction hqDir = rc.getLocation().directionTo(hqLoc);

        // 8 Directions, init all weight 1
        int weights [] = {1, 1, 1, 1, 1, 1, 1, 1};

        // Directions pointing towards hq lowest weight
        // Directions away from hq higher weight
        weights[Random.getDirectionOrderNum(hqDir.opposite().rotateLeft())] *= 2;
        weights[Random.getDirectionOrderNum(hqDir.opposite().rotateRight())] *= 2;
        weights[Random.getDirectionOrderNum(hqDir.opposite())] *= 2;

        // Perpendicular directions highest weight
        weights[Random.getDirectionOrderNum(hqDir.rotateLeft().rotateLeft())] *= 4;
        weights[Random.getDirectionOrderNum(hqDir.rotateRight().rotateRight())] *= 4;

        // Get direction using weights until can move
        Direction dir = Random.nextDirWeighted(weights, Constants.TOTAL_WEIGHT_DIRECTIONS);
        while(!rc.canMove(dir)) dir = Random.nextDir();
        return dir;
    }
}
