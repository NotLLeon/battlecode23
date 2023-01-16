package v3_2;

import battlecode.common.*;

import java.util.HashSet;

public class Carrier extends Robot {

    public enum CARRIER_STATE {
        COLLECTING, EXPLORING, RETURNING, MOVE_TO_WELL, ANCHORING
    }

    static HashSet<Integer> ad_well_locs = new HashSet<Integer>();
    static HashSet<Integer> mana_well_locs = new HashSet<Integer>();
    static HashSet<Integer> elixir_well_locs = new HashSet<Integer>();

    static CARRIER_STATE state = CARRIER_STATE.EXPLORING;
    static MapLocation current_objective = new MapLocation(0, 0);
    static int island_objective_id = 0;

    private static void decide_role(RobotController rc) throws GameActionException {
        //int num_wells = Comms.getNumWells(rc);

        int num_mana_wells = Comms.getNumManaWells(rc);
        int num_ad_wells = Comms.getNumAdWells(rc);

        int random_choice = Random.nextInt(num_mana_wells + num_ad_wells+1);

        if (random_choice != 0) {
            if (num_mana_wells > 0 && num_ad_wells > 0) {
                //Set to 4 to be impossible (temp).
                if (Random.nextInt(3) == 1) {
                    //Adamantium
                    int random_index = Random.nextInt(num_ad_wells);
                    current_objective = Comms.getAdWell(rc, random_index);
                    state = CARRIER_STATE.MOVE_TO_WELL;
                } else {
                    //Mana
                    int random_index = Random.nextInt(num_mana_wells);
                    current_objective = Comms.getManaWell(rc, random_index);
                    state = CARRIER_STATE.MOVE_TO_WELL;
                }
            } else {
                int offset = (num_ad_wells > 0) ? 0 : 10;
                int random_index = (offset == 0) ? Random.nextInt(num_ad_wells) : Random.nextInt(num_mana_wells);
                current_objective = Comms.decodeWellLoc(rc, rc.readSharedArray(Comms.getNumHQs(rc) + offset + random_index));
                state = CARRIER_STATE.MOVE_TO_WELL;
            }
        } else {
            state = CARRIER_STATE.EXPLORING;
        }
    }

    static void runCarrier(RobotController rc, int turnCount) throws GameActionException {
        MapLocation current_location = rc.getLocation();

        //Decide Initial Role, will anchor on returning.
        if (turnCount == 1) {
            decide_role(rc);
        }

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

                int chance = Random.nextInt(3);

                if (wells.length > 0 && chance + ((wells[0].getResourceType() == ResourceType.MANA) ? 1 : 0) > 1) {
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
                rc.setIndicatorString("MOVE_TO_WELL, Current Objective: (" + current_objective.x + ", " + current_objective.y + "), WELLS: " + Comms.getNumWells(rc));

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

                    Comms.writeWellLocs(rc, ad_well_locs, ResourceType.ADAMANTIUM);
                    Comms.writeWellLocs(rc, mana_well_locs, ResourceType.MANA);

                    int num_islands = Comms.getNumIslands(rc);
                    if (num_islands > 0) {
                        //Uniformly, randomly, pick an island to go to.
                        int random = Random.nextInt(num_islands);
                        // isn't actually being used
                        if (rc.canTakeAnchor(current_objective, Anchor.ACCELERATING)) {
                            rc.takeAnchor(current_objective, Anchor.ACCELERATING);
                            current_objective = Comms.getIsland(rc, random);
                            island_objective_id = Comms.getIslandID(rc, random);
                            state = CARRIER_STATE.ANCHORING;
                            break;
                        }
                        if (rc.canTakeAnchor(current_objective, Anchor.STANDARD)) {
                            rc.takeAnchor(current_objective, Anchor.STANDARD);
                            current_objective = Comms.getIsland(rc, random);
                            island_objective_id = Comms.getIslandID(rc, random);
                            state = CARRIER_STATE.ANCHORING;
                            break;
                        }
                        if (rc.senseRobotAtLocation(current_objective).getTotalAnchors() > 0) {
                            //Clock.yield();
                            break;
                        }
                    }
                    decide_role(rc);
                } else {
                    current_objective = getClosestHQ(rc);
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
            case ANCHORING:
                rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") SKY: " + Comms.getNumIslands(rc));
                int island_id = rc.senseIsland(current_location);
                if (island_id != -1) {
                    rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Within Range)");
                    if (rc.canPlaceAnchor()) {
                        rc.placeAnchor();
                        current_objective = getClosestHQ(rc);
                        state = CARRIER_STATE.RETURNING;
                        break;
                    }
                    if (rc.senseAnchor(island_id) != null) {
                        int random = Random.nextInt(Comms.getNumIslands(rc));
                        current_objective = Comms.getIsland(rc, random);
                        island_objective_id = Comms.getIslandID(rc, random);
                    }
                }
                moveTo(rc, current_objective);
                break;
            default:
            break;
        }
    }
}
