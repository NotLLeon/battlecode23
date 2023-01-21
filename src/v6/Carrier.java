package v6;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;

public class Carrier extends Robot {

    public enum CARRIER_STATE {
        COLLECTING, EXPLORING, RETURNING, MOVE_TO_WELL, ANCHORING, ISLAND_SEARCH, RETREAT_TO_ALLY
    }

    private static final ResourceType [] resourceTypes = {
            ResourceType.ADAMANTIUM,
            ResourceType.MANA,
            ResourceType.ELIXIR
    };

    private static HashSet<Integer> ad_well_locs = new HashSet<Integer>();
    private static HashSet<Integer> mn_well_locs = new HashSet<Integer>();
    private static HashSet<Integer> ex_well_locs = new HashSet<Integer>();
    private  static HashMap<Integer, Integer> island_locs = new HashMap<Integer, Integer>();
    private static CARRIER_STATE state = CARRIER_STATE.EXPLORING;
    private static MapLocation current_objective = new MapLocation(0, 0);
    private static MapLocation current_threat = new MapLocation(0, 0);
    private static int island_objective_id = 0;
    private static int attempts = 0;
    private static int patience = 0;
    private static int fear_count = 0;
    private static int fear_tolerance = 5;
    private static int patience_limit = 35;
    private static CARRIER_STATE previous_state = CARRIER_STATE.EXPLORING;
    static final int weightFactor = (int) 1e6;
    static int stratificationFactor = 2;
    private static MapLocation random_well_distance(RobotController rc, int num_wells, ResourceType type) throws GameActionException{
        int[] weights = new int[num_wells];
        MapLocation[] locs = new MapLocation[num_wells];
        for (int i = 0; i < num_wells; i++) {
            locs[i] = (type == ResourceType.ADAMANTIUM) ? Comms.getAdWell(rc, i) : Comms.getManaWell(rc, i);
            weights[i] = weightFactor/(int)Math.sqrt(rc.getLocation().distanceSquaredTo(locs[i]));
        }
        return locs[Random.nextIndexWeighted(weights)];
    }
    private static MapLocation random_all_well_distance(RobotController rc, int num_ad_wells, int num_mana_wells) throws GameActionException{
        int[] weights = new int[num_ad_wells+num_mana_wells];
        MapLocation[] locs = new MapLocation[num_ad_wells+num_mana_wells];
        for (int i = 0; i < num_ad_wells+num_mana_wells; i++) {
            int dist = rc.getLocation().distanceSquaredTo(locs[i]);
            if (dist == 0) return locs[i];
            locs[i] = (i < num_ad_wells) ? Comms.getAdWell(rc, i) : Comms.getManaWell(rc, i-num_ad_wells);
            weights[i] = weightFactor/(int)Math.sqrt(rc.getLocation().distanceSquaredTo(locs[i]));
        }
        return locs[Random.nextIndexWeighted(weights)];
    }

