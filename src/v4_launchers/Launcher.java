package v4_launchers;

import battlecode.common.*;

public class Launcher extends Robot {

    private static MapLocation originHq = null;
    private static MapLocation[] targets;
    private static int targetInd = 0;
    private static boolean onTarget = false;
    private static int roundsNearTarget = 0;

    static MapLocation meet = null;

    static enum LAUNCHER_STATE {
        GOTO_LOCATION, PATROL
    };

    static LAUNCHER_STATE state = LAUNCHER_STATE.GOTO_LOCATION;

    static void runLauncher(RobotController rc, int turnCount) throws GameActionException {
        if(turnCount == 1) {
            generateTargets(rc);

            MapLocation spawn_hq = targets[3];
            int x = spawn_hq.x;
            int y = spawn_hq.y;
            double centerx = rc.getMapWidth() / 2;
            double centery = rc.getMapHeight() / 2;
            if (Math.abs(centerx - x) < rc.getMapWidth() / 6) {
                meet = new MapLocation(x, y + 5 * (((centery-y) < 0) ? -1 : 1));
            }
            else if (Math.abs(centery - y) < rc.getMapWidth() / 6) {
                meet = new MapLocation(x + 5 * (((centerx-x) < 0) ? -1 : 1), y);
            } else {
                meet = new MapLocation(x + 4 * (((centerx-x) < 0) ? -1 : 1), y + 4 * (((centery-y) < 0) ? -1 : 1));
            }

        }
        MapLocation curLoc = rc.getLocation();
        MapLocation shot = tryToShoot(rc);
        if(shot != null) {
            moveTo(rc, new MapLocation(curLoc.x + (curLoc.x - shot.x), curLoc.y + (curLoc.y - shot.y)));
            onTarget = false;
        }
        switch (state) {
            case GOTO_LOCATION:
            rc.setIndicatorString("GOTO_LOCATION");
            moveToRadius(rc, meet, 2);
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(10, rc.getTeam());
            int numLaunchers = 0;
            for (RobotInfo rob : nearbyRobots) {
                if (rob.getType() == RobotType.LAUNCHER) { numLaunchers++; }
            }
            if (numLaunchers > 3) { state = LAUNCHER_STATE.PATROL; }
            break;
            case PATROL:
            default:
            rc.setIndicatorString("PATROL: " + targets[targetInd]);
            if(!onTarget){
                MapLocation curTarget = targets[targetInd];
                moveToRadius(rc, curTarget, 4);
                if(curLoc.isWithinDistanceSquared(curTarget, 4)) {
                    if(!canSeeHq(rc)) nextTarget();
                    else onTarget = true;
                } else if(curLoc.isWithinDistanceSquared(curTarget, 16)) {
                    roundsNearTarget++;
                }
                if(!isReachable(rc, curTarget) || roundsNearTarget > 20) {
                    nextTarget();
                }
            }
            break;
        }
        tryToShoot(rc);
    }

    private static boolean canSeeHq(RobotController rc) throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        for (RobotInfo enemy : enemies) {
            if (enemy.getType() == RobotType.HEADQUARTERS) return true;
        }
        return false;
    }

    private static MapLocation tryToShoot(RobotController rc) throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 100;
        int smallestDistance = 100;
        RobotInfo target = null;

        for (RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.HEADQUARTERS) continue;
            int enemyHealth = enemy.getHealth();
            if (enemy.type == RobotType.LAUNCHER) {
                enemyHealth -= 200;
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
        MapLocation [] Hqs = Comms.getHQs(rc);
        MapLocation curLoc = rc.getLocation();

        int mapW = rc.getMapWidth();
        int mapH = rc.getMapHeight();
        int closestDis = 999999;

        for(MapLocation hq : Hqs) {
            int dis = curLoc.distanceSquaredTo(hq);
            if(dis < closestDis) {
                closestDis = dis;
                originHq = hq;
            }
        }
        targets = new MapLocation[4];
        targets[0] = new MapLocation(
                mapW-originHq.x-1,
                mapH-originHq.y-1
        );
        targets[1] = new MapLocation(
                mapW-originHq.x-1,
                originHq.y
        );
        targets[2] = new MapLocation(
                originHq.x,
                mapH-originHq.y-1
        );
        targets[3] = originHq;
    }


}
