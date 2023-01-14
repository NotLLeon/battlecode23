package v2;

import battlecode.common.*;

public class Carrier extends Robot{

    public enum CARRIER_STATE {
        COLLECTING, EXPLORING, RETURNING, MOVE_TO_WELL
    }

    static CARRIER_STATE state = CARRIER_STATE.EXPLORING;
    static MapLocation current_objective = new MapLocation(0, 0); 

    static void runCarrier(RobotController rc, int turnCount) throws GameActionException {
        MapLocation current_location = rc.getLocation();
        switch (state) {
            case EXPLORING:
                rc.setIndicatorString("EXPLORING");

                WellInfo[] wells = rc.senseNearbyWells();
                if (wells.length > 0 && Random.nextInt(3) == 1) {
                    WellInfo well_one = wells[0];
                    current_objective = well_one.getMapLocation();
                    state = CARRIER_STATE.MOVE_TO_WELL;
                } else {
                    Direction dir = Explore.exploreAwayFromHQ(rc);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                } 
            break;
            case MOVE_TO_WELL:
                rc.setIndicatorString("MOVE_TO_WELL, Current Objective: (" + current_objective.x + ", " + current_objective.y + ")");

                if (current_location.isAdjacentTo(current_objective)) {
                    state = CARRIER_STATE.COLLECTING;
                } else {
                    moveTo(rc, current_objective);
                }
            break;
            case RETURNING:
                rc.setIndicatorString("RETURNING, Current Objective: (" + current_objective.x + ", " + current_objective.y + ")");

                if (current_location.isAdjacentTo(current_objective)) {
                    if (rc.canTransferResource(current_objective, ResourceType.ADAMANTIUM, 1)) {
                        rc.transferResource(current_objective, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
                    }
                    if (rc.canTransferResource(current_objective, ResourceType.MANA, 1)) {
                        rc.transferResource(current_objective, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
                    }
                    if (rc.canTransferResource(current_objective, ResourceType.ELIXIR, 1)) {
                        rc.transferResource(current_objective, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
                    }
                    state = CARRIER_STATE.EXPLORING;
                } else {
                    moveTo(rc, current_objective);
                }
            break;
            case COLLECTING:
                rc.setIndicatorString("COLLECTING");

                if (rc.canCollectResource(current_objective, -1)) {
                        // if (rng.nextBoolean()) {
                            rc.collectResource(current_objective, -1);
                            rc.setIndicatorString("Collecting, now have, AD:" +
                                    rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                                    " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                                    " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                        // }
                } else {
                   // int raw = rc.readSharedArray(0)-1;
                   // current_objective = new MapLocation(raw%rc.getMapWidth(), raw/rc.getMapWidth());
                    int t = 0;
                    int min_loc = 0;
                    int min_dist = 10000;
                    for (int i = 0; i < 8 && (t = rc.readSharedArray(i)) != 0; i++) {
                        int x = (t-1)%rc.getMapWidth();
                        int y = (t-1)/rc.getMapWidth();
                       // System.out.println("(" + x + ", " + y + ")");
                        int cur_dist = (x-current_location.x) * (x-current_location.x) + (y-current_location.y)*(y-current_location.y);
                        if (cur_dist < min_dist) {
                            min_dist = cur_dist;
                            min_loc = (t-1);
                        }
                    }
                    current_objective = new MapLocation(min_loc%rc.getMapWidth(), min_loc/rc.getMapWidth());
                    state = CARRIER_STATE.RETURNING;
                }
            break;
            default:
            break;
            // if (rc.getAnchor() != null) {
            //     // If I have an anchor singularly focus on getting it to the first island I see
            //     int[] islands = rc.senseNearbyIslands();
            //     Set<MapLocation> islandLocs = new HashSet<>();
            //     for (int id : islands) {
            //         MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
            //         islandLocs.addAll(Arrays.asList(thisIslandLocs));
            //     }
            //     if (islandLocs.size() > 0) {
            //         MapLocation islandLocation = islandLocs.iterator().next();
            //         rc.setIndicatorString("Moving my anchor towards " + islandLocation);
            //         while (!rc.getLocation().equals(islandLocation)) {
            //             Direction dir = rc.getLocation().directionTo(islandLocation);
            //             if (rc.canMove(dir)) {
            //                 rc.move(dir);
            //             }
            //         }
            //         if (rc.canPlaceAnchor()) {
            //             rc.setIndicatorString("Huzzah, placed anchor!");
            //             rc.placeAnchor();
            //         }
            //     }
            // }
            // boolean coll = false;
            // RobotInfo[] enemyRobots = rc.senseNearbyRobots(9, rc.getTeam().opponent());
            // if (enemyRobots.length > 0) {
            //     if (rc.canAttack(enemyRobots[0].location)) {
            //         rc.attack(enemyRobots[0].location);
            //     }
            //     if (turnCount < 500) {
            //         System.out.println("ATTACKING: " + enemyRobots[0].location);
            //     }
            // }

            // // Try to gather from squares around us.
            // for (int dx = -1; dx <= 1; dx++) {
            //     for (int dy = -1; dy <= 1; dy++) {
            //         MapLocation wellLocation = new MapLocation(me.x + dx, me.y + dy);
            //         if (rc.canCollectResource(wellLocation, -2)) {
            //             // if (rng.nextBoolean()) {
            //                 rc.collectResource(wellLocation, -2);
            //                 coll = true;
            //                 rc.setIndicatorString("Collecting, now have, AD:" +
            //                         rc.getResourceAmount(ResourceType.ADAMANTIUM) +
            //                         " MN: " + rc.getResourceAmount(ResourceType.MANA) +
            //                         " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
            //             // }
            //         }
            //     }
            // }

            // // If we can see a well, move towards it
            // if (!coll){
            // WellInfo[] wells = rc.senseNearbyWells();
            // if (wells.length > 1 && rng.nextInt(3) == 1) {
            //     WellInfo well_one = wells[1];
            //     Direction dir = me.directionTo(well_one.getMapLocation());
            //     if (rc.canMove(dir))
            //         rc.move(dir);
            // }
            // // Also try to move randomly.
            // Direction dir = directions[rng.nextInt(directions.length)];
            // if (rc.canMove(dir) && !coll) {
            //     rc.move(dir);
            // }
            // MapLocation testDepo = new MapLocation(me.x + 1, me.y + 1);
            // // if (rc.canTransferResource(testDepo, ResourceType.ADAMANTIUM, 1)) {
                
            // // }
            // }
        }
    }

}
