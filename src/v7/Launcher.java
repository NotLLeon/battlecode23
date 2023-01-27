package v7;

import battlecode.common.*;

import java.util.ArrayList;

public class Launcher extends Robot {

    private static MapLocation originHq = null;
    private static MapLocation[] targets;
    private static int targetInd = 0;
    private static int roundsNearTarget = 0;
    private static MapLocation defendPoint = null;

    static MapLocation meet = null;

    enum LAUNCHER_STATE {
        GOTO_LOCATION, PATROL, DEFEND, ON_TARGET, RETURNING
    };

    enum SYMMETRY_CHECK {
        HORIZONTAL(1), VERTICAL(2), ROTATIONAL(4), BASE(0);

        private int corres;
        private int getVal() {
            return corres;
        }
        private SYMMETRY_CHECK(int corres) {
            this.corres = corres;
        }
    }

    static LAUNCHER_STATE state = LAUNCHER_STATE.PATROL;
    // used to rule out symmetries while exploring
    static SYMMETRY_CHECK[] locsyms;
    static boolean[] locIgnore;
    static int lastSymState = -1;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        // rc.setIndicatorString("" + state);
        if(turnCount == 1) {
            originHq = getClosestHQ(rc);
        }
        pruneSymmetries(rc);
        generateTargets(rc);
        // setMeetLocation(rc, turnCount);
        MapLocation curLoc = rc.getLocation();
        MapLocation shot = tryToShoot(rc);
        if(shot != null && state != LAUNCHER_STATE.ON_TARGET) {
            Direction moveBack = curLoc.directionTo(shot).opposite();
            if(rc.canMove(moveBack)) rc.move(moveBack);
            else if(rc.canMove(moveBack.rotateLeft())) rc.move(moveBack.rotateLeft());
            else if(rc.canMove(moveBack.rotateRight())) rc.move(moveBack.rotateRight());
            state = LAUNCHER_STATE.PATROL;
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(4, rc.getTeam());

        int numLaunchers = 0;

        for (RobotInfo rob : nearbyRobots) {
            if (rob.getType() == RobotType.LAUNCHER) numLaunchers++;
        }

        if(state != LAUNCHER_STATE.ON_TARGET) {
            MapLocation[] distress = Comms.getDistressLocations(rc);
            if (distress.length > 0 && state != LAUNCHER_STATE.DEFEND) {
                if(rc.getRoundNum() > 200) {
                    state = LAUNCHER_STATE.DEFEND;
                    defendPoint = distress[Random.nextInt(distress.length)];
                } else {
                    for(MapLocation disLoc : distress) {
                        if(disLoc.equals(originHq)) {
                            state = LAUNCHER_STATE.DEFEND;
                            defendPoint = disLoc;
                            break;
                        }
                    }
                }
            }
            else if (distress.length == 0 && state == LAUNCHER_STATE.DEFEND) {
                // state = LAUNCHER_STATE.GOTO_LOCATION;
                state = LAUNCHER_STATE.PATROL;
            }
            // else if (state == LAUNCHER_STATE.PATROL && numLaunchers < 2) {
            //     state = LAUNCHER_STATE.GOTO_LOCATION;
            // }
        }

        runLauncherState(rc);
        tryToShoot(rc);
        takePotshot(rc);
    }
    private static void runLauncherState(RobotController rc) throws GameActionException {
        switch (state) {
            case GOTO_LOCATION: runLauncherGoto(rc); break;
            case DEFEND:        runLauncherDefend(rc); break;
            case PATROL:        runLauncherPatrol(rc); break;
            case ON_TARGET:     runLauncherOnTarget(rc); break;
            case RETURNING:     runLauncherReturning(rc); break;
        }
    }

    private static void runLauncherGoto(RobotController rc) throws GameActionException {
//        rc.setIndicatorString("GOTO_LOCATION: " + meet);
        moveToOutsideRadius(rc, meet, 0);
        MapLocation curLoc = rc.getLocation();

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(4, rc.getTeam());

        int numLaunchers = 0;
        int adjLaunchers = 0;

        for (RobotInfo rob : nearbyRobots) {
            if (rob.getType() == RobotType.LAUNCHER) {
                if(rob.getLocation().isAdjacentTo(curLoc)) adjLaunchers++;
                numLaunchers++;
            }
        }
//                rc.setIndicatorString("NUMLAUNCHERS: " + numLaunchers + " ADJLAUNCH: " + adjLaunchers);
        if (numLaunchers >= 3 && adjLaunchers > 2) {
            state = LAUNCHER_STATE.PATROL;
            runLauncherState(rc);
        }
    }

