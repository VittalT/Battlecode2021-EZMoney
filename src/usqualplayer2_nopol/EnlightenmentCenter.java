package usqualplayer2_nopol;

import battlecode.common.*;
import common.*;

import java.util.*;

class EnlightenmentCenter extends Robot {
    private int turnCount = 0;
    private int unitsBuilt = 0;
    private int unitsIndex = 0;
    private int spawnIndex = 0;
    private RobotType unitToBuild;
    private int infToSpend;
    private Direction dirTarget, buildDirection;
    private int slandDistAway = 10;
    private int influence, maxInf;
    private int unitFunction;
    private int encoding = 0, nextEncoding = 0;
    private Map.Entry<MapLocation, Integer> minNeutral, minAllNeutral;
    private Map.Entry<MapLocation, Integer> minEnemy, minAllEnemy;
    private Set<MapLocation> friendlyHQs = new HashSet<MapLocation>();
    private Map<MapLocation, Integer> neutralHQs = new HashMap<MapLocation, Integer>();
    private Map<MapLocation, Integer> enemyHQs = new HashMap<MapLocation, Integer>();
    private Map<MapLocation, Integer> polSentID = new HashMap<MapLocation, Integer>();
    private ArrayList<Integer> units = new ArrayList<Integer>();
    private final Bidding bidController;
    private boolean bidLastRound;
    Queue<MapLocation> spawnLocs = new LinkedList<MapLocation>();

    EnlightenmentCenter(RobotController rcin) throws GameActionException {
        super(rcin);
        bidController = new Bidding();
    }

    void updateECs(int minBytecodeLeft) throws GameActionException {
        while (Clock.getBytecodesLeft() > minBytecodeLeft && !units.isEmpty()) {
            unitsIndex %= units.size();
            int unitID = units.get(unitsIndex);
            if (rc.canGetFlag(unitID)) {
                parseUnitFlag(rc.getFlag(unitID));
                unitsIndex++;
            } else {
                units.remove(unitsIndex);
            }
        }
    }

    void run() throws GameActionException {
        while (true) {
            turnCount++;
            // TODO: Implement smart handling of other units and other HQs
            // gatherIntel();

            encoding = nextEncoding;
            nextEncoding = 0;
            influence = 0;
            unitFunction = 0;
            updateECs(8000);

            // int start = Clock.getBytecodesLeft();

            minNeutral = minEntryHQ(neutralHQs);
            minEnemy = minEntryHQ(enemyHQs);
            minAllNeutral = entryWithMinVal(neutralHQs);
            minAllEnemy = entryWithMinVal(enemyHQs);
            maxInf = rc.getInfluence() - 30;
            slandCenter = slandCenter();

            if (rc.isReady() && !fullySurrounded()) {
                unitToBuild = getUnitToBuild();
                if (unitToBuild != RobotType.ENLIGHTENMENT_CENTER) {
                    // if (unitToBuild != RobotType.SLANDERER && rc.getInfluence() - 20 >
                    // Constants.minimumPolInf) unitToBuild = RobotType.POLITICIAN;
                    infToSpend = getNewUnitInfluence();
                    if (!unitToBuild.equals(RobotType.SLANDERER) && Constants.optimalSlandInfSet.contains(infToSpend)) {
                        // allows politicians to distinguish slanderers by their influence
                        infToSpend--;
                    }
                    if (unitToBuild == RobotType.POLITICIAN && !spawnLocs.isEmpty()) {
                        dirTarget = directionTo(spawnLocs.remove());
                    } else {
                        dirTarget = getPreferredDirection();
                    }
                    buildDirection = getBuildDirection(unitToBuild, dirTarget, infToSpend);
                    // || (unitToBuild == RobotType.POLITICIAN && Math.random() < 0.2);
                    if (rc.canBuildRobot(unitToBuild, buildDirection, infToSpend)) {
                        rc.buildRobot(unitToBuild, buildDirection, infToSpend);
                        unitsBuilt++;
                        RobotInfo robotBuilt = rc.senseRobotAtLocation(rc.getLocation().add(buildDirection));
                        int builtID = robotBuilt.getID();
                        units.add(builtID);
                        if (unitFunction == 4) {
                        	//polSentID.put(minNeutral.getKey(), builtID);
                        } else if (unitFunction == 2) {
                        	//polSentID.put(minEnemy.getKey(), builtID);
                        }

                        // TODO: Implement flag based orders
                        // trySetFlag(getOrdersForUnit(unitToBuild));
                    }
                }
            }

            trySetFlag(getTarget());

            if (shouldBid()) {
                int bidAmount = (int) (bidController.getBidBaseAmount() * bidController.getBidMultiplier());
                bidLastRound = tryBid(Math.max(bidAmount - (bidAmount % 2), 2));
            } else {
                bidLastRound = false;
            }
            // int numBytecodes = Clock.getBytecodesLeft() - start;
            // if (numBytecodes < -8000) System.out.println(numBytecodes);
            // System.out.println(rc.getEmpowerFactor(allyTeam, 0));

            updateECs(1000);

            Clock.yield();
        }
    }

