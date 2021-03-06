package simpleplayer1;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount;
    static int hqID;
    static int storedFlag;
    static MapLocation hqLocation;
    static Direction curDir = Direction.NORTH;

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {
            turnCount += 1;

            try {
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER:
                        runEnlightenmentCenter();
                        break;
                    case POLITICIAN:
                        runPolitician();
                        break;
                    case SLANDERER:
                        runSlanderer();
                        break;
                    case MUCKRAKER:
                        runMuckraker();
                        break;
                }

                Clock.yield();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        RobotType toBuild;

        if (rc.getRoundNum() == 1 && rc.getInfluence() > 20) {
            rc.buildRobot(RobotType.SLANDERER, Direction.NORTHWEST,
                    rc.getInfluence() - ((rc.getInfluence() - 1) % 20) - 1);
            if (rc.canBid(3))
                rc.bid(3);
            return;
        }

        boolean any_open_pol_spots = false;

        for (int i = 0; i < Formations.stageone_wall.length; i++) {
            int[] dxy = Formations.stageone_wall[i];
            MapLocation pos = rc.getLocation().translate(dxy[0], dxy[1]);
            if (rc.canSenseLocation(pos) && !rc.isLocationOccupied(pos)) {
                rc.setFlag(10 + i);
                any_open_pol_spots = true;
                break;
            }
        }

        if (any_open_pol_spots) {
            toBuild = RobotType.POLITICIAN;
            if (rc.getInfluence() > 69 && rc.canBuildRobot(toBuild, Direction.WEST, rc.getInfluence() - 1)) {
                rc.buildRobot(toBuild, Direction.WEST, rc.getInfluence() - 1);
            }
        }

        int bidAmount = Bidding.bidAmount(rc);
        if (rc.canBid(bidAmount)) {
            // System.out.println("Bidding " + bidAmount);
            rc.bid(bidAmount);
        }

        if (turnCount % 10 == 0) {
            // System.out.println(rc.getInfluence());
        }
    }

    static void runPolitician() throws GameActionException {
        if (turnCount == 1) {
            RobotInfo[] robots = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    storedFlag = rc.getFlag(robot.getID());
                    hqLocation = robot.getLocation();
                    break;
                }
            }
        }

        if (!rc.isReady())
            return;

        if (storedFlag > 9 && storedFlag < 19) { // defense mode

            int[] dxy = Formations.stageone_wall[storedFlag - 10];
            MapLocation pos = hqLocation.translate(dxy[0], dxy[1]);
            // tryMove(PathFind.get_path_direction(rc, pos));
            tryMove(PathFind.pathTo(rc, pos));

        } else {
            System.out.println("Error invalid flag" + storedFlag);
        }
    }

    static void runSlanderer() throws GameActionException {
    }

    static void runMuckraker() throws GameActionException {
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        return false;
    }
}