    private static void runLauncherDefend(RobotController rc) throws GameActionException {
        moveToOutsideRadius(rc, defendPoint, 9);
        if (rc.isMovementReady()) {
            Direction rdmMove = Random.nextDir();
            if (rc.canMove(rdmMove)) {
                rc.move(rdmMove);
            }
        }
    }

    private static void runLauncherPatrol(RobotController rc) throws GameActionException {
//        rc.setIndicatorString(""+onTarget);
        MapLocation curTarget = targets[targetInd];
        moveToRadius(rc, curTarget, 4);
        MapLocation curLoc = rc.getLocation();

        if(rc.canSenseLocation(curTarget)) {
            if(canSeeHq(rc)) {
                state = LAUNCHER_STATE.ON_TARGET;
                runLauncherState(rc);
            } else {
                locIgnore[targetInd] = true;
                nextTarget();
                if (curLoc.distanceSquaredTo(originHq) * 3 < curLoc.distanceSquaredTo(targets[targetInd])) {
                    state = LAUNCHER_STATE.RETURNING;
                }
            }
        } else if(curLoc.isWithinDistanceSquared(curTarget, 16)) {
            roundsNearTarget++;
        }
        if(!isReachable(rc, curTarget) || roundsNearTarget > 20) {
            nextTarget();
        }
        rc.setIndicatorDot(targets[targetInd], 0, 255, 0);
    }
    private static void runLauncherOnTarget(RobotController rc) throws GameActionException {
        MapLocation curTarget = targets[targetInd];
        moveToOutsideRadius(rc, curTarget, 16);
        
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(4, rc.getTeam());
        int numLaunchers = 0;

        for (RobotInfo rob : nearbyRobots) {
            if (rob.getType() == RobotType.LAUNCHER && rc.getID() > rob.getID()) numLaunchers++;
        }
        if (numLaunchers > 4) {
            nextTarget();
            state = LAUNCHER_STATE.PATROL;
        }
    }

    private static void runLauncherReturning(RobotController rc) throws GameActionException {
        moveToRadius(rc, originHq, 9);
    }

    private static void pruneSymmetries(RobotController rc) throws GameActionException {
        if (rc.canWriteSharedArray(Constants.IDX_POSSIBLE_SYMS, lastSymState) && locsyms.length > 0 && lastSymState > 0) {
            for (int i = 0; i < locsyms.length; i++) {
                if (locIgnore[i]) {
                    int dec = locsyms[i].getVal();
                    if (dec != 0 && (lastSymState / dec) % 2 == 1) {
                        lastSymState -= locsyms[i].getVal();
                    }
                }
            }
            Comms.writePossibleSyms(rc, lastSymState);
            state = LAUNCHER_STATE.PATROL;
        }
    }

    private static ArrayList<MapLocation> shotLocs = new ArrayList<MapLocation>();
    private static void takePotshot(RobotController rc) throws GameActionException {
        if(!rc.isActionReady()) return;
        shotLocs.clear();
        MapLocation curLoc = rc.getLocation();
        // shoot randomly at a position that is out of vision
        if(!rc.senseMapInfo(curLoc).hasCloud()) {
            MapLocation[] cloudLocs = rc.senseNearbyCloudLocations();
            for (MapLocation cloudLoc : cloudLocs) {
//                System.out.println(loc.isWithinDistanceSquared(curLoc, 4) + " " + info.hasCloud());
                if (!cloudLoc.isWithinDistanceSquared(curLoc, 4) && rc.canActLocation(cloudLoc)) {
                    shotLocs.add(cloudLoc);
                }
            }
        } else {
            // TODO: shoot at some location outside vision radius (4)
        }
//        rc.setIndicatorString(""+shotLocs.size());
        if(!shotLocs.isEmpty()) rc.attack(shotLocs.get(Random.nextInt(shotLocs.size())));
    }

    private static boolean canSeeHq(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots();
        for (RobotInfo enemy : enemies) {
            if (enemy.getType() == RobotType.HEADQUARTERS && enemy.getTeam() != rc.getTeam()) return true;
        }
        return false;
    }

