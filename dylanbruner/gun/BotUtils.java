package dylanbruner.gun;
import robocode.*;

/**
 * BotUtils - a class by ABC
 */
final class BotUtils {
	
	public static double sqr(double x) {
		return x * x;
	}
	
	public static double sinD(double ang) {
		return(Math.sin(Math.toRadians(ang)));
	}
	
	public static double cosD(double ang) {
		return(Math.cos(Math.toRadians(ang)));
	}
	
	public static double tanD(double ang) {
		return(Math.tan(Math.toRadians(ang)));
	}
	
	public static double angleTo(double x1, double y1, double x2, double y2) {
		return(Math.toDegrees(Math.PI/2 - Math.atan2(y2 - y1, x2 - x1)));
	}
	
	public static double angleToRadians(double x1, double y1, double x2, double y2) {
		return(Math.PI/2 - Math.atan2(y2 - y1, x2 - x1));
	}
	
	public static double angleTo(AdvancedRobot bot, double x, double y) {
		return(Math.toDegrees(Math.PI/2 - Math.atan2(y - bot.getY(), x - bot.getX())));
	}
	
	public static double angleToRadians(AdvancedRobot bot, double x, double y) {
		return(Math.PI/2 - Math.atan2(y - bot.getY(), x - bot.getX()));
	}
	
	public static double distanceTo(double x1, double y1, double x2, double y2) {
		return(Math.sqrt(sqr(x2 - x1) + sqr(y2 - y1)));
	}
	
	public static double distanceTo(AdvancedRobot bot, double x2, double y2) {
		return(Math.sqrt(sqr(x2 - bot.getX()) + sqr(y2 - bot.getY())));
	}
	
	public static double normalizeBearing(double ang) {
		ang = ang % 360;
		if (ang > 180) ang -= 360;
		if (ang < -180) ang += 360;
		return ang;
	}
	
	public static double normalizeBearingRadians(double ang) {
		ang = ang % Math.PI * 2;
		if (ang > Math.PI) ang -= Math.PI * 2;
		if (ang < -Math.PI) ang += Math.PI * 2;
		return ang;
	}
	
	public static void setTurnToAngle(AdvancedRobot bot, double ang) {
		bot.setTurnRight(normalizeBearing(ang - bot.getHeading()));
	}
	
	public static void setTurnToAngleRadians(AdvancedRobot bot, double ang) {
		bot.setTurnLeftRadians(normalizeBearingRadians(bot.getHeadingRadians() - ang));
	}
	
	public static double setTurnToAngle90(AdvancedRobot bot, double ang) {
		double ang1 = normalizeBearing(bot.getHeading() - ang);
		if (ang1 > 90) ang1 -= 180;
		if (ang1 < -90) ang1 += 180;
		bot.setTurnLeft(ang1);
		return (bot.getHeading() < 90 || bot.getHeading() > 270 ? 1 : -1);
	}
	
	public static void setTurnGunToAngle(AdvancedRobot bot, double ang) {
		bot.setTurnGunLeft(normalizeBearing(bot.getGunHeading() - ang));
	}
	
	public static void setTurnRadarToAngle(AdvancedRobot bot, double ang) {
		bot.setTurnRadarLeft(normalizeBearing(bot.getRadarHeading() - ang));
	}
	
	public static void setGoto(AdvancedRobot bot, double x, double y) {
        double dist = Math.sqrt(sqr(bot.getX() - x) + sqr(bot.getY() - y));
        double ang = normalizeBearing(angleTo(bot, x, y) - bot.getHeading());
        if (Math.abs(ang) > 90.0) {
            dist *= -1.0;
            if (ang > 0.0) {
                ang -= 180.0;
            }
            else {
                ang += 180.0;
            }
        }
		if (Math.abs(ang) > 20) bot.setMaxVelocity(3);
		else bot.setMaxVelocity(8);
        bot.setTurnRight(ang);
        bot.setAhead(dist);
    }
}
