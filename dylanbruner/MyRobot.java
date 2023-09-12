package dylanbruner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dylanbruner.gun.PatternGun;
import dylanbruner.gun.GFGun.GunTuner;
import dylanbruner.gun.GFGun.MainGun;
import dylanbruner.gun.Gun;
import dylanbruner.move.Surfer;
import dylanbruner.gen.Watcher;

import robocode.*;
import robocode.util.Utils;

public class MyRobot extends AdvancedRobot {
    static final String RED = "\u001B[31m";
    static final String GREEN = "\u001B[32m";
    static final String RESET = "\u001B[0m";

    static final boolean GUN_TUNING_MODE = false;

    static Gun gun;
    static GunTuner tuner;
    static Surfer surfer;
    Watcher tracker;

    public static void main(String[] args) {
        try {
            Process proc = new ProcessBuilder("python", "compile.py").start();
            new Thread(() -> new BufferedReader(new InputStreamReader(proc.getInputStream()))
                    .lines().forEach(System.out::println)).start();
            new Thread(() -> new BufferedReader(new InputStreamReader(proc.getErrorStream()))
                    .lines().forEach(System.err::println)).start();
            int exitCode = proc.waitFor();
            System.out.println(
                    exitCode == 0 ? GREEN + "Compilation successful" + RESET
                            : RED + "Error compiling Python code. Exit code: " + exitCode + RESET);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error running compilation process: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        setTurnRadarLeftRadians(Double.POSITIVE_INFINITY);

        if (gun == null && !GUN_TUNING_MODE) {gun = new MainGun(this);}
        if (GUN_TUNING_MODE && tuner == null){tuner = new GunTuner(this);}
        if (surfer == null) {surfer = new Surfer(this);}
        tracker = new Watcher(this);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        // // Radar lock
        double radarTurn = getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));

        tracker.onScannedRobot(e);
        if (GUN_TUNING_MODE) {
            tuner.onScannedRobot(e);
        } else {
            gun.onScannedRobot(e);
        }

        surfer.onScannedRobot(e);
    }

    public void onBulletHit(BulletHitEvent e) {
        tracker.onBulletHit(e);
        if (GUN_TUNING_MODE) {
            tuner.onBulletHit(e);
        } else {
            gun.onBulletHit(e);
        }
    }

    public void onBulletHitBullet(BulletHitBulletEvent e) {
        tracker.onBulletHitBullet(e);
        if (!GUN_TUNING_MODE) {
            gun.onBulletHitBullet(e);
        }
    }

    public void onRoundEnded(RoundEndedEvent e) {
        if (GUN_TUNING_MODE) {
            tuner.onRoundEnded(e);
        } else {
            gun.onRoundEnded(e);
        }
    }

    public void onBulletMissed(BulletMissedEvent e) {
        if (GUN_TUNING_MODE) {
            tuner.onBulletMissed(e);
        }
    }

    public void onHitWall(HitWallEvent e) {}
    public void onHitByBullet(HitByBulletEvent e) {
        surfer.onHitByBullet(e);
    }
    public void onSkippedTurn(SkippedTurnEvent e) {
    }
    public void onWin(WinEvent e) {
    }

    public void onPaint(java.awt.Graphics2D g) {
        if (GUN_TUNING_MODE) {
            tuner.onPaint(g);
        }
    }
}