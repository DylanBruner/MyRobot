package dylanbruner.gun;

import robocode.*;
import robocode.util.*;
import java.awt.geom.*;

import dylanbruner.MyRobot;
import dylanbruner.utils.myUtils;
import dylanbruner.gen.Watcher;

public class MainGun extends Gun implements Watcher.EnergyListener {
    static int shots = 0;
    static int hits = 0;

    private static double lateralDirection;
    private static double lastEnemyVelocity;

    MyRobot parent;

    public MainGun(MyRobot parent) {
        super(parent);
        this.parent = parent;
        lateralDirection = 1;
        lastEnemyVelocity = 0;
    }

    // Robocode events ==========================================================
    public void onScannedRobot(ScannedRobotEvent e) {
        double enemyAbsoluteBearing = parent.getHeadingRadians() + e.getBearingRadians();
        double enemyDistance = e.getDistance();
        double enemyVelocity = e.getVelocity();
        if (enemyVelocity != 0) {
            lateralDirection = myUtils.sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
        }
        GFWave wave = new GFWave(parent);
        wave.gunLocation = new Point2D.Double(parent.getX(), parent.getY());
        GFWave.targetLocation = myUtils.project(wave.gunLocation, enemyAbsoluteBearing, enemyDistance);
        wave.lateralDirection = lateralDirection;
        wave.bulletPower = getGunPower(e);
        wave.setSegmentations(enemyDistance, enemyVelocity, lastEnemyVelocity);
        lastEnemyVelocity = enemyVelocity;
        wave.bearing = enemyAbsoluteBearing;
        parent.setTurnGunRightRadians(Utils
                .normalRelativeAngle(
                        enemyAbsoluteBearing - parent.getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
        if (parent.getEnergy() >= wave.bulletPower && parent.getGunHeat() == 0) {
            parent.setFire(wave.bulletPower);
            parent.addCustomEvent(wave);
            shots++;
        }
    }

    public void onBulletHit(BulletHitEvent e) {
        hits++;
    }

    public void onRoundEnded(RoundEndedEvent e) {
        System.out.println("Shots: " + shots + " Hits: " + hits + " Accuracy: " + (double) hits / shots);
    }

    // Custom events ============================================================
    public void onEnergyChange(double deltaEnergy, int reason) {}
}