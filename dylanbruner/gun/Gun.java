package dylanbruner.gun;

import dylanbruner.MyRobot;
import dylanbruner.gen.Watcher;
import dylanbruner.gen.Watcher.TrackedRobot;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;

public class Gun {
    MyRobot parent;

    public Gun(MyRobot parent) {this.parent = parent;}
    public Gun(AdvancedRobot parent) {this.parent = (MyRobot) parent;}

    public void onScannedRobot(ScannedRobotEvent e) {}
    public void onBulletHit(BulletHitEvent e) {}
    public void onBulletHitBullet(BulletHitBulletEvent e) {}
    public void onRoundEnded(RoundEndedEvent e) {}

    public double getGunPower(ScannedRobotEvent e) {
        double bulletPower = (e.getDistance() < 100) ? 2.95 : 1.95;
        bulletPower = Math.min(bulletPower, e.getEnergy() / 4); // Only use amount of energy required
        if (parent.getEnergy() < 20) {
            bulletPower = Math.min(bulletPower, 1);
        }

        TrackedRobot robot = Watcher.getRobot(e.getName());

        // Attempt to undercut their bullets
        // if (robot != null) {
            // if (robot.getLastFirePower() >= 0.1) {
                // bulletPower = Math.max(Math.min(bulletPower, robot.getLastFirePower() - 0.1), 0.1);
            // }
        // }

        // Power = 0.1 if they're shielding
        if (robot != null) {
            if (robot.isShielding()) {
                bulletPower = 0.1;
            }
        }

        return Math.min(Math.max(bulletPower, .1), 3);
    }
}
