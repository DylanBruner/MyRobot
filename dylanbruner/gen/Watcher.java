package dylanbruner.gen;

import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashMap;

import dylanbruner.MyRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;

public class Watcher {
    public static final Rectangle2D.Double FIELD = new Rectangle2D.Double(0, 0, 800, 600);
    private ArrayList<EnergyListener> listeners = new ArrayList<EnergyListener>();
    private MyRobot parent;

    // Config ================================================================
    private static final int BULLET_HIT_TIME_THRESHOLD = 0; // How long ago the bullet hit must be to be considered
                                                            // "recent"

    // Tracking info ==========================================================
    private static HashMap<String, TrackedRobot> trackedRobots = new HashMap<String, TrackedRobot>();

    public Watcher(MyRobot parent) {
        this.parent = parent;
    }

    // Robocode events ========================================================
    public void onScannedRobot(ScannedRobotEvent e) {
        TrackedRobot robot;
        if (trackedRobots.containsKey(e.getName())) {
            robot = trackedRobots.get(e.getName());
        } else {
            trackedRobots.put(e.getName(), new TrackedRobot());
            robot = trackedRobots.get(e.getName());
            System.out.println("[Watcher] New robot: " + e.getName());
        }

        if (robot.lastEnergy == -1) {
            robot.setLastEnergy(e.getEnergy());
            return;
        }

        robot.__shielding_averageVelocity += e.getVelocity();
        robot.__shielding_sampleCount++;
        if (robot.__shielding_averageVelocity > 10000){robot.__shielding_averageVelocity = 100;} // Prevent overflow
        
        robot.lastLocation = new Point2D.Double(parent.getX() + e.getDistance() * Math.sin(e.getBearingRadians()),
                parent.getY() + e.getDistance() * Math.cos(e.getBearingRadians()));
        robot.absoluteBearing = parent.getHeadingRadians() + e.getBearingRadians();

        if (e.getEnergy() != robot.lastEnergy) {
            double deltaEnergy = robot.lastEnergy - e.getEnergy();
            robot.lastEnergy = e.getEnergy();

            // Figure out the reason for the energy change
            if (robot.lastBulletHitTime != -1
                    && parent.getTime() - robot.lastBulletHitTime < BULLET_HIT_TIME_THRESHOLD) {
                callListeners(deltaEnergy, Reason.HIT_BY_BULLET);
            } else { // This code sorta works, the energy tracking isn't 100% accurate but the gun
                     // heat tracking should be fine
                // Most likely a shot fired (if we're in 1v1)
                double bulletPower = (deltaEnergy < 0 ? 0 : Math.abs(deltaEnergy) / 3);
                
                bulletPower = (double) ((int) (bulletPower * 10.0)) / 10.0;
                robot.setLastShotTime(parent.getTime());
                
                if (bulletPower <= 3 && bulletPower >= 0.1) {
                    robot.lastFirePower = bulletPower;
                    callListeners(bulletPower, Reason.SHOT_BULLET);
                    robot.setLastShotTime(parent.getTime()); // for gun heat tracking
                } else {
                    callListeners(deltaEnergy, Reason.MISC);
                }

            }
        }

        robot.setLastEnergy(e.getEnergy());
    }

    public void onBulletHit(BulletHitEvent e) {
        TrackedRobot robot;
        if (trackedRobots.containsKey(e.getName())) {
            robot = trackedRobots.get(e.getName());
        } else {
            return;
        }

        robot.lastBulletHitTime = parent.getTime();

        robot.shotsHit++;
        if (robot.isShielding() && robot.shotsShielded < robot.shotsHit * 4 && robot.shotsHit > 5 && 
            Math.abs(robot.__shielding_averageVelocity / robot.__shielding_sampleCount) > 0.05) {
            robot.probableShielder = false;
            System.out.println("[Watcher] " + e.getName() + " is probably not a shielder");
        }
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        TrackedRobot robot = trackedRobots.get(e.getHitBullet().getName());

        if (robot == null)
            return;

        robot.lastBulletHitTime = parent.getTime();

        robot.shotsShielded++;
        if (!robot.isShielding() && robot.shotsShielded * 2 > robot.shotsHit && robot.shotsShielded > 5 && 
            Math.abs(robot.__shielding_averageVelocity / robot.__shielding_sampleCount) < 0.005) {
            robot.probableShielder = true;
            System.out.println("[Watcher] " + e.getHitBullet().getName() + " is probably a shielder");
            System.out.println("[Watcher] Total movement: " + (robot.__shielding_averageVelocity / robot.__shielding_sampleCount));
        }
    }

    // Internal functions =====================================================
    private void callListeners(double deltaEnergy, int reason) {
        for (EnergyListener listener : listeners) {
            listener.onEnergyChange(deltaEnergy, reason);
        }
    }

    // User functions ========================================================
    public void register(EnergyListener listener) {
        listeners.add(listener);
    }

    public static TrackedRobot getRobot(String robot) {
        return trackedRobots.get(robot);
    }

    // Classes ============================================================
    public interface EnergyListener {
        public void onEnergyChange(double deltaEnergy, int reason);
    }

    public class Reason {
        public static final int MISC = 0;
        public static final int HIT_BY_BULLET = 1;
        public static final int SHOT_BULLET = 2;
    }

    public class TrackedRobot {
        // Energy tracking & gun heat tracking
        private double lastEnergy = -1; // last energy of the robot
        private double lastFirePower; // raw fire power of the last shot, no rounding
        private long lastShotTime = -1; // last time the robot fired a bullet
        private long lastBulletHitTime; // last time the robot was hit by a bullet

        // Other statistics
        private boolean probableShielder = false; // if the robot is probably a shielder
        private int shotsHit = 0; // Amount of times we've hit them
        private int shotsShielded = 0; // Amount of they've shielded
        private double __shielding_averageVelocity = 0; // Average velocity of the robot while shielding
        private double __shielding_sampleCount = 0; // Amount of samples taken for the average velocity

        // Location info
        private Point2D.Double lastLocation = null; // last location of the robot
        private double absoluteBearing = -1; // absolute bearing of the robot

        // Helpers ========================================================
        public double getGunHeat() {
            double heat = Rules.getGunHeat(lastFirePower);
            if (lastShotTime != -1) {
                heat -= (parent.getTime() - lastShotTime) * parent.getGunCoolingRate();
            } else
                return -1; // unknown or no shot fired

            return heat;
        }

        // Setters ========================================================
        private void setLastEnergy(double energy) {
            lastEnergy = energy;
        }

        private void setLastShotTime(long time) {
            lastShotTime = time;
        }

        // Getters ========================================================
        public double getLastEnergy() {
            return lastEnergy;
        }

        public double getLastFirePower() {
            return lastFirePower;
        }

        public long getLastShotTime() {
            return lastShotTime;
        }

        public long getLastBulletHitTime() {
            return lastBulletHitTime;
        }

        public boolean isShielding() {
            return probableShielder;
        }

        public int getShotsHit() {
            return shotsHit;
        }

        public int getShotsShielded() {
            return shotsShielded;
        }

        public Point2D.Double getLastLocation() {
            return lastLocation;
        }

        public double getAbsoluteBearing() {
            return absoluteBearing;
        }
    }
}