    private static void decide_role(RobotController rc) throws GameActionException {
        int num_mana_wells = Comms.getNumManaWells(rc);
        int num_ad_wells = Comms.getNumAdWells(rc);

        int roundNum = rc.getRoundNum();
        int bound = num_mana_wells + num_ad_wells;
        //TODO: The following is inefficient and very scuffed.

        int[] combined_weights = new int[num_mana_wells+num_ad_wells+1];
        combined_weights[0] = weightFactor/(int)(4*Math.pow(5,stratificationFactor)*RobotType.HEADQUARTERS.visionRadiusSquared);
        MapLocation[] combined_locs = new MapLocation[num_ad_wells+num_mana_wells];

        if (Comms.getNumWells(rc) > 4) {
            stratificationFactor = 1;
        }

        for (int i = 1; i < num_mana_wells+num_ad_wells+1; i++) {
            if(i-1 < num_ad_wells) combined_locs[i-1] = Comms.getAdWell(rc, i-1);
            else combined_locs[i-1] = Comms.getManaWell(rc, i-1-num_ad_wells);
            int dist = rc.getLocation().distanceSquaredTo(combined_locs[i-1]);
            if (dist == 0) {
                current_objective = combined_locs[i-1];
                state = CARRIER_STATE.COLLECTING;
                return;
            }
            combined_weights[i] = weightFactor/(int)(Math.pow(dist,stratificationFactor));
        }
        int index = Random.nextIndexWeighted(combined_weights);

        if (index != 0) {
            if (num_mana_wells > 0 && num_ad_wells > 0) {
                if (Random.nextBoolean()) {
                    //Mana
                    current_objective = random_well_distance(rc, num_mana_wells, ResourceType.MANA);
                } else {
                    //Adamantium
                    current_objective = random_well_distance(rc, num_ad_wells, ResourceType.ADAMANTIUM);
                }
            } else {
                current_objective = combined_locs[index-1];
            }
            state = CARRIER_STATE.MOVE_TO_WELL;
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
        if (turnCount == 1) {
            decide_role(rc);
        }

        //Check for nearby launchers
        RobotInfo[] robotInfo = rc.senseNearbyRobots();

        for (RobotInfo robot : robotInfo) {
            if (robot.getTeam() != rc.getTeam() && robot.getType() == RobotType.LAUNCHER) {
                if (state != CARRIER_STATE.RETREAT_TO_ALLY) {
                    previous_state = state;
                }
                state = CARRIER_STATE.RETREAT_TO_ALLY;
                break;
            }
        }

       /* for (int i = 0; i < Comms.getNumAdWells(rc); i++) {
            MapLocation loc = Comms.getAdWell(rc, i);
            rc.setIndicatorDot(loc, 255, 0, 0);
        }

        for (int i = 0; i < Comms.getNumManaWells(rc); i++) {
            MapLocation loc = Comms.getAdWell(rc, i);
            rc.setIndicatorDot(loc, 0, 0, 255);
        }*/

        switch (state) {
            case EXPLORING:     runCarrierExploring(rc); break;
            case MOVE_TO_WELL:  runCarrierMoveToWell(rc); break;
            case RETURNING:     runCarrierReturning(rc); break;
            case COLLECTING:    runCarrierCollecting(rc); break;
            case ISLAND_SEARCH: runCarrierIslandSearch(rc); break;
            case ANCHORING:     runCarrierAnchoring(rc); break;
            case RETREAT_TO_ALLY: runCarrierRetreatToAlly(rc); break;
        }
    }

    private static void runCarrierRetreatToAlly(RobotController rc) throws GameActionException {
        rc.setIndicatorString("RETREAT TO ALLY");
        RobotInfo[] robos = rc.senseNearbyRobots();
        int min_distance_ally = 10000;
        int min_distance_threat = 10000;
        RobotInfo closest_robo = null;
        RobotInfo closest_threat = null;
        boolean nearby_ally = false;
        for (int i = 0; i < robos.length; i++) {
            if (robos[i].getTeam() == rc.getTeam() && robos[i].getType() == RobotType.LAUNCHER) {
                int candidate = rc.getLocation().distanceSquaredTo(robos[i].getLocation());
                if (candidate < min_distance_ally) {
                    min_distance_ally = candidate;
                    closest_robo = robos[i];
                    nearby_ally = true;
                }
            } else if (robos[i].getTeam() != rc.getTeam() && robos[i].getType() == RobotType.LAUNCHER) {
                int candidate = rc.getLocation().distanceSquaredTo(robos[i].getLocation());
                if (candidate < min_distance_threat) {
                    min_distance_ally = candidate;
                    closest_threat = robos[i];
                }
            }
        }

        if (closest_threat != null) {
            current_threat = closest_threat.getLocation();
            rc.setIndicatorString("RETREATING from threat at (" + current_threat.x + "," + current_threat.y + ")");
        } else {
            rc.setIndicatorString("RETREATING (waiting) " + fear_count);
            fear_count++;
            if (fear_count >= fear_tolerance
                    || rc.getLocation().distanceSquaredTo(current_threat)
                    > RobotType.LAUNCHER.actionRadiusSquared + 5) {
                fear_count = 0;
                rc.setIndicatorString("RETREATING (no longer afraid) " + fear_count);
                state = previous_state;
                return;
            }
        }

        if (nearby_ally) {
            rc.setIndicatorString("RETREATING from threat at (" + current_threat.x + "," + current_threat.y + ") to (" +(closest_robo.getLocation().x) + "," + closest_robo.getLocation().y + ")");
            moveTo(rc, closest_robo.getLocation());
        } else {
            moveToOutsideRadius(rc, current_threat, RobotType.LAUNCHER.actionRadiusSquared + 5);
        }
    }

    private static void runCarrierExploring(RobotController rc) throws GameActionException {
        // TODO: rewrite
        rc.setIndicatorString("EXPLORING");

        int[] island_ids = rc.senseNearbyIslands();

        for (int i = 0; i < island_ids.length; i++) {
            if (!Comms.knowsIsland(rc, island_ids[i])) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(island_ids[i]);
                island_locs.put(island_ids[i], Comms.encodeIslandLoc(rc,locs[0]));
            }
        }

        WellInfo[] wells = senseAndStoreWellLocs(rc);
        MapLocation new_well_loc = null;

        for (int i = 0; i < wells.length; i++) {
            MapLocation well_loc = wells[i].getMapLocation();
            if (!Comms.knowsWell(rc, well_loc)) {
                new_well_loc = well_loc;
                break;
            }
        }

        if (new_well_loc != null && Random.nextInt(3) <= 1) {
            current_objective = new_well_loc;
            state = CARRIER_STATE.MOVE_TO_WELL;
        } else {
            exploreNewArea(rc);
        }

        /*int chance = Random.nextInt(3);
        if (wells.length > 0 && chance + ((wells[0].getResourceType() == ResourceType.MANA) ? 1 : 0) > 1) {
            WellInfo well_one = wells[0];
            current_objective = well_one.getMapLocation();
            state = CARRIER_STATE.MOVE_TO_WELL;
        } else {
            exploreNewArea(rc);
        }*/

        patience++;

        if (patience >= patience_limit) {
            current_objective = getClosestHQ(rc);
            state = CARRIER_STATE.RETURNING;
            patience = 0;
        }
    }

