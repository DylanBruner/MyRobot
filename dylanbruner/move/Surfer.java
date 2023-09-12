package dylanbruner.move;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.util.ArrayList;

import dylanbruner.MyRobot;

import java.awt.*;

public class Surfer {
    public static int BINS = 47;
    public static double surfStats[][][] = new double[8][12][BINS];
    public Point2D.Double myLocation;
    public Point2D.Double enemyLocation;

    public Point2D.Double lastGoToPoint;
    public double direction = 1;
    public long lastDirChangeTime = 0;
    public int lastDirection = 0;

    public ArrayList<EnemyWave> enemyWaves = new ArrayList<EnemyWave>();
    public ArrayList<Integer> surfDirections = new ArrayList<Integer>();
    public ArrayList<Double> surfAbsBearings = new ArrayList<Double>();

    public static double oppEnergy = 100.0;

    public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
    public static double WALL_STICK = 160;

    AdvancedRobot parent;

    public Surfer(MyRobot parent) {
        this.parent = parent;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        myLocation = new Point2D.Double(parent.getX(), parent.getY());

        double lateralVelocity = parent.getVelocity() * Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + parent.getHeadingRadians();

        parent.setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - parent.getRadarHeadingRadians()) * 2);

        surfDirections.add(0, (lateralVelocity >= 0) ? 1 : -1);
        surfAbsBearings.add(0, absBearing + Math.PI);

        double bulletPower = oppEnergy - e.getEnergy();
        if (bulletPower < 3.01 && bulletPower > 0.09
                && surfDirections.size() > 2) {
            
            EnemyWave ew = new EnemyWave();
            ew.fireTime = parent.getTime() - 1;
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = bulletVelocity(bulletPower);
            ew.direction = ((Integer) surfDirections.get(2));
            ew.directAngle = ((Double) surfAbsBearings.get(2));
            ew.fireLocation = (Point2D.Double) enemyLocation.clone(); // last tick
            enemyWaves.add(ew);
        }

        oppEnergy = e.getEnergy();

        enemyLocation = project(myLocation, absBearing, e.getDistance());

        updateWaves();
        doSurfing();
    }

    public void updateWaves() {
        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave) enemyWaves.get(x);

            ew.distanceTraveled = (parent.getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled > myLocation.distance(ew.fireLocation) + 50) {
                enemyWaves.remove(x);
                x--;
            }
        }
    }

    public EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000;
        EnemyWave surfWave = null;

        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave) enemyWaves.get(x);
            double distance = myLocation.distance(ew.fireLocation)
                    - ew.distanceTraveled;

            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }

        return surfWave;
    }

    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
                - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / maxEscapeAngle(ew.bulletVelocity) * ew.direction;

        return (int) limit(0,
                (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
                BINS - 1);
    }

    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        int distance = (int) ew.fireLocation.distance(targetLocation) / 100;
        int lateralVelocity = (int) Math.abs(parent.getVelocity()
                * Math.sin(ew.directAngle - absoluteBearing(ew.fireLocation, targetLocation)));
        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[lateralVelocity][distance][x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                    e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;

            for (int x = 0; x < enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave) enemyWaves.get(x);

                if (Math.abs(ew.distanceTraveled -
                        myLocation.distance(ew.fireLocation)) < 50
                        && Math.abs(bulletVelocity(e.getBullet().getPower())
                                - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }

            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);

                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    // CREDIT: mini sized predictor from Apollon, by rozu
    // http://robowiki.net?Apollon
    public ArrayList<Point2D.Double> predictPositions(EnemyWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double) myLocation.clone();
        double predictedVelocity = parent.getVelocity();
        double predictedHeading = parent.getHeadingRadians();
        double maxTurning, moveAngle, moveDir;
        ArrayList<Point2D.Double> traveledPoints = new ArrayList<Point2D.Double>();

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;

        do {
            double distance = predictedPosition.distance(surfWave.fireLocation);
            double offset = Math.PI / 2 - 1 + distance / 400;

            moveAngle = wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                    predictedPosition) + (direction * (offset)), direction)
                    - predictedHeading;
            moveDir = 1;

            if (Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }

            moveAngle = Utils.normalRelativeAngle(moveAngle);

            maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                    + limit(-maxTurning, moveAngle, maxTurning));

            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir);
            predictedVelocity = limit(-8, predictedVelocity, 8);

            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);

            traveledPoints.add(predictedPosition);

            counter++;

            if (predictedPosition.distance(surfWave.fireLocation) - 20 < surfWave.distanceTraveled
                    + (counter * surfWave.bulletVelocity)) {
                // System.out.println("Calculation intercepted at " + counter + " ticks.");
                if (counter >= 15){
                    parent.setMaxVelocity(6);
                } else {
                    parent.setMaxVelocity(8);
                }
                intercepted = true;
            }
        } while (!intercepted && counter < 500);

        if (traveledPoints.size() > 1)
            traveledPoints.remove(traveledPoints.size() - 1);

        return traveledPoints;
    }

    public double checkDanger(EnemyWave surfWave, Point2D.Double position) {
        int index = getFactorIndex(surfWave, position);
        double distance = position.distance(surfWave.fireLocation);
        int lateralVelocity = (int) Math.abs(parent.getVelocity()
                * Math.sin(surfWave.directAngle - absoluteBearing(surfWave.fireLocation, position)));
        return surfStats[lateralVelocity][(int) distance / 100][index] / distance;
    }

    public Point2D.Double getBestPoint(EnemyWave surfWave) {
        if (surfWave.safePoints == null) {
            ArrayList<Point2D.Double> forwardPoints = predictPositions(surfWave, 1);
            ArrayList<Point2D.Double> reversePoints = predictPositions(surfWave, -1);
            int FminDangerIndex = 0;
            int RminDangerIndex = 0;
            double FminDanger = Double.POSITIVE_INFINITY;
            double RminDanger = Double.POSITIVE_INFINITY;
            for (int i = 0, k = forwardPoints.size(); i < k; i++) {
                double thisDanger = checkDanger(surfWave, (Point2D.Double) (forwardPoints.get(i)));
                if (thisDanger <= FminDanger) {
                    FminDangerIndex = i;
                    FminDanger = thisDanger;
                }
            }
            for (int i = 0, k = reversePoints.size(); i < k; i++) {
                double thisDanger = checkDanger(surfWave, (Point2D.Double) (reversePoints.get(i)));
                if (thisDanger <= RminDanger) {
                    RminDangerIndex = i;
                    RminDanger = thisDanger;
                }
            }
            ArrayList<Point2D.Double> bestPoints;
            int minDangerIndex;

            if (FminDanger < RminDanger) {
                bestPoints = forwardPoints;
                minDangerIndex = FminDangerIndex;
            } else {
                bestPoints = reversePoints;
                minDangerIndex = RminDangerIndex;
            }

            Point2D.Double bestPoint = (Point2D.Double) bestPoints.get(minDangerIndex);

            while (bestPoints.indexOf(bestPoint) != -1)
                bestPoints.remove(bestPoints.size() - 1);
            bestPoints.add(bestPoint);

            surfWave.safePoints = bestPoints;

            bestPoints.add(0, new Point2D.Double(parent.getX(), parent.getY()));

        } else if (surfWave.safePoints.size() > 1)
            surfWave.safePoints.remove(0);

        if (surfWave.safePoints.size() >= 1) {
            for (int i = 0, k = surfWave.safePoints.size(); i < k; i++) {
                Point2D.Double goToPoint = (Point2D.Double) surfWave.safePoints.get(i);
                if (goToPoint.distanceSq(myLocation) > 20 * 20 * 1.1)
                    return goToPoint;
            }
            return (Point2D.Double) surfWave.safePoints.get(surfWave.safePoints.size() - 1);

        }

        return null;
    }

    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();
        double distance = enemyLocation.distance(myLocation);
        if (surfWave == null || distance < 50) {
            // do 'away' movement best distance of 400 - modified from RaikoNano
            double absBearing = absoluteBearing(myLocation, enemyLocation);
            double headingRadians = parent.getHeadingRadians();
            double stick = 160;// Math.min(160,distance);
            double v2, offset = Math.PI / 2 + 1 - distance / 400;

            while (!_fieldRect.contains(project(myLocation, v2 = absBearing + direction * (offset -= 0.02), stick)))
                ;

            if (offset < Math.PI / 3)
                direction = -direction;

            parent.setAhead(50 * Math.cos(v2 - headingRadians));
            parent.setTurnRightRadians(Math.tan(v2 - headingRadians));
        } else {
            goTo(getBestPoint(surfWave));
        }
    }

    private void goTo(Point2D.Double destination) {
        if (destination == null) {
            if (lastGoToPoint != null)
                destination = lastGoToPoint;
            else
                return;
        }

        lastGoToPoint = destination;
        Point2D.Double location = new Point2D.Double(parent.getX(), parent.getY());
        double distance = location.distance(destination);
        double angle = Utils.normalRelativeAngle(absoluteBearing(location, destination) - parent.getHeadingRadians());
        if (Math.abs(angle) > Math.PI / 2) {
            distance = -distance;
            if (angle > 0) {
                angle -= Math.PI;
            } else {
                angle += Math.PI;
            }
        }

        parent.setTurnRightRadians(angle * Math.signum(Math.abs((int) distance)));

        parent.setAhead(distance);
    }

    // CREDIT: Iterative WallSmoothing by Kawigi
    // - return absolute angle to move at after account for WallSmoothing
    // robowiki.net?WallSmoothing
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(project(botLocation, angle, 160))) {
            angle += orientation * 0.05;
        }
        return angle;
    }

    // CREDIT: from CassiusClay, by PEZ
    // - returns point length away from sourceLocation, at angle
    // robowiki.net?CassiusClay
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
                sourceLocation.y + Math.cos(angle) * length);
    }

    // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
    // - returns the absolute angle (in radians) from source to target points
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double bulletVelocity(double power) {
        return (20D - (3D * power));
    }

    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0 / velocity);
    }

    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI / 2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1 * angle);
            } else {
                robot.setTurnRightRadians(angle);
            }
            robot.setAhead(100);
        }
    }

    public void onPaint(java.awt.Graphics2D g) {
        g.setColor(Color.red);
        for (int i = 0; i < enemyWaves.size(); i++) {
            EnemyWave w = (EnemyWave) (enemyWaves.get(i));
            Point2D.Double center = w.fireLocation;

            int radius = (int) w.distanceTraveled;

            if (radius - 40 < center.distance(myLocation)){
                if (w.isInvalid){
                    g.setColor(Color.cyan);
                } else {
                    g.setColor(Color.red);
                }
                g.drawOval((int) (center.x - radius), (int) (center.y - radius), radius * 2, radius * 2);

                // draw the safe points as long as there are some
                g.setColor(Color.blue);
                if (w.safePoints != null) {
                    for (int j = 0; j < w.safePoints.size(); j++) {
                        Point2D.Double p = (Point2D.Double) w.safePoints.get(j);
                        // g.drawOval((int) (p.x) - 4, (int) (p.y) - 4, 8, 8);
                        // filled in version
                        g.fillOval((int) (p.x) - 4, (int) (p.y) - 4, 8, 8);
                    }
                }
            }
        }
    }
}