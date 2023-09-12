package dylanbruner.gun.GFGun;

import robocode.*;

import dylanbruner.MyRobot;

// private static double MAX_DISTANCE = 1000;
// private static int DISTANCE_INDEXES = 10;
// private static int VELOCITY_INDEXES = 10;
// private static int BINS = 31;

public class GunTuner {
    MyRobot parent;

    // CONFIG ====================
    static final double TEST_ROUNDS   = 100; // Number of rounds to test each configuration
    static final int MAX_TRIAL_CHANGE = 10; // Maximum change in each parameter per trial
    static final int MAX_CHANGE       = 100; // Maximum change in each parameter

    // Base Values
    static final double MAX_DISTANCE  = GFWave.MAX_DISTANCE;
    static final int DISTANCE_INDEXES = GFWave.DISTANCE_INDEXES;
    static final int VELOCITY_INDEXES = GFWave.VELOCITY_INDEXES;
    static final int BINS             = GFWave.BINS;

    // EO CONFIG =================

    long lastDraw = 0;

    private static MainGun gun;
    private ConfigSnapshot currentSnapshot;
    private ConfigSnapshot bestSnapshot;
    private int trialStartRound = -1;

    public GunTuner(MyRobot parent) {
        this.parent = parent;

        if (gun == null) gun = new MainGun(parent);

        currentSnapshot = new ConfigSnapshot(MAX_DISTANCE, DISTANCE_INDEXES, VELOCITY_INDEXES, BINS);
        bestSnapshot = currentSnapshot;
        currentSnapshot.applyParameters();
        trialStartRound = parent.getRoundNum();
        System.out.println("[TUNER] Starting with configuration: " + currentSnapshot);
    }

    // We don't pass any events other than onScan to stop the gun from debug-printing
    public void onScannedRobot(ScannedRobotEvent e) {gun.onScannedRobot(e);}
    public void onBulletHit(BulletHitEvent e) {currentSnapshot.hits++;}
    public void onBulletMissed(BulletMissedEvent e) {currentSnapshot.misses++;}
    public void onRoundEnded(RoundEndedEvent e) {
        if (trialStartRound == -1) return;
        
        if (e.getRound() - trialStartRound >= TEST_ROUNDS) {
            System.out.println("[TUNER] Trial (" + currentSnapshot + ") ended with accuracy " + currentSnapshot.getAccuracy());
            if (bestSnapshot == null || currentSnapshot.getAccuracy() > bestSnapshot.getAccuracy()) {
                bestSnapshot = currentSnapshot;
                System.out.println("[TUNER::BEST] New best configuration: " + bestSnapshot);
            }
            currentSnapshot = new ConfigSnapshot(bestSnapshot);
            // gun = new MainGun(parent);
            currentSnapshot.applyParameters(); // Update the gun with the new parameters
            trialStartRound = e.getRound();
            System.out.println("[TUNER] Starting new trial with configuration: " + currentSnapshot);
            System.out.println("[TUNER] Best configuration: " + bestSnapshot);
        }
    }

    public void onPaint(java.awt.Graphics2D g) {
        // White background for the text
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, -55, 800, 50);
        if (currentSnapshot != null){
            g.setColor(java.awt.Color.RED);
            // increase font size
            g.setFont(g.getFont().deriveFont(16f));
            g.drawString("Current Configuration: " + currentSnapshot, 10, -20);
        }
        // paint the current best configuration
        if (bestSnapshot != null) {
            g.setColor(java.awt.Color.GREEN);
            // increase font size
            g.setFont(g.getFont().deriveFont(16f));
            g.drawString("Best Configuration: " + bestSnapshot, 10, -40);
        }
    }

    class ConfigSnapshot {
        private double maxDistance;
        private int distanceIndexes;
        private int velocityIndexes;
        private int bins;

        private int hits;
        private int misses;

        ConfigSnapshot(double maxDistance, int distanceIndexes, int velocityIndexes, int bins) {
            this.maxDistance = maxDistance;
            this.distanceIndexes = distanceIndexes;
            this.velocityIndexes = velocityIndexes;
            this.bins = bins;
        }

        ConfigSnapshot(ConfigSnapshot other) {
            this(other.maxDistance, other.distanceIndexes, other.velocityIndexes, other.bins);

            // perform a mutation on a random parameter, limit the change to MAX_TRIAL_CHANGE and MAX_CHANGE (aka max overall change)
            switch ((int) (Math.random() * 3)) {
                case 0:
                    maxDistance = modify(maxDistance, MAX_DISTANCE);
                    System.out.println("Max Distance: " + maxDistance);
                    break;
                case 1:
                    distanceIndexes = (int) modify(distanceIndexes, DISTANCE_INDEXES);
                    System.out.println("Distance Indexes: " + distanceIndexes);
                    break;
                case 2:
                    velocityIndexes = (int) modify(velocityIndexes, VELOCITY_INDEXES);
                    System.out.println("Velocity Indexes: " + velocityIndexes);
                    break;
                case 3:
                    bins = (int) modify(bins, BINS);
                    System.out.println("Bins: " + bins);
                    break;
            }
        }

        private double modify(double value, double base){
            int sign = 1;
            double newVal = value + sign;
            // limit the change to MAX_TRIAL_CHANGE and MAX_CHANGE (aka max overall change)
            if (Math.abs(newVal - base) > MAX_TRIAL_CHANGE || Math.abs(newVal - value) > MAX_CHANGE) {
                newVal = value - sign;
            }

            return newVal;
        }

        // Utility Methods
        public void applyParameters() {
            GFWave.recalculateBuffers((int) maxDistance, distanceIndexes, velocityIndexes, bins);
        }

        // Getters
        public double getMaxDistance() {return maxDistance;}
        public int getDistanceIndexes() {return distanceIndexes;}
        public int getVelocityIndexes() {return velocityIndexes;}
        public int getBins() {return bins;}

        public double getAccuracy() {return (double) hits / (hits + misses);}
        public String toString() {
            return "Max Distance: " + maxDistance + " Distance Indexes: " + distanceIndexes + " Velocity Indexes: " + velocityIndexes + " Bins: " + bins + " Accuracy: " + 
            Math.round(getAccuracy() * 10000.0) / 100.0 + "%";
        }
    }
}