    private boolean fullySurrounded() throws GameActionException {
    	return rc.senseNearbyRobots(2).length == nbSquares();
    }
    
    private int nbSquares() throws GameActionException {
    	int nbs = 0;
    	for (Direction dir : DirectionUtils.nonCenterDirections) {
    		if (rc.onTheMap(rc.getLocation().add(dir))) {
    			nbs++;
    		}
    	}
    	return nbs;
    }
    
    Map.Entry<MapLocation, Integer> minEntryHQ(Map<MapLocation, Integer> HQs) {
    	Map<MapLocation, Integer> HQsToSpawn = new HashMap<MapLocation, Integer>();
    	for (MapLocation HQ : HQs.keySet()) {
    		if (!polSentID.containsKey(HQ) || !rc.canGetFlag(polSentID.get(HQ))) {
    			HQsToSpawn.put(HQ, HQs.get(HQ));
    		}
    	}
    	return entryWithMinVal(HQsToSpawn);
    }
    
    private Map.Entry<MapLocation, Integer> entryWithMinVal(Map<MapLocation, Integer> HQs) {
        if (HQs.isEmpty()) {
            return null;
        }
        return Collections.min(HQs.entrySet(), Map.Entry.comparingByValue());
    }

    private void parseUnitFlag(int flag) throws GameActionException {
        MapLocation tempLocation = Encoding.getLocationFromFlag(rc, flag);
        switch (Encoding.getTypeFromFlag(flag)) {
            case 2:
                enemyHQs.put(tempLocation, Encoding.getConvFromFlag(flag));
                friendlyHQs.remove(tempLocation);
                neutralHQs.remove(tempLocation);
                break;
            case 3:
                enemyHQs.remove(tempLocation);
                friendlyHQs.add(tempLocation);
                neutralHQs.remove(tempLocation);
                break;
            case 4:
                neutralHQs.put(tempLocation, Encoding.getConvFromFlag(flag));
                break;
            case 7:
                spawnLocs.add(tempLocation);
        }
    }

    MapLocation avgLoc(Set<MapLocation> locs) {
        MapLocation avg = Constants.origin;
        if (locs.size() == 0) {
            return avg;
        }
        for (MapLocation loc : locs) {
            avg = avg.translate(loc.x, loc.y);
        }
        return new MapLocation(avg.x / locs.size(), avg.y / locs.size());
    }

    MapLocation slandCenter() {
        if (enemyHQs.size() == 0) {
            return rc.getLocation();
            //return Constants.origin;
        }
        MapLocation avgEnemyHQ = avgLoc(enemyHQs.keySet());
        Direction awayFromEnemyHQs = rc.getLocation().directionTo(avgEnemyHQ).opposite();
        MapLocation center = rc.getLocation().translate(slandDistAway * awayFromEnemyHQs.dx, slandDistAway * awayFromEnemyHQs.dy);
        return center;
    }

    

