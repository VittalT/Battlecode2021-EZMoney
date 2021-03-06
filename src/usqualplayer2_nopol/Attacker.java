package usqualplayer2_nopol;

import battlecode.common.*;

public abstract class Attacker extends Pawn {
    protected MapLocation enemyHQ = null;
    protected MapLocation neutralHQ = null;
    Attacker(RobotController rcin) throws GameActionException {
        super(rcin);
    }

    protected boolean withinAttackRange(RobotInfo enemy) throws GameActionException {
        return withinAttackRange(enemy.getLocation());
    }

    protected boolean withinAttackRange(MapLocation enemyLocation) throws GameActionException {
        return rc.getLocation().distanceSquaredTo(enemyLocation) <= actionRadiusSquared;
    }

    protected boolean enemyHQIsCurrent() throws GameActionException {
        if (enemyHQ != null && rc.canSenseLocation(enemyHQ)) {
            RobotInfo tempHQ = rc.senseRobotAtLocation(enemyHQ);
            if (!tempHQ.getTeam().equals(enemyTeam)) {
                enemyHQ = null;
                encoded = Encoding.encode(tempHQ.getLocation(), flagCodeFromHQTeam(tempHQ.team), tempHQ.conviction);
                return false;
            }
        }
        return true;
    }

    protected boolean neutralHQIsCurrent() throws GameActionException {
        if (neutralHQ != null && rc.canSenseLocation(neutralHQ)) {
            RobotInfo tempHQ = rc.senseRobotAtLocation(neutralHQ);
            if (!tempHQ.getTeam().equals(Team.NEUTRAL)) {
                neutralHQ = null;
                encoded = Encoding.encode(tempHQ.getLocation(), flagCodeFromHQTeam(tempHQ.team), tempHQ.conviction);
                return false;
            }
        }
        return true;
    }
}
