package sprintplayer2;

import battlecode.common.*;
import java.util.*;
import common.*;

class Slanderer extends Pawn {
	Politician successor;
	private MapLocation slandCenter = null;

	Slanderer(RobotController rcin) throws GameActionException {
		super(rcin); // Don't remove this.
	}

	void run() throws GameActionException {
		while (rc.getType().equals(RobotType.SLANDERER)) {
			turnCount++;
			if (rc.isReady()) {
				if (!runToSlandCenter()) {
					if (rc.canGetFlag(hqID)) {
						parseHQFlag(rc.getFlag(hqID));
					} else {
						runSimpleCode();
					}
				}
			} else {
				if (slandCenter == null && rc.canGetFlag(hqID)) {
					parseHQFlag(rc.getFlag(hqID));
				}
			}
			// setNearbyHQFlag();

			Clock.yield();
		}
		if (successor == null) {
			successor = new Politician(this);
		}
		successor.run();
	}

	private void parseHQFlag(int flag) throws GameActionException {
		MapLocation tempLocation = Encoding.getLocationFromFlag(rc, flag);
		switch (Encoding.getInfoFromFlag(flag)) {
			case 6:
				slandCenter = tempLocation;
				runToSlandCenter();
				break;
			default:
				runSimpleCode();
				break;
		}
	}

	private boolean runToSlandCenter() throws GameActionException {
		if (!rc.isReady()) {
			return false;
		}

		if (slandCenter == null || hqLocation == null) {
			return false;
		}

		if (!tryDirForward180(awayFromEnemyMuckrakers())) {
			tryDirForward90(directionTo(slandCenter));
		}
		return true;
	}

	private boolean runSimpleCode() throws GameActionException {
		if (!rc.isReady()) {
			return false;
		}

		return tryDirForward180(awayFromEnemyMuckrakers());
	}

	protected Direction awayFromEnemyMuckrakers() throws GameActionException {
		ArrayList<RobotInfo> robots = new ArrayList<>(
				Arrays.asList(rc.senseNearbyRobots(sensorRadiusSquared, enemyTeam)));
		if (robots.size() == 0) {
			return dirTarget;
		}
		robots.removeIf(r -> (r.type != RobotType.MUCKRAKER));
		return awayFromRobots(robots);
	}
}