    private RobotType getUnitToBuild() throws GameActionException {
        double rand = Math.random();
        if (rc.getRoundNum() <= 2) {
            return RobotType.SLANDERER;
        } else if (maxInf < Constants.minimumPolInf) {
            return RobotType.MUCKRAKER;
        } else if (rc.getEmpowerFactor(allyTeam, 11) > 1.5 || crowdedByEnemy(rc.getLocation()) || !spawnLocs.isEmpty()) {
            return RobotType.POLITICIAN;
        } else if (canSenseEnemyPolitician()) {
            return RobotType.MUCKRAKER;
        } else if (slandCenter != Constants.origin && rand > 0.8) {
            nextEncoding = Encoding.encode(slandCenter, FlagCodes.slandCenter, false);
            influence = rc.getInfluence() > 100 ? Constants.minimumPolInf * 2 : Constants.minimumPolInf;
            return RobotType.POLITICIAN;
        } else if (rand > 0.6 && ((minNeutral != null && maxInf * rc.getEmpowerFactor(allyTeam, 20) >= minNeutral.getValue()) || (minEnemy != null && maxInf * rc.getEmpowerFactor(allyTeam, 20) >= minEnemy.getValue() ) )) {
            return RobotType.POLITICIAN;
        } else if (rand > (0.4 + 0.2 * rc.getRoundNum() / Constants.MAX_ROUNDS)) {
            return RobotType.MUCKRAKER;
        } else if (rand < Math.max(0.3, 0.4 - 0.1 * turnCount / 100) && !canSenseEnemyMuckraker()
                && rc.getEmpowerFactor(enemyTeam, 0) < 1.1) {
            return RobotType.SLANDERER;
        } else {
            return RobotType.ENLIGHTENMENT_CENTER; // build no robot this round
        }
    }

    int getNewUnitInfluence() throws GameActionException {
        if (influence != 0) {
            return influence;
        }
        switch (unitToBuild) {
            case SLANDERER:
                Integer maxOptimalSlandInf = Constants.optimalSlandInfSet.floor(maxInf);
                if (slandCenter != Constants.origin) {
                    nextEncoding = Encoding.encode(slandCenter, FlagCodes.slandCenter);
                }
                return maxOptimalSlandInf != null ? maxOptimalSlandInf : 0;
            case POLITICIAN:
                if (rc.getEmpowerFactor(allyTeam, 11) > 1.5) {
                    return maxInf / 2;
                } else if (minNeutral != null && maxInf * rc.getEmpowerFactor(allyTeam, 20) >= minNeutral.getValue()) {
                    unitFunction = 4;
                    nextEncoding = Encoding.encode(minNeutral.getKey(), FlagCodes.neutralHQ, false);
                    return minNeutral.getValue();
                } else if (minEnemy != null && maxInf * rc.getEmpowerFactor(allyTeam, 20) >= minEnemy.getValue()) {
                    unitFunction = 2;
                    nextEncoding = Encoding.encode(minEnemy.getKey(), FlagCodes.enemyHQ, false);
                    return minEnemy.getValue();
                } else if (Math.random() < 1 && slandCenter != Constants.origin) {
                    nextEncoding = Encoding.encode(slandCenter, FlagCodes.slandCenter, false);
                    return Constants.minimumPolInf;
                } else {
                    return Math.max(Constants.minimumPolInf, maxInf / 2);
                }
            case MUCKRAKER:
                return 1;
            default:
                return 0;
        }
    }

    private int getTarget() throws GameActionException {
        if (encoding != 0) {
            return encoding;
        }

        if (canSenseEnemy()) {
            return Encoding.encode(rc.getLocation(), FlagCodes.patrol);
        } else if (!neutralHQs.isEmpty()
                && ((unitToBuild.equals(RobotType.POLITICIAN) && Math.random() < 0.5) || Math.random() < 0.2)) {
            return Encoding.encode(minAllNeutral.getKey(), FlagCodes.neutralHQ);
        } else if (!enemyHQs.isEmpty()) {
            if (Math.random() < 0.2 && slandCenter != Constants.origin) {
                return Encoding.encode(slandCenter, FlagCodes.slandCenter);
            }
            return Encoding.encode(minAllEnemy.getKey(), FlagCodes.enemyHQ);
        } else {
            return Encoding.encode(rc.getLocation(), FlagCodes.simple);
        }
    }