    private static MapLocation tryToShoot(RobotController rc) throws GameActionException {
        if(!rc.isActionReady()) return null;
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 1000;
        int smallestDistance = 100;
        RobotInfo target = null;

        for (RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) continue;
            int enemyHealth = enemy.getHealth();
            if (enemy.getType() == RobotType.LAUNCHER) {
                enemyHealth -= 2000;
            }
            int enemyDistance = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (enemyHealth < lowestHealth) {
                target = enemy;
                lowestHealth = enemyHealth;
                smallestDistance = enemyDistance;
            } else if (enemyHealth == lowestHealth) {
                if (enemyDistance < smallestDistance) {
                    target = enemy;
                    smallestDistance = enemyDistance;
                }
            }
        }
        if (target != null && rc.canAttack(target.getLocation())) {
            rc.attack(target.getLocation());
            return target.getLocation();
        }
        return null;
    }

    private static void nextTarget() {
        targetInd = (targetInd+1)%targets.length;
        roundsNearTarget = 0;
    }

    private static void generateTargets(RobotController rc) throws GameActionException {
        int syms = Comms.getPossibleSyms(rc);
        rc.setIndicatorString("SYMS: " + syms + " SET: " + lastSymState);
        if (syms == lastSymState) { return; }
        
        // % 2 for horz, /2 %2 for vert, /4 for rotational
        targetInd = 0;
        MapLocation [] Hqs = Comms.getHQs(rc);

        lastSymState = syms;
        int mapW = rc.getMapWidth();
        int mapH = rc.getMapHeight();

        if (syms == 1) {
            MapLocation[] temp = new MapLocation[Hqs.length];
            for (int i = 0; i < Hqs.length; i++) {
                temp[i] = new MapLocation(Hqs[i].x, mapH-Hqs[i].y-1);
            }
            targets = temp;
            locsyms = new SYMMETRY_CHECK[]{};
            locIgnore = new boolean[]{};
        } else if (syms == 2) {
            MapLocation[] temp = new MapLocation[Hqs.length];
            for (int i = 0; i < Hqs.length; i++) {
                temp[i] = new MapLocation(mapW-Hqs[i].x-1, Hqs[i].y);
            }
            targets = temp;
            locsyms = new SYMMETRY_CHECK[]{};
            locIgnore = new boolean[]{};
        } else if (syms == 4) {
            MapLocation[] temp = new MapLocation[Hqs.length];
            for (int i = 0; i < Hqs.length; i++) {
                temp[i] = new MapLocation(mapW-Hqs[i].x-1, mapH-Hqs[i].y-1);
            }
            targets = temp;
            locsyms = new SYMMETRY_CHECK[]{};
            locIgnore = new boolean[]{};
        } else if (syms == 7) {
            targets = new MapLocation[4];
            locsyms = new SYMMETRY_CHECK[4];
            locIgnore = new boolean[4];

            targets[0] = new MapLocation(
                    mapW-originHq.x-1,
                    originHq.y
            );
            targets[1] = new MapLocation(
                    originHq.x,
                    mapH-originHq.y-1
            );
            targets[2] = new MapLocation(
                mapW-originHq.x-1,
                mapH-originHq.y-1
            );
            targets[3] = originHq;
    
            int t1 = 60 * 60;
            int t2 = 60 * 60;
            for (MapLocation hq : Hqs) {
                int t1dist = hq.distanceSquaredTo(targets[0]);
                int t2dist = hq.distanceSquaredTo(targets[1]);
                if (t1dist < t1) {
                    t1 = t1dist;
                }
                if (t2dist < t2) {
                    t2 = t2dist;
                }
            }
            if (t2 < t1) {
                MapLocation temp = targets[0];
                targets[0] = targets[1];
                targets[1] = targets[2];
                targets[2] = temp;
                locsyms[0] = SYMMETRY_CHECK.HORIZONTAL;
                locsyms[1] = SYMMETRY_CHECK.ROTATIONAL;
                locsyms[2] = SYMMETRY_CHECK.VERTICAL;
                locsyms[3] = SYMMETRY_CHECK.BASE;
            } else {
                MapLocation temp = targets[1];
                targets[1] = targets[2];
                targets[2] = temp;
                locsyms[0] = SYMMETRY_CHECK.VERTICAL;
                locsyms[1] = SYMMETRY_CHECK.ROTATIONAL;
                locsyms[2] = SYMMETRY_CHECK.HORIZONTAL;
                locsyms[3] = SYMMETRY_CHECK.BASE;
            }
        } else {
            boolean horz = syms % 2 == 1;
            boolean vert = (syms / 2) % 2 == 1;
            boolean rot = (syms / 4) == 1;
            
            targets = new MapLocation[3];
            locsyms = new SYMMETRY_CHECK[3];
            locIgnore = new boolean[3];

            if (horz && vert) {
                // compare the 2 to see what is longer
                targets[0] = new MapLocation(
                        mapW-originHq.x-1,
                        originHq.y
                );
                targets[1] = new MapLocation(
                        originHq.x,
                        mapH-originHq.y-1
                );
                targets[2] = originHq;
        
                int t1 = 60 * 60;
                int t2 = 60 * 60;
                for (MapLocation hq : Hqs) {
                    int t1dist = hq.distanceSquaredTo(targets[0]);
                    int t2dist = hq.distanceSquaredTo(targets[1]);
                    if (t1dist < t1) {
                        t1 = t1dist;
                    }
                    if (t2dist < t2) {
                        t2 = t2dist;
                    }
                }
                if (t2 > t1) {
                    MapLocation temp = targets[0];
                    targets[0] = targets[1];
                    targets[1] = temp;
                    locsyms[0] = SYMMETRY_CHECK.HORIZONTAL;
                    locsyms[1] = SYMMETRY_CHECK.VERTICAL;
                    locsyms[2] = SYMMETRY_CHECK.BASE;
                } else {
                    locsyms[0] = SYMMETRY_CHECK.VERTICAL;
                    locsyms[1] = SYMMETRY_CHECK.HORIZONTAL;
                    locsyms[2] = SYMMETRY_CHECK.BASE;
                }
            } else {
                if (horz) {
                    targets[0] = new MapLocation(originHq.x, mapH-originHq.y-1);
                    locsyms[0] = SYMMETRY_CHECK.HORIZONTAL;
                } else if (vert) {
                    targets[0] = new MapLocation(mapW-originHq.x-1, originHq.y);
                    locsyms[0] = SYMMETRY_CHECK.VERTICAL;
                }

                targets[1] = new MapLocation(mapW-originHq.x-1, mapH-originHq.y-1);
                targets[2] = originHq;
                locsyms[1] = SYMMETRY_CHECK.ROTATIONAL;
                locsyms[2] = SYMMETRY_CHECK.BASE;
            }
        }
    }

    private static void setMeetLocation(RobotController rc, int turnCount) throws GameActionException {
        MapLocation spawn_hq = originHq;
        int x = spawn_hq.x;
        int y = spawn_hq.y;
        double centerx = rc.getMapWidth() / 2.0;
        double centery = rc.getMapHeight() / 2.0;

        MapLocation[] hqs = Comms.getHQs(rc);
        // // TODO: take other hq positions into account
        // if (Math.abs(centerx - x) < rc.getMapWidth() / 6.0) {
        //     meet = new MapLocation(x, y + (4 + (turnCount / 30)) * (((centery-y) < 0) ? -1 : 1));
        // } else if (Math.abs(centery - y) < rc.getMapWidth() / 6.0) {
        //     meet = new MapLocation(x + (4 + (turnCount / 30)) * (((centerx-x) < 0) ? -1 : 1), y);
        // } else {
        //     meet = new MapLocation(x + (3 + turnCount / 30) * (((centerx-x) < 0) ? -1 : 1), y + (3 + (turnCount / 30)) * (((centery-y) < 0) ? -1 : 1));
        // }
        // MapLocation center = new MapLocation((int)centerx, (int)centery);
        // if (spawn_hq.distanceSquaredTo(meet) > spawn_hq.distanceSquaredTo(center)) {
        //     meet = center;
        // }
        if (Math.abs(centerx - x) < rc.getMapWidth() / 6.0) {
            meet = new MapLocation(x, y + 7 * (((centery-y) < 0) ? -1 : 1));
        } else if (Math.abs(centery - y) < rc.getMapWidth() / 6.0) {
            meet = new MapLocation(x + 7 * (((centerx-x) < 0) ? -1 : 1), y);
        } else {
            meet = new MapLocation(x + 5 * (((centerx-x) < 0) ? -1 : 1), y + 5 * (((centery-y) < 0) ? -1 : 1));
        }
        int xtot = 0;
        int ytot = 0;
        for (MapLocation hq : hqs) {
            xtot += hq.x;
            ytot += hq.y;
        }
        meet = new MapLocation((xtot + rc.getMapWidth()) / (hqs.length + 2), (ytot + rc.getMapHeight()) / (hqs.length + 2));
    }

}
