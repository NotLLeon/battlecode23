package v7;

import battlecode.common.*;

import java.util.HashMap;
import java.util.HashSet;

public class Carrier extends Robot {

    public enum CARRIER_STATE {
        COLLECTING, EXPLORING, RETURNING, MOVE_TO_WELL, ANCHORING, ISLAND_SEARCH
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
    private static int island_objective_id = 0;
    private static int attempts = 0;
    private static int patience = 0;
    private static int explore_patience_limit = 35;
    private static final int moveToWell_patience_limit = 20;
    static final int weightFactor = (int) 1e6;
    static int stratificationFactor = 2;

    //Number of turns where adamantium is ignored.
    static int magic_rush_number = 60;
    static int old_num_mana_wells = 0;
    static int old_num_mana_wells_turn = 0;

    static int prev_num_islands=0;
    static HashSet<Integer> captured_islands = new HashSet<Integer>();

    private static MapLocation last_well=null;

    private static int random_island_distance_blacklist(RobotController rc, HashSet<Integer> ignore_ids) throws GameActionException {
        int num_islands = Comms.getNumIslands(rc);
        if (num_islands <= ignore_ids.size()) {
            return -1;
        } else {
            int[] indices = new int[num_islands- ignore_ids.size()];
            int[] weights = new int[indices.length];
            int offset = 0;
            for (int i = 0; i < indices.length; i++) {
                if (ignore_ids.contains(Comms.getIslandID(rc, i+offset))) {
                    offset++;
                }
                indices[i] = i + offset;
                int dist = Comms.getIsland(rc, indices[i]).distanceSquaredTo(rc.getLocation());
                weights[i] = weightFactor / ((dist == 0) ? weightFactor : (int) Math.pow(Math.sqrt(dist), stratificationFactor));
            }
            int random = Random.nextIndexWeighted(weights);
            if (rc.getID() == 11994) {
                System.out.println("Giving Island: " + Comms.getIsland(rc, indices[random]) + " bad? " + ignore_ids.contains(indices[random]));
            }
            return indices[random];
        }
    }
    private static int random_island_distance(RobotController rc) throws GameActionException {
        int[] indices = new int[Comms.getNumIslands(rc)];
        int[] weights = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
            int dist = Comms.getIsland(rc, indices[i]).distanceSquaredTo(rc.getLocation());
            weights[i] = weightFactor / ((dist == 0) ? weightFactor : (int) Math.pow(Math.sqrt(dist), stratificationFactor));
        }
        int random = Random.nextIndexWeighted(weights);
        return indices[random];
    }
    private static MapLocation random_well_distance(RobotController rc, int num_wells, ResourceType type) throws GameActionException{
        int[] weights = new int[num_wells];
        MapLocation[] locs = new MapLocation[num_wells];
        for (int i = 0; i < num_wells; i++) {
            locs[i] = (type == ResourceType.ADAMANTIUM) ? Comms.getAdWell(rc, i) : Comms.getManaWell(rc, i);
            weights[i] = weightFactor/(int)Math.pow(Math.sqrt(rc.getLocation().distanceSquaredTo(locs[i])), 2);
        }
        return locs[Random.nextIndexWeighted(weights)];
    }

