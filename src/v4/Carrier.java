package v4;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;

public class Carrier extends Robot {

    public enum CARRIER_STATE {
        COLLECTING, EXPLORING, RETURNING, MOVE_TO_WELL, ANCHORING, ISLAND_SEARCH
    }

    private static final ResourceType [] resourceTypes = {ResourceType.ADAMANTIUM, ResourceType.MANA, ResourceType.ELIXIR};
    private static HashSet<Integer> ad_well_locs = new HashSet<Integer>();
    private static HashSet<Integer> mn_well_locs = new HashSet<Integer>();
    private static HashSet<Integer> ex_well_locs = new HashSet<Integer>();
    private  static HashMap<Integer, Integer> island_locs = new HashMap<Integer, Integer>();
    private static CARRIER_STATE state = CARRIER_STATE.EXPLORING;
    private static MapLocation current_objective = new MapLocation(0, 0);
    private static int island_objective_id = 0;
    private static int attempts = 0;

    private static void decide_role(RobotController rc) throws GameActionException {
        //int num_wells = Comms.getNumWells(rc);

        int num_mana_wells = Comms.getNumManaWells(rc);
        int num_ad_wells = Comms.getNumAdWells(rc);

        int random_choice = Random.nextInt(num_mana_wells + num_ad_wells+1);

        if (random_choice != 0) {
            if (num_mana_wells > 0 && num_ad_wells > 0) {
                //Set to 4 to be impossible (temp).
                if (Random.nextInt(2) == 1) {
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

    private static WellInfo[] senseAndStoreWellLocs(RobotController rc) {
        WellInfo[] wells = rc.senseNearbyWells();

        for (WellInfo well : wells) {
            switch(well.getResourceType()) {
                case ADAMANTIUM: ad_well_locs.add(Comms.encodeWellLoc(rc, well.getMapLocation())); break;
                case MANA: mn_well_locs.add(Comms.encodeWellLoc(rc, well.getMapLocation())); break;
                case ELIXIR: ex_well_locs.add(Comms.encodeWellLoc(rc, well.getMapLocation())); break;
            }
        }
        return wells;
    }

    static void runCarrier(RobotController rc, int turnCount) throws GameActionException {

        //Decide Initial Role, will anchor on returning.
        if (turnCount == 1) decide_role(rc);

        //Check for nearby launchers
        RobotInfo[] robotInfo = rc.senseNearbyRobots();

        for (int i = 0; i < robotInfo.length; i++) {
            if (robotInfo[i].getTeam() != rc.getTeam()
                    && robotInfo[i].type == RobotType.LAUNCHER
                    && rc.canAttack(robotInfo[i].getLocation())) {
                rc.attack(robotInfo[i].getLocation());
            }
        }

        switch (state) {
            case EXPLORING:     runCarrierExplore(rc); break;
            case MOVE_TO_WELL:  runCarrierMoveToWell(rc); break;
            case RETURNING:     runCarrierReturning(rc); break;
            case COLLECTING:    runCarrierCollecting(rc); break;
            case ISLAND_SEARCH: runCarrierIslandSearch(rc); break;
            case ANCHORING:     runCarrierAnchoring(rc); break;
        }
    }

    private static void runCarrierExplore(RobotController rc) throws GameActionException {
        // FIXME: rewrite
        rc.setIndicatorString("EXPLORING");

                /*int[] island_ids = rc.senseNearbyIslands();

                for (int i = 0; i < island_ids.length; i++) {
                    if (!Comms.knowsIsland(rc, island_ids[i])) {
                        MapLocation[] locs = rc.senseNearbyIslandLocations(island_ids[i]);
                        for (int j = 0; j < locs.length; j++) {
                            island_locs.put(i, j);
                        }
                    }
                }*/

        WellInfo[] wells = senseAndStoreWellLocs(rc);

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
    }

    private static void runCarrierMoveToWell(RobotController rc) throws GameActionException {
        rc.setIndicatorString("MOVE_TO_WELL, Current Objective: (" + current_objective.x + ", " + current_objective.y + "), WELLS: " + Comms.getNumWells(rc));
        if (rc.getLocation().isAdjacentTo(current_objective)) {
            state = CARRIER_STATE.COLLECTING;
        } else {
            moveTo(rc, current_objective);
        }
    }

    private static void runCarrierReturning(RobotController rc) throws GameActionException {
        rc.setIndicatorString("RETURNING, Current Objective: (" + current_objective.x + ", " + current_objective.y + ")");

        if (rc.getLocation().isAdjacentTo(current_objective)) {

            for(ResourceType resType: resourceTypes) {
                if (rc.canTransferResource(current_objective, resType, 1)) {
                    rc.transferResource(current_objective, resType, rc.getResourceAmount(resType));
                }
            }

            Comms.writeWellLocs(rc, ad_well_locs, ResourceType.ADAMANTIUM);
            Comms.writeWellLocs(rc, mn_well_locs, ResourceType.MANA);
            // Comms.writeIslandLocs(rc, island_locs);

            int num_islands = Comms.getNumIslands(rc);
            int num_anchors = 0;

            if (rc.canSenseRobotAtLocation(current_objective)) {
                num_anchors = rc.senseRobotAtLocation(current_objective).getTotalAnchors();
            }

                   /* if (num_islands == 0 && num_anchors > 0) {
                        if (rc.canTakeAnchor(current_objective, Anchor.ACCELERATING)) {
                            rc.takeAnchor(current_objective, Anchor.ACCELERATING);
                            state = CARRIER_STATE.ISLAND_SEARCH;
                        }
                        if (rc.canTakeAnchor(current_objective, Anchor.STANDARD)) {
                            rc.takeAnchor(current_objective, Anchor.STANDARD);
                            state = CARRIER_STATE.ISLAND_SEARCH;
                        }
                        break;
                    } else */if (num_islands > 0 && num_anchors > 0) {
                //Uniformly, randomly, pick an island to go to.
                int random = Random.nextInt(num_islands);
                // isn't actually being used

                boolean canTakeAccelerating = rc.canTakeAnchor(current_objective, Anchor.ACCELERATING);
                boolean canTakeStandard = rc.canTakeAnchor(current_objective, Anchor.STANDARD);
                if (canTakeAccelerating) rc.takeAnchor(current_objective, Anchor.ACCELERATING);
                if (canTakeStandard) rc.takeAnchor(current_objective, Anchor.STANDARD);
                if(canTakeAccelerating || canTakeStandard) {
                    current_objective = Comms.getIsland(rc, random);
                    island_objective_id = Comms.getIslandID(rc, random);
                    state = CARRIER_STATE.ANCHORING;
                    attempts = 0;
                    return;
                }
                if (rc.senseRobotAtLocation(current_objective).getTotalAnchors() > 0) {
                    //Clock.yield();
                    return;
                }
            }
            decide_role(rc);
        } else {
            current_objective = getClosestHQ(rc);
            moveTo(rc, current_objective);
        }
    }

    private static void runCarrierCollecting(RobotController rc) throws GameActionException {
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
    }

    private static void runCarrierIslandSearch(RobotController rc) throws GameActionException {
        rc.setIndicatorString("ISLAND SEARCH");

        if (rc.getNumAnchors(Anchor.STANDARD) > 0) {
            senseAndStoreWellLocs(rc);

            int[] island_ids = rc.senseNearbyIslands();

            for (int i = 0; i < island_ids.length; i++) {
                if (!Comms.knowsIsland(rc, island_ids[i])) {
                    MapLocation[] locs = rc.senseNearbyIslandLocations(island_ids[i]);
                    for (int j = 0; j < locs.length; j++) {
                        island_locs.put(i, j);
                    }
                    current_objective = locs[0];
                    island_objective_id = island_ids[i];
                    state = CARRIER_STATE.ANCHORING;
                }
            }

            int island_id = rc.senseIsland(rc.getLocation());

            if (island_id != -1 && rc.senseAnchor(island_id) == null) {
                if (rc.canPlaceAnchor()) {
                    rc.placeAnchor();
                    current_objective = getClosestHQ(rc);
                    state = CARRIER_STATE.RETURNING;
                }
            } else {
                Direction dir = Explore.exploreAwayFromHQ(rc, getClosestHQ(rc));
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        } else {
            current_objective = getClosestHQ(rc);
            state = CARRIER_STATE.RETURNING;
        }
    }

    private static void runCarrierAnchoring(RobotController rc) throws GameActionException {
        rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") SKY: " + Comms.getNumIslands(rc));
        int island_id = rc.senseIsland(rc.getLocation());
        if (island_id == island_objective_id) {
            rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Within Range)");
            if (rc.senseAnchor(island_id) != null) {
                int num_islands = Comms.getNumIslands(rc);
                if (num_islands != 0) {
                    int random = Random.nextInt(Comms.getNumIslands(rc));
                    current_objective = Comms.getIsland(rc, random);
                    island_objective_id = Comms.getIslandID(rc, random);
                    rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Already Anchored)");
                    attempts++;
                }
            } else if (rc.canPlaceAnchor()) {
                rc.placeAnchor();
                current_objective = getClosestHQ(rc);
                state = CARRIER_STATE.RETURNING;
                return;
            }
        }
        if (attempts >= Comms.getNumIslands(rc)) {
            state = CARRIER_STATE.ISLAND_SEARCH;
        }
        moveTo(rc, current_objective);
    }
}
