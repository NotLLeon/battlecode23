package v3CappedArc;

import battlecode.common.*;

import java.util.HashSet;

public class Carrier extends Robot{

    public enum CARRIER_STATE {
        COLLECTING, EXPLORING, RETURNING, MOVE_TO_WELL, ANCHOR, SKY_ISLAND
    }

    static HashSet<Integer> ad_well_locs = new HashSet<Integer>();
    static HashSet<Integer> mana_well_locs = new HashSet<Integer>();
    static HashSet<Integer> elixir_well_locs = new HashSet<Integer>();

    static CARRIER_STATE state = CARRIER_STATE.EXPLORING;
    static MapLocation current_objective = new MapLocation(0, 0); 

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


    static void runCarrier(RobotController rc, int turnCount) throws GameActionException {
        MapLocation current_location = rc.getLocation();
        switch (state) {
            case EXPLORING:
                rc.setIndicatorString("EXPLORING");

                WellInfo[] wells = rc.senseNearbyWells();

                for (int i = 0; i < wells.length; i++) {
                    if (wells[i].getResourceType() == ResourceType.ADAMANTIUM) {
                        ad_well_locs.add(Comms.encodeWellLoc(rc, wells[i].getMapLocation()));
                    } else if (wells[i].getResourceType() == ResourceType.MANA) {
                        mana_well_locs.add(Comms.encodeWellLoc(rc, wells[i].getMapLocation()));
                    } else if (wells[i].getResourceType() == ResourceType.ELIXIR) {
                        mana_well_locs.add(Comms.encodeWellLoc(rc, wells[i].getMapLocation()));
                    }
                }

                if (wells.length > 0 && Random.nextInt(3) == 1) {
                    WellInfo well_one = wells[0];
                    current_objective = well_one.getMapLocation();
                    state = CARRIER_STATE.MOVE_TO_WELL;
                } else {
                    Direction dir = Explore.exploreAwayFromHQ(rc, getClosestHQ(rc));
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
                    boolean skip = false;
                    if (rc.canTransferResource(current_objective, ResourceType.ADAMANTIUM, 1)) {
                        rc.transferResource(current_objective, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
                        skip = true;
                    }
                    if (rc.canTransferResource(current_objective, ResourceType.MANA, 1)) {
                        rc.transferResource(current_objective, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
                        skip = true;
                    }
                    if (rc.canTransferResource(current_objective, ResourceType.ELIXIR, 1)) {
                        rc.transferResource(current_objective, ResourceType.ELIXIR, rc.getResourceAmount(ResourceType.ELIXIR));
                        skip = true;
                    }

                    Comms.writeWellLocs(rc, ad_well_locs);
                    Comms.writeWellLocs(rc, mana_well_locs);
                    if (!skip) {
                        if (rc.canTakeAnchor(current_objective, Anchor.STANDARD)) {
                            rc.takeAnchor(current_objective, Anchor.STANDARD);
                            state = CARRIER_STATE.ANCHOR;
                        } else {
                            state = CARRIER_STATE.EXPLORING;
                        }
                    }
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
                    current_objective = getClosestHQ(rc);
                    state = CARRIER_STATE.RETURNING;
                }
            break;
            case ANCHOR:
                rc.setIndicatorString("ANCHOR");

                int[] islands = rc.senseNearbyIslands();
                // use senseTeamOccupyingIsland(idx) to skip already owned islands
                if (islands.length > 0 && Random.nextInt(3) == 1) {
                    current_objective = rc.senseNearbyIslandLocations(islands[0])[0];
                    state = CARRIER_STATE.SKY_ISLAND;
                } else {
                    Direction dir = Explore.exploreAwayFromHQ(rc, getClosestHQ(rc));
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                } 
            break;
            case SKY_ISLAND:
            rc.setIndicatorString("SKY_ISLAND, Current Objective: (" + current_objective.x + ", " + current_objective.y + ")");

            if (rc.canPlaceAnchor()) {
                rc.placeAnchor();
                state = CARRIER_STATE.EXPLORING;
            }
            else {
                moveTo(rc, current_objective);
            }
            break;
            default:
            break;
        }
    }
}