    private static void runCarrierMoveToWell(RobotController rc) throws GameActionException {
        rc.setIndicatorString("MOVE_TO_WELL, Current Objective: (" + current_objective.x + ", " + current_objective.y + "), WELLS: " + Comms.getNumWells(rc));
        MapLocation curLoc = rc.getLocation();
        if (curLoc.isAdjacentTo(current_objective)) {
            state = CARRIER_STATE.COLLECTING;
            return;
        } else {
            moveTo(rc, current_objective);
            moveTo(rc, current_objective);
        }

       /* patience += (rc.getLocation().distanceSquaredTo(current_objective) > 4) ? 1 : 0;

        if (patience >= patience_limit) {
            patience = 0;
            decide_role(rc);
        }*/


        // If well is too busy, decide role again
        RobotInfo[] info = rc.senseNearbyRobots();
        int num_carriers = 0;
        for (RobotInfo robotInfo : info) {
            if (robotInfo.getTeam() == rc.getTeam()
                    && robotInfo.getType() == RobotType.CARRIER
                    && robotInfo.getLocation().isWithinDistanceSquared(current_objective, 4)) {
                num_carriers++;
            }
        }
        if (num_carriers > 9) decide_role(rc);
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
            Comms.writeIslandLocs(rc, island_locs);

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

        boolean ret = true;
        while (rc.canCollectResource(current_objective, -1)
        && (rc.getResourceAmount(ResourceType.ADAMANTIUM)
                +rc.getResourceAmount(ResourceType.MANA)
                +rc.getResourceAmount(ResourceType.ELIXIR)) < 39) {
            ret = false;
            // if (rng.nextBoolean()) {
            rc.collectResource(current_objective, -1);
//            rc.setIndicatorString("Collecting, now have, AD:" +
//                    rc.getResourceAmount(ResourceType.ADAMANTIUM) +
//                    " MN: " + rc.getResourceAmount(ResourceType.MANA) +
//                    " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
            // }
        }
        if(ret) {
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
                    island_locs.put(island_ids[i], Comms.encodeIslandLoc(rc,locs[0]));
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
                exploreNewArea(rc);
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
//            rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Within Range)");
            if (rc.senseAnchor(island_id) != null) {
                int num_islands = Comms.getNumIslands(rc);
                if (num_islands != 0) {
                    int random = Random.nextInt(Comms.getNumIslands(rc));
                    current_objective = Comms.getIsland(rc, random);
                    island_objective_id = Comms.getIslandID(rc, random);
//                    rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Already Anchored)");
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