    //Only use this method if you want to take the blacklist risk.
    private static MapLocation random_well_distance_ignore(RobotController rc, int num_wells, ResourceType type, MapLocation ignore_loc) throws GameActionException {
        if (num_wells == 1 || ignore_loc == null) {
            return random_well_distance(rc, num_wells, type);
        } else {
            int[] indices = new int[num_wells - 1];
            int[] weights = new int[num_wells - 1];
            int offset = 0;
            for (int i = 0; i < num_wells - 1; i++) {
                MapLocation well = (type == ResourceType.ADAMANTIUM) ? Comms.getAdWell(rc, i + offset) : Comms.getManaWell(rc, i + offset);
                if (well.equals(ignore_loc)) {
                    offset = 1;
                    well = (type == ResourceType.ADAMANTIUM) ? Comms.getAdWell(rc, i + offset) : Comms.getManaWell(rc, i + offset);
                }
                indices[i] = i + offset;
                int dist = well.distanceSquaredTo(rc.getLocation());
                weights[i] = (dist == 0) ? 1 : weightFactor / (int) Math.pow(Math.sqrt(dist), stratificationFactor);
            }
            int random = Random.nextIndexWeighted(weights);
            return (type == ResourceType.ADAMANTIUM) ? Comms.getAdWell(rc, indices[random]) : Comms.getManaWell(rc, indices[random]);
        }
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
        combined_weights[0] = weightFactor/(int)(6*Math.pow(5,stratificationFactor)*RobotType.HEADQUARTERS.visionRadiusSquared);
        MapLocation[] combined_locs = new MapLocation[num_ad_wells+num_mana_wells];

        if (rc.getRoundNum() > magic_rush_number) {
            stratificationFactor = 1;
        }

        int min_dist = 10000;

        for (int i = 1; i < num_mana_wells+num_ad_wells+1; i++) {
            if(i-1 < num_ad_wells) combined_locs[i-1] = Comms.getAdWell(rc, i-1);
            else combined_locs[i-1] = Comms.getManaWell(rc, i-1-num_ad_wells);
            int dist = rc.getLocation().distanceSquaredTo(combined_locs[i-1]);
            if (dist == 0 && (rc.getRoundNum() > magic_rush_number || i-1 >= num_ad_wells)) {
                current_objective = combined_locs[i-1];
                state = CARRIER_STATE.COLLECTING;
                runCarrierCollecting(rc);
                return;
            } else if (dist == 0 && rc.getRoundNum() <= magic_rush_number) {
                break;
            }

            if (dist < min_dist && i-1 >= num_ad_wells) {
                min_dist = dist;
            }
            //The ternary operation here is for safety. In the cases where
            //dist is 0 and the code actually gets here, whatever the weights are
            //here are useless.
            combined_weights[i] = (dist == 0) ? 1 : weightFactor/(int)(Math.pow(dist,stratificationFactor));
        }
        int index = Random.nextIndexWeighted(combined_weights);

        double total_ad = Comms.getTotalAd(rc);
        double total_resources = Comms.getTotalAd(rc) + Comms.getTotalMana(rc);

        double ratio = (total_resources > 0) ? total_ad/total_resources : Constants.ideal_ratio;

        if (Math.sqrt(min_dist) > Math.max(rc.getMapWidth(), rc.getMapHeight())/2 && rc.getRoundNum() <= magic_rush_number) {
            state = CARRIER_STATE.EXPLORING;
        } else {
            if (index != 0 && (rc.getRoundNum() > magic_rush_number || num_mana_wells > 0)) {
                if (num_mana_wells > 0 && num_ad_wells > 0) {
//                if (Random.nextBoolean()) {
//                    double random = (double) Random.nextInt(weightFactor) / (double) (weightFactor);
//                    if (random <= Constants.ideal_ratio * Constants.ideal_ratio / ratio && rc.getRoundNum() > magic_rush_number) {
                    if (Random.nextInt(10) == 0 && rc.getRoundNum() > magic_rush_number) {
                        //Adamantium
                        current_objective = random_well_distance_ignore(rc, num_ad_wells, ResourceType.ADAMANTIUM, last_well);
                    } else {
                        //Mana
                        current_objective = random_well_distance_ignore(rc, num_mana_wells, ResourceType.MANA, last_well);
                    }
                } else {
                    current_objective = combined_locs[index - 1];
                }
                state = CARRIER_STATE.MOVE_TO_WELL;
            } else {
                state = CARRIER_STATE.EXPLORING;
            }
        }
        last_well = null;
        runCarrierState(rc);

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

        //Check for nearby launchers
        RobotInfo[] robotInfo = rc.senseNearbyRobots();

        for (RobotInfo robot : robotInfo) {
            // FIXME: jank
            if (robot.getTeam() != rc.getTeam() && robot.getType() == RobotType.LAUNCHER) {
                MapLocation enemyLoc = robot.getLocation();
                MapLocation loc1 = rc.getLocation();
                Direction dir = rc.getLocation().directionTo(robot.getLocation()).opposite();
                Direction dir_left = dir.rotateLeft();
                Direction dir_right = dir.rotateRight();
                while (rc.canMove(dir)) rc.move(dir);
                while (rc.canMove(dir_left)) rc.move(dir_left);
                while (rc.canMove(dir_right)) rc.move(dir_right);
                if(loc1.equals(rc.getLocation())) {
                    Direction dir_to = dir.opposite();
                    Direction dir_to_left = dir_to.rotateLeft();
                    Direction dir_to_right = dir_to.rotateRight();
                    while (rc.canMove(dir_to)) rc.move(dir_to);
                    while (rc.canMove(dir_to_left)) rc.move(dir_to_left);
                    while (rc.canMove(dir_to_right)) rc.move(dir_to_right);
                    if (rc.canAttack(robot.getLocation())) rc.attack(enemyLoc);
                }
                break;
            }
        }

        //Decide Initial Role, will anchor on returning.
        if (turnCount == 1) {
            explore_patience_limit = (rc.getMapHeight() + rc.getMapWidth())/3;
            decide_role(rc);
            return;
        }

       /* for (int i = 0; i < Comms.getNumAdWells(rc); i++) {
            MapLocation loc = Comms.getAdWell(rc, i);
            rc.setIndicatorDot(loc, 255, 0, 0);
        }

        for (int i = 0; i < Comms.getNumManaWells(rc); i++) {
            MapLocation loc = Comms.getAdWell(rc, i);
            rc.setIndicatorDot(loc, 0, 0, 255);
        }*/
        runCarrierState(rc);


    }

