package usqualplayer2;

import battlecode.common.*;
import common.*;

class Muckraker extends Attacker {
	private boolean wasEnemyHQMuckSaturated = false;
	protected boolean defending = false;

	Muckraker(RobotController rcin) throws GameActionException {
		super(rcin);
	}

	void run() throws GameActionException {
		while (true) {
			turnCount++;
			if (hasOrCanGetHomeHQ()) {
				parseHQFlag(rc.getFlag(hqID));
				if (explorer && !defending) {
					runSimpleCode();
				} else if ((mode == FlagCodes.enemyHQ || (enemyHQ != null && enemyHQIsCurrent())) && !wasEnemyHQMuckSaturated) {
					runAttackCode();
				} else if (mode == FlagCodes.patrol && defending) {
					runDefendCode();
				} else {
					runSimpleCode();
				}
			} else {
				if (enemyHQ != null && enemyHQIsCurrent() && !wasEnemyHQMuckSaturated) {
					runAttackCode();
				} else {
					runSimpleCode();
				}
			}

			setNearbyHQFlag();

			Clock.yield();
		}
	}

	private void parseHQFlag(int flag) throws GameActionException {
		MapLocation tempLocation = Encoding.getLocationFromFlag(rc, flag);
		switch (Encoding.getTypeFromFlag(flag)) {
			case FlagCodes.enemyHQ:
				if (enemyHQ == null) {
					enemyHQ = tempLocation;
					wasEnemyHQMuckSaturated = false;
				} else if (minMovesLeft(rc.getLocation(), enemyHQ) > 20 && wasEnemyHQMuckSaturated) {
					wasEnemyHQMuckSaturated = Math.random() > .01;
				}
				defending = false;
				mode = FlagCodes.enemyHQ;
				break;
			case FlagCodes.patrol:
				if (mode != FlagCodes.patrol) {
					if (distanceSquaredTo(hqLocation) < actionRadiusSquared) {
						defending = Math.random() < .8;
					}
					mode = FlagCodes.patrol;
				}
				break;
			default:
				defending = false;
				mode = FlagCodes.simple;
				break;
		}
	}

	private void runDefendCode() throws GameActionException {
		if (!rc.isReady()) {
			return;
		} else if (hqLocation == null) {
			runSimpleCode();
			return;
		} else if (distanceSquaredTo(hqLocation) > actionRadiusSquared / 2
				&& tryMove(pathingController.dirToTarget(hqLocation))) {
			return;
		} else {
			pathingController.resetPrevMovesAndDir();
			tryDirForward90180(directionTo(hqLocation).opposite());
		}
	}

	private boolean huntOrExposeSlanderer() throws GameActionException {
		RobotInfo[] nearby = rc.senseNearbyRobots();
		int maxExposeInf = 0, maxSenseInf = 0;
		MapLocation robotExposeLoc = Constants.origin, robotSenseLoc = Constants.origin;

		for (RobotInfo robot : nearby) {
			if (robot.type.equals(RobotType.SLANDERER) && robot.team.equals(enemyTeam)) {
				if (robot.influence > maxSenseInf) {
					maxSenseInf = robot.influence;
					robotSenseLoc = robot.location;
				}
				if (rc.canExpose(robot.location) && robot.influence > maxExposeInf) {
					maxExposeInf = robot.influence;
					robotExposeLoc = robot.location;
				}
			}
		}

		if (robotExposeLoc != Constants.origin) {
			return tryExpose(robotExposeLoc);
		} else if (robotSenseLoc != Constants.origin) {
			tryDirForward90(directionTo(robotSenseLoc));
			return true;
		}
		return false;
	}

	private void HQAttackRoutine(MapLocation locHQ) throws GameActionException {
		if (huntOrExposeSlanderer()) {
			return;
		} else if (distanceSquaredTo(locHQ) <= 2) {
			return;
		} else if (distanceSquaredTo(locHQ) > 17) {
			tryMove(pathingController.dirToTarget(enemyHQ));
		} else if (isEnemyHQMuckSaturated(locHQ)) {
			wasEnemyHQMuckSaturated = true;
			dirTarget = DirectionUtils.randomDirection();
			runSimpleCode();
			return;
		} else {
			tryMove(pathingController.dirToTarget(enemyHQ));
		}
	}

	private boolean isEnemyHQMuckSaturated(MapLocation locHQ) throws GameActionException {
		if (rc.canSenseLocation(locHQ) && distanceSquaredTo(locHQ) <= 17) {
			return rc.senseNearbyRobots(locHQ, 2, allyTeam).length == 8;
		}
		return false;
	}

	private void runAttackCode() throws GameActionException {
		if (rc.canSenseLocation(enemyHQ) && !rc.senseRobotAtLocation(enemyHQ).getTeam().equals(enemyTeam)){
			enemyHQ = null;
		}
		if (!rc.isReady()) {
			return;
		}
		if (enemyHQ == null) {
			runSimpleCode();
			return;
		}
		HQAttackRoutine(enemyHQ);
	}

	private void runSimpleCode() throws GameActionException {
		if (!rc.isReady()) {
			return;
		}

		pathingController.resetPrevMovesAndDir();
		updateDirIfOnBorder();

		if (!huntOrExposeSlanderer() && !tryDirForward90(dirTarget)) {
			tryDirForward90180(awayFromAllies());
		}

	}

	private boolean tryExpose(MapLocation pos) throws GameActionException {
		if (rc.canExpose(pos)) {
			rc.expose(pos);
			return true;
		}
		return false;
	}

}