    Direction getPreferredDirection() throws GameActionException {
        return DirectionUtils.randomDirection();
    }

    private Direction getBuildDirection(RobotType type, Direction prefDir, int inf) throws GameActionException {
        if (rc.getInfluence() < inf || prefDir.equals(Direction.CENTER)) {
            return Direction.CENTER;
        }
        Direction[] dirs = { prefDir, prefDir.rotateRight(), prefDir.rotateLeft(),
                DirectionUtils.rotateRight90(prefDir), DirectionUtils.rotateLeft90(prefDir),
                prefDir.opposite().rotateLeft(), prefDir.opposite().rotateRight(), prefDir.opposite() };
        for (Direction dir : dirs) {
            if (rc.canBuildRobot(type, dir, inf)) {
                return dir;
            }
        }
        return Direction.CENTER;
    }

    private boolean shouldBid() {
        return rc.getTeamVotes() < Constants.VOTES_TO_WIN && rc.getRoundNum() >= Bidding.START_BIDDING_ROUND;
    }

    private boolean tryBid(int bidAmount) throws GameActionException {
        if (rc.canBid(bidAmount)) {
            rc.bid(bidAmount);
            return true;
        }
        return false;
    }

    private class Bidding {
        private static final int MAX_ROUNDS = GameConstants.GAME_MAX_NUMBER_OF_ROUNDS;
        private static final int VOTES_TO_WIN = MAX_ROUNDS / 2 + 1;
        private static final int START_BIDDING_ROUND = MAX_ROUNDS / 8;
        private int prevBidBase = 2, prevTeamVotes = -1;
        private double linGrow = 0.002, multGrow = .02, multDecay = .01;
        private int decayAccum = 0, growAccum = 0;

        private int getBidBaseAmount() {
            if (rc.getTeamVotes() >= VOTES_TO_WIN)
                return 0; // won bidding

            int bidBase = prevBidBase, curTeamVotes = rc.getTeamVotes();
            if (prevTeamVotes != -1 && bidLastRound) {
                if (curTeamVotes > prevTeamVotes) {
                    growAccum = 0;
                    decayAccum += (int) Math.ceil(multDecay * bidBase);
                    bidBase -= decayAccum;
                } else {
                    decayAccum = 0;
                    bidBase += (int) (linGrow * rc.getInfluence());
                    growAccum += (int) Math.ceil(multGrow * bidBase);
                    bidBase += growAccum;
                }
            }
            bidBase = Math.max(bidBase + (bidBase % 2), 2);
            prevBidBase = bidBase;
            prevTeamVotes = curTeamVotes;
            return bidBase;
        }

        private double getBidMultiplier() {
            final int lowerVote = Math.max(VOTES_TO_WIN - MAX_ROUNDS + rc.getRoundNum(), 0);
            final int upperVote = Math.min(rc.getRoundNum(), VOTES_TO_WIN);
            if (rc.getTeamVotes() > upperVote) {
                System.out.println("Error, vote count out of expected bounds.... ????");
                return 1;
            } else if (rc.getTeamVotes() < lowerVote) {
                return 1.7;
            }
            return ((1 + .5 * (.05 * (Math.log(rc.getTeamVotes() + 30 - lowerVote) / Math.log(1.5))))
                    / (1 + Math.exp(0.03 * (rc.getTeamVotes() - lowerVote)))) + 1 + (1 / (upperVote - lowerVote));
        }

    }

    void printStoredECs() {
        System.out.println();
        for (Map.Entry<MapLocation, Integer> entry : enemyHQs.entrySet())
            System.out.println("Enemy EC: " + printLoc(entry.getKey()) + " with conv to spawn " + entry.getValue());
        System.out.println();
        for (MapLocation loc : friendlyHQs)
            System.out.println("Friendly EC: " + printLoc(loc));
        System.out.println();
        for (Map.Entry<MapLocation, Integer> entry : neutralHQs.entrySet())
            System.out.println("Neutral EC: " + printLoc(entry.getKey()) + " with conv to spawn " + entry.getValue());
        System.out.println();
        for (Integer unit : units)
            System.out.println("Unit ID: " + unit);
        System.out.println();
    }

}