    private static final int BYTECODE_THRESHOLD = 3000;
    private static void runCarrierState(RobotController rc) throws GameActionException {
        if(Clock.getBytecodesLeft() < BYTECODE_THRESHOLD) return;
        switch (state) {
            case EXPLORING:     runCarrierExploring(rc); break;
            case MOVE_TO_WELL:  runCarrierMoveToWell(rc); break;
            case RETURNING:     runCarrierReturning(rc); break;
            case COLLECTING:    runCarrierCollecting(rc); break;
            case ISLAND_SEARCH: runCarrierIslandSearch(rc); break;
            case ANCHORING:     runCarrierAnchoring(rc); break;
        }

        if (Comms.getNumManaWells(rc) > old_num_mana_wells) {
            old_num_mana_wells = Comms.getNumManaWells(rc);
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

        //Bandwagoning Collectors code

        if (rc.getRoundNum() <= magic_rush_number) {
            int num_mana_wells = Comms.getNumManaWells(rc);
            if (num_mana_wells > old_num_mana_wells) {
                old_num_mana_wells = num_mana_wells;
                decide_role(rc);
            } else {
                for (int i = 0; i < wells.length; i++) {
                    MapLocation well_loc = wells[i].getMapLocation();
                    if (rc.getRoundNum() <= magic_rush_number) {
                        if (!Comms.knowsWell(rc, well_loc) && wells[i].getResourceType() == ResourceType.MANA) {
                            new_well_loc = well_loc;
                        }
                    } else if (!Comms.knowsWell(rc, well_loc)) {
                        new_well_loc = well_loc;
                        break;
                    }
                }

                int random = (rc.getRoundNum() <= magic_rush_number) ? 0 : Random.nextInt(3);

                if (new_well_loc != null && random <= 1) {
                    //current_objective = new_well_loc;
                    //state = CARRIER_STATE.MOVE_TO_WELL;
                    current_objective = getClosestHQ(rc);
                    state = CARRIER_STATE.RETURNING;
                    patience = 0;
                    runCarrierState(rc);
                } else {
                    exploreNewArea(rc);
                }
            }
        } else {
            for (int i = 0; i < wells.length; i++) {
                MapLocation well_loc = wells[i].getMapLocation();
                if (rc.getRoundNum() <= magic_rush_number) {
                    if (!Comms.knowsWell(rc, well_loc) && wells[i].getResourceType() == ResourceType.MANA) {
                        new_well_loc = well_loc;
                    }
                } else if (!Comms.knowsWell(rc, well_loc)) {
                    new_well_loc = well_loc;
                    break;
                }
            }
            int random = (rc.getRoundNum() <= magic_rush_number) ? 0 : Random.nextInt(3);
            if (new_well_loc != null && random <= 1) {
                current_objective = new_well_loc;
                state = CARRIER_STATE.MOVE_TO_WELL;
                patience = 0;
                runCarrierState(rc);
            } else {
                exploreNewArea(rc);
            }
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

        if (patience >= explore_patience_limit) {
            current_objective = getClosestHQ(rc);
            patience = 0;
            state = CARRIER_STATE.RETURNING;
            runCarrierState(rc);
        }
    }

    private static void runCarrierMoveToWell(RobotController rc) throws GameActionException {
        rc.setIndicatorString("MOVE_TO_WELL, Current Objective: (" + current_objective.x + ", " + current_objective.y + "), LIMIT: " + (moveToWell_patience_limit - patience));
        if (!rc.getLocation().isAdjacentTo(current_objective)) moveTo(rc, current_objective);
        if (!rc.getLocation().isAdjacentTo(current_objective)) moveTo(rc, current_objective);
        senseAndStoreWellLocs(rc);

        if (Clock.getBytecodesLeft() >= BYTECODE_THRESHOLD) {
            int island_ids[] = rc.senseNearbyIslands();
            for (int i = 0; i < island_ids.length; i++) {
                if (!Comms.knowsIsland(rc, island_ids[i])) {
                    MapLocation[] locs = rc.senseNearbyIslandLocations(island_ids[i]);
                    island_locs.put(island_ids[i], Comms.encodeIslandLoc(rc, locs[0]));
                }
            }
        }
        MapLocation curLoc = rc.getLocation();
        if(curLoc.isAdjacentTo(current_objective)) {
            state = CARRIER_STATE.COLLECTING;
            runCarrierState(rc);
            return;
        }
        int disToWell = curLoc.distanceSquaredTo(current_objective);
        if(disToWell > 4 && disToWell <= 36) ++patience;
        if(patience >= moveToWell_patience_limit) {
            last_well = current_objective;
            patience = 0;
            decide_role(rc);
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
        current_objective = getClosestHQ(rc);
        moveTo(rc, current_objective);
        moveTo(rc, current_objective);
        senseAndStoreWellLocs(rc);
        if (Clock.getBytecodesLeft() >= BYTECODE_THRESHOLD) {
            int island_ids[] = rc.senseNearbyIslands();
            for (int i = 0; i < island_ids.length; i++) {
                if (!Comms.knowsIsland(rc, island_ids[i])) {
                    MapLocation[] locs = rc.senseNearbyIslandLocations(island_ids[i]);
                    island_locs.put(island_ids[i], Comms.encodeIslandLoc(rc, locs[0]));
                }
            }
        }

        if (rc.getLocation().isWithinDistanceSquared(current_objective, 9)) {
            Comms.writeWellLocs(rc, ad_well_locs, ResourceType.ADAMANTIUM);
            Comms.writeWellLocs(rc, mn_well_locs, ResourceType.MANA);
            Comms.writeIslandLocs(rc, island_locs);
        }

        if (rc.getLocation().isAdjacentTo(current_objective)) {

            for(ResourceType resType: resourceTypes) {
                if (rc.canTransferResource(current_objective, resType, 1)) {
                    rc.transferResource(current_objective, resType, rc.getResourceAmount(resType));
                }
            }

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
                    } else */
            if (num_islands > 0 && num_anchors > 0) {
                //Randomly pick an island to go to, weighted by distance.
                int random_index = random_island_distance(rc);
                // isn't actually being used

                boolean canTakeAccelerating = rc.canTakeAnchor(current_objective, Anchor.ACCELERATING);
                boolean canTakeStandard = rc.canTakeAnchor(current_objective, Anchor.STANDARD);
                if (canTakeAccelerating) rc.takeAnchor(current_objective, Anchor.ACCELERATING);
                if (canTakeStandard) rc.takeAnchor(current_objective, Anchor.STANDARD);
                if(canTakeAccelerating || canTakeStandard) {
                    current_objective = Comms.getIsland(rc, random_index);
                    island_objective_id = Comms.getIslandID(rc, random_index);
                    state = CARRIER_STATE.ANCHORING;
                    attempts = 0;
                    runCarrierState(rc);
                    return;
                }
                if (rc.senseRobotAtLocation(current_objective).getTotalAnchors() > 0) {
                    //Clock.yield();
                    return;
                }
            }
            decide_role(rc);
        }
    }

    private static void runCarrierCollecting(RobotController rc) throws GameActionException {
        rc.setIndicatorString("COLLECTING");
        if(!rc.getLocation().isAdjacentTo(current_objective)) {
            state = CARRIER_STATE.MOVE_TO_WELL;
            runCarrierState(rc);
        } else if(rc.getWeight() >= 39) {
            current_objective = getClosestHQ(rc);
            state = CARRIER_STATE.RETURNING;
            runCarrierState(rc);
        } else {
            MapLocation curLoc = rc.getLocation();
            if(!curLoc.equals(current_objective) && rc.canMove(curLoc.directionTo(current_objective))) {
                rc.move(curLoc.directionTo(current_objective));
            }
            while (rc.canCollectResource(current_objective, -1)){
                rc.collectResource(current_objective, -1);
            }
        }
    }

    private static void runCarrierIslandSearch(RobotController rc) throws GameActionException {
        rc.setIndicatorString("ISLAND SEARCH");

        int num_islands = Comms.getNumIslands(rc);

        if (rc.getNumAnchors(Anchor.STANDARD) > 0) {
            senseAndStoreWellLocs(rc);

            int[] island_ids = rc.senseNearbyIslands();

            for (int islandId : island_ids) {
                if (!Comms.knowsIsland(rc, islandId)) {
                    MapLocation[] locs = rc.senseNearbyIslandLocations(islandId);
                    island_locs.put(islandId, Comms.encodeIslandLoc(rc, locs[0]));
                }
                if (rc.senseAnchor(islandId) == null) {
                    MapLocation[] locs = rc.senseNearbyIslandLocations(islandId);
                    island_locs.put(islandId, Comms.encodeIslandLoc(rc, locs[0]));
                    current_objective = locs[0];
                    island_objective_id = islandId;
                    state = CARRIER_STATE.ANCHORING;
                    runCarrierState(rc);
                } else {
                    captured_islands.add(islandId);
                }
            }

            if (num_islands > prev_num_islands) {
                prev_num_islands = num_islands;
                //int random = Random.nextInt(Comms.getNumIslands(rc));
                //rc.setIndicatorString("fh389uieojkvfkdje " + island_id + " " + island_objective_id);
                int random_index = random_island_distance_blacklist(rc, captured_islands);
                current_objective = Comms.getIsland(rc, random_index);
                island_objective_id = Comms.getIslandID(rc, random_index);
//                    rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Already Anchored)");
            }

            int island_id = rc.senseIsland(rc.getLocation());

            if (island_id != -1 && rc.senseAnchor(island_id) == null) {
                if (rc.canPlaceAnchor()) {
                    rc.placeAnchor();
                    current_objective = getClosestHQ(rc);
                    state = CARRIER_STATE.RETURNING;
                    runCarrierState(rc);
                }
            } else {
                exploreNewArea(rc);
                return;
            }
        } else {
            current_objective = getClosestHQ(rc);
            state = CARRIER_STATE.RETURNING;
        }
        runCarrierState(rc);
    }

    private static void runCarrierAnchoring(RobotController rc) throws GameActionException {
        rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") SKY: " + Comms.getNumIslands(rc));

        int[] island_ids = rc.senseNearbyIslands();
        int num_islands = Comms.getNumIslands(rc);

        for (int islandId : island_ids) {
            if (!Comms.knowsIsland(rc, islandId)) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(islandId);
                island_locs.put(islandId, Comms.encodeIslandLoc(rc, locs[0]));
            }
            if (rc.senseAnchor(islandId) != null) {
                captured_islands.add(islandId);
                if (islandId == island_objective_id) {
                    prev_num_islands = num_islands;
                    int random_index = random_island_distance_blacklist(rc, captured_islands);

                    if (random_index == -1) {
                        state = CARRIER_STATE.ISLAND_SEARCH;
                        runCarrierState(rc);
                    }

                    current_objective = Comms.getIsland(rc, random_index);
                    island_objective_id = Comms.getIslandID(rc, random_index);
                }
            } else if (captured_islands.contains(islandId)) {
                captured_islands.remove(islandId);
            }
        }

        int island_id = rc.senseIsland(rc.getLocation());
        if (rc.getID() == 11994) {
            System.out.println("Captured: " + captured_islands.size() + " known: " + num_islands);
        }
      /*  if (rc.getID() == 12278 && captured_islands.size() == num_islands) {
            System.out.println("32huihfdfweiohjweijoh3rojir32");
        }*/
        if (island_id != -1) {
//            rc.setIndicatorString("ANCHORING AT (" + rc.getLocation().x + "," + rc.getLocation().y + ") (Within Range) ");
            if (rc.senseAnchor(island_id) != null) {
                if (num_islands > prev_num_islands || island_id == island_objective_id) {
                    prev_num_islands = num_islands;
                    //int random = Random.nextInt(Comms.getNumIslands(rc));
                    //rc.setIndicatorString("fh389uieojkvfkdje " + island_id + " " + island_objective_id);
                    int random_index = (island_id == island_objective_id) ? random_island_distance_blacklist(rc, captured_islands) : random_island_distance(rc);
                    if (random_index == -1 && island_id == island_objective_id) {
                        state = CARRIER_STATE.ISLAND_SEARCH;
                        runCarrierState(rc);
                    } else {
                        current_objective = Comms.getIsland(rc, random_index);
                        island_objective_id = Comms.getIslandID(rc, random_index);
//                    rc.setIndicatorString("ANCHORING AT (" + current_objective.x + "," + current_objective.y + ") (Already Anchored)");
                        attempts += (island_id == island_objective_id) ? 1 : 0;
                    }
                    //rc.setIndicatorString(");
                }
            } else if (rc.canPlaceAnchor()) {
                rc.placeAnchor();
                current_objective = getClosestHQ(rc);
                state = CARRIER_STATE.RETURNING;
                runCarrierState(rc);
                return;
            }
        }
        if (attempts >= Comms.getNumIslands(rc)) {
            state = CARRIER_STATE.ISLAND_SEARCH;
            runCarrierState(rc);
        }
        moveTo(rc, current_objective);
        moveTo(rc, current_objective);
    }
}