package dylanbruner.gun.GFGun;

import java.awt.geom.Point2D;

import dylanbruner.utils.RollingAverage;
import dylanbruner.utils.myUtils;
import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.util.Utils;

public class GFWave extends Condition {
    public static Point2D targetLocation;

    public double bulletPower;
    public Point2D gunLocation;
    public double bearing;
    public double lateralDirection;

    public static double MAX_DISTANCE = 1000;
    public static int DISTANCE_INDEXES = 10;
    public static int VELOCITY_INDEXES = 10;
    public static int BINS = 31;
    public static int MIDDLE_BIN = (BINS - 1) / 2;
    public static final double MAX_ESCAPE_ANGLE = 0.7;
    public static double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double) MIDDLE_BIN;

    public static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
    public static RollingAverage[][][] rollingAverages = new RollingAverage[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES];

    public int[] buffer;
    public AdvancedRobot robot;
    public double distanceTraveled;

    // For tuning only!!!
    public static void recalculateBuffers(int maxDistance, int distanceIndexes, int velocityIndexes, int bins) {
        MAX_DISTANCE = maxDistance;
        DISTANCE_INDEXES = distanceIndexes;
        VELOCITY_INDEXES = velocityIndexes;
        BINS = bins;
        MIDDLE_BIN = (BINS - 1) / 2;
        BIN_WIDTH = MAX_ESCAPE_ANGLE / (double) MIDDLE_BIN;

        statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
        rollingAverages = new RollingAverage[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES];
    }

    public GFWave(AdvancedRobot _robot) {
        this.robot = _robot;

        for (int i = 0; i < DISTANCE_INDEXES; i++) {
            for (int j = 0; j < VELOCITY_INDEXES; j++) {
                for (int k = 0; k < VELOCITY_INDEXES; k++) {
                    rollingAverages[i][j][k] = new RollingAverage();
                }
            }
        }
    }

    public boolean test() {
        advance();
        if (hasArrived()) {
            buffer[currentBin()]++;
            robot.removeCustomEvent(this);
        }
        return false;
    }

    public double mostVisitedBearingOffset() {
        // return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
        double rollingAverage = getRollingAverage(distanceTraveled, robot.getVelocity(), robot.getVelocity());
        return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN) + rollingAverage;
    }

    public void setSegmentations(double distance, double velocity, double lastVelocity) {
        int distanceIndex = (int) (distance / (MAX_DISTANCE / DISTANCE_INDEXES));
        int velocityIndex = (int) Math.abs(velocity / 2);
        int lastVelocityIndex = (int) Math.abs(lastVelocity / 2);
        buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];

        rollingAverages[distanceIndex][velocityIndex][lastVelocityIndex].addValue(buffer[currentBin()]);
    }

    public double getRollingAverage(double distance, double velocity, double lastVelocity) {
        int distanceIndex = (int) (distance / (MAX_DISTANCE / DISTANCE_INDEXES));
        int velocityIndex = (int) Math.abs(velocity / 2);
        int lastVelocityIndex = (int) Math.abs(lastVelocity / 2);
        return rollingAverages[distanceIndex][velocityIndex][lastVelocityIndex].getAverage();
    }

    private void advance() {
        distanceTraveled += myUtils.bulletVelocity(bulletPower);
    }

    private boolean hasArrived() {
        return distanceTraveled > gunLocation.distance(targetLocation) - 18;
    }

    private int currentBin() {
        int bin = (int) Math
                .round(((Utils.normalRelativeAngle(myUtils.absoluteBearing(gunLocation, targetLocation) - bearing)) /
                        (lateralDirection * BIN_WIDTH)) + MIDDLE_BIN);
        return myUtils.minMax(bin, 0, BINS - 1);
    }

    private int mostVisitedBin() {
        int mostVisited = MIDDLE_BIN;
        for (int i = 0; i < BINS; i++) {
            if (buffer[i] > buffer[mostVisited]) {
                mostVisited = i;
            }
        }
        return mostVisited;
    }
}