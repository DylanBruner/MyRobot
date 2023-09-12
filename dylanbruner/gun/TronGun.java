package dylanbruner.gun;

import robocode.*;
import java.awt.geom.*;

/**
 * DCGun_1on1 - a gun by ABC
 * A (slightly) simplified version of Shadow/Tron Gun
 * No guessFactors, no stats bins
 */
public class TronGun extends Gun {
    private AdvancedRobot bot;
    Point2D enemyLocation;
    double enemyEnergy, enemyDistance;
    boolean aiming;
    public int logLevel = 2;
    public double minBulletPower = .1, maxBulletPower = 3;
    boolean referenceMode = false;
    protected long lastAimTime = 0;

    public long bulletsFired = 0, bulletsHit = 0;
    public double powerFired = 0, powerHit = 0;

    private int maxSize = 30000;// max log size

    private int centerHitDist = 0;
    private double toleranceWidth = 20;// hit tolerance

    public ScanInfo first, last;// doubly linked list of scans
    public int size = 0;

    private double lastDir = 1;
    private long lastRunStart = 0;
    private double lastRunTime = 0;
    private double lastVel;

    long startTime = 0;
    double bulletPower = 3;
    private long lastTime = 0;
    private double battleFieldWidth, battleFieldHeight;

    private double tolerance = 0;
    private double nextX = 0, nextY = 0, headOnAngle;

    private static int shots = 0;
    private static int hits = 0;

    public TronGun(AdvancedRobot b) {
        super(b);
        bot = b;
        battleFieldWidth = bot.getBattleFieldWidth();
        battleFieldHeight = bot.getBattleFieldHeight();
    }

    public void initRound() {
        enemyLocation = null;
        aiming = false;
    }

    public void bulletFired(Bullet b) {
        bulletsFired++;
        powerFired += b.getPower();
    }

    public void cleanUpRound() {
        enemyLocation = null;
        // bot.out.println("missed scans: " + missedScans);
        // bot.out.println("PMLog size=" + size);
        if (size == 0)
            return;
        while (size > maxSize) {
            first = first.next;
            size--;
        }
        first.previous = null;

        if (logLevel > 1) {
            bot.out.println("Bullets fired/hit: " + bulletsFired + "/" + bulletsHit
                    + " (" + Math.round(bulletsHit * 1000D / bulletsFired) / 10D + "%)");
            bot.out.println("Power fired/hit: " + (int) powerFired + "/" + (int) powerHit
                    + " (" + Math.round(powerHit * 1000D / powerFired) / 10D + "%)");
        }
    }

    public double calcBulletPower() {
        double bulletPower = 0;
        if (enemyLocation == null)
            return (0);
        bulletPower = enemyDistance > 150 ? 1.9 : 3;
        bulletPower = Math.min(bulletPower, (enemyEnergy + .1) / 4);
        if (bulletPower * 6 >= bot.getEnergy())
            bulletPower = bot.getEnergy() / 6;
        if (bulletPower >= bot.getEnergy() - .1)
            bulletPower = bot.getEnergy() - .1;
        bulletPower = Math.max(minBulletPower, Math.min(maxBulletPower, bulletPower));
        if (referenceMode)
            bulletPower = Math.min(bot.getEnergy(), 3);
        return (bulletPower);
    }

    public void execute() {
        Bullet b = null;
        if (enemyLocation == null) {
            bot.setTurnGunRight(bot.getRadarTurnRemaining());
            if (bot.getOthers() == 0)
                bot.setFire(.1);
        } else {
            // compute my position in the next tick
            double nextTurn;
            if (bot.getTurnRemaining() >= 0)
                nextTurn = Math.min(bot.getTurnRemaining(), 10 - .75 * Math.abs(bot.getVelocity()));
            else
                nextTurn = Math.max(bot.getTurnRemaining(), -10 + .75 * Math.abs(bot.getVelocity()));
            nextX = bot.getX() + bot.getVelocity() * BotUtils.sinD(nextTurn);
            nextY = bot.getY() + bot.getVelocity() * BotUtils.cosD(nextTurn);
            headOnAngle = BotUtils.angleTo(nextX, nextY, enemyLocation.getX(), enemyLocation.getY());
            //
            bulletPower = calcBulletPower();
            if (bot.getGunHeat() > bot.getGunCoolingRate() || bot.getEnergy() < bulletPower || bulletPower == 0) {
                aiming = false;
                bot.setTurnGunRight(BotUtils.normalizeBearing(headOnAngle - bot.getGunHeading()));
            } else {
                if (aiming && bot.getGunTurnRemaining() == 0 && bot.getGunHeat() == 0) {
                    b = bot.setFireBullet(bulletPower);
                    shots++;
                    aiming = false;
                } else if (!aiming) {
                    lastAimTime = bot.getTime();
                    double fireAngle = findBestAngle();
                    if (fireAngle != 10000) {
                        bot.setTurnGunRight(BotUtils.normalizeBearing(fireAngle - bot.getGunHeading()));
                        aiming = true;
                    } else {// gun says: don't fire!
                        bot.setTurnGunRight(BotUtils.normalizeBearing(headOnAngle - bot.getGunHeading()));
                    }
                }
            }
        }
        if (b != null) {
            bulletFired(b);
        }
    }

    public void onBulletHit(BulletHitEvent e) {
        bulletsHit++;
        hits++;
        System.out.println("Hit rate: " + (double) hits / shots);
        powerHit += e.getBullet().getPower();
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (referenceMode && bot.getEnergy() <= .1)
            return;
        long t1 = bot.getTime() - startTime;
        if (t1 - lastTime > 20 || t1 < lastTime) {// new round or 20+ missed scans
            startTime = bot.getTime();
            lastRunStart = startTime;
        }
        t1 = bot.getTime() - startTime;
        double dist = e.getDistance();
        double velocity = e.getVelocity();
        double heading = (bot.getHeading() + e.getBearing()) % 360;
        if (heading < 0)
            heading += 360;
        double x = bot.getX() + dist * BotUtils.sinD(heading);
        double y = bot.getY() + dist * BotUtils.cosD(heading);
        enemyLocation = new Point2D.Double(x, y);
        enemyEnergy = e.getEnergy();
        enemyDistance = dist;

        lastDir = velocity != 0 ? velocity : lastDir;
        if (velocity != lastVel) {
            lastRunTime = Math.min(40.0, (double) t1 - lastRunStart) / 40;
            lastRunStart = t1;
        }
        // if (velocity * lastDir < 0 || velocity == 0) lastRunStart = t1;
        heading = lastDir < 0 ? (e.getHeading() + 180) % 360 : e.getHeading();
        ScanInfo currInfo = new ScanInfo(x, y, heading, (Math.abs(velocity) / 8), t1);
        // currInfo.dtm = Math.min(e.getDistance() / (20 - bulletPower * 3), 70.0) / 70;
        currInfo.dtm = Math.min(e.getDistance(), 800) / 800;
        currInfo.runTime = Math.min(40.0, (double) (t1 - lastRunStart)) / 40.0;// 40
        currInfo.lastRunTime = lastRunTime;
        currInfo.myGunHeat = Math.min(1.5, bot.getGunHeat()) / 1.5;
        if (Math.abs(lastVel) > Math.abs(velocity))
            currInfo.acc = 1;
        else if (Math.abs(lastVel) < Math.abs(velocity))
            currInfo.acc = -1;
        lastVel = velocity;
        currInfo.atm = Math.abs(BotUtils.normalizeBearing(heading - bot.getHeading() - e.getBearing())) / 180;

        // wallDanger
        double maxWDist = 400;
        double distV = 0, distH = 0;
        if (heading == 90 || heading == 270)
            distV = Double.POSITIVE_INFINITY;
        else if (heading < 90 || heading > 270)
            distV = (battleFieldHeight - y) / BotUtils.cosD(heading);
        else
            distV = y / BotUtils.cosD(heading - 180);
        if (heading == 180 || heading == 0)
            distH = Double.POSITIVE_INFINITY;
        else if (heading < 180)
            distH = (battleFieldWidth - x) / BotUtils.cosD(heading - 90);
        else
            distH = x / BotUtils.cosD(heading - 180 - 90);
        currInfo.dtwf = 1 - (Math.min(Math.min(distV, distH), maxWDist) / maxWDist);

        double h1 = (heading + 180) % 360;
        if (h1 < 0)
            h1 += 360;
        if (h1 == 90 || h1 == 270)
            distV = Double.POSITIVE_INFINITY;
        else if (h1 < 90 || h1 > 270)
            distV = (battleFieldHeight - y) / BotUtils.cosD(h1);
        else
            distV = y / BotUtils.cosD(h1 - 180);
        if (h1 == 180 || h1 == 0)
            distH = Double.POSITIVE_INFINITY;
        else if (h1 < 180)
            distH = (battleFieldWidth - x) / BotUtils.cosD(h1 - 90);
        else
            distH = x / BotUtils.cosD(h1 - 180 - 90);
        currInfo.dtwb = 1 - (Math.min(Math.min(distV, distH), maxWDist) / maxWDist);
        // --

        if (size == 0) {
            first = currInfo;
            last = currInfo;
        } else {
            last.next = currInfo;
            currInfo.previous = last;
            last = currInfo;
        }
        size++;
        lastTime = t1;
    }

    public double findBestAngle() {
        // The Guns main method:
        // 1.Finds the topCount scans closest form the current (last) one
        // 2.Computes the corresponding firing angles
        // 3.Chooses the one that hits in most of the cases

        int topCount = 50, angMax = 50;// nr of closest matches

        boolean firedOnlyStats = (bot.getRoundNum() > 0 && (double) bulletsHit / bulletsFired > .12);
        if (firedOnlyStats) {// anti-Cigaret
            topCount = 250;
            angMax = 250;
        }

        ScanInfo info = last;
        info.fired = true;
        ScanInfo currentInfo = info;
        double currentDistance = 0;
        boolean newRound = true;
        ScanInfo[] topInfo = new ScanInfo[topCount];
        double topDiff[] = new double[topCount];
        double bestAngle = 0;

        if (last == null)
            return (headOnAngle);

        long t1 = info.t;

        for (int i = 0; i < topCount; i++) {
            topDiff[i] = Double.POSITIVE_INFINITY;
            topInfo[i] = currentInfo;
        }

        while (info.previous != null) {
            if (newRound) {
                t1 = info.t;
                if (info.t <= t1)
                    newRound = false;
            } else {
                currentDistance = 0;

                currentDistance += sqr((currentInfo.dtm - info.dtm)) * 4;
                currentDistance += sqr((currentInfo.atm - info.atm));
                currentDistance += sqr((currentInfo.v - info.v)) * 2;
                currentDistance += sqr((currentInfo.dtwf - info.dtwf)) * 4;
                currentDistance += sqr((currentInfo.dtwb - info.dtwb));
                currentDistance += sqr((currentInfo.runTime - info.runTime));
                currentDistance += sqr((currentInfo.lastRunTime - info.lastRunTime));
                currentDistance += sqr((currentInfo.acc - info.acc) / 2);
                // the following line makes it score over 90%+ against Cigaret 1.31TC, but worse
                // against others (wavesurfers in particular)
                if (firedOnlyStats)
                    currentDistance += sqr((currentInfo.myGunHeat - info.myGunHeat)) * 100;

                int i = topCount - 1;
                while (i >= 0 && currentDistance < topDiff[i])
                    i--;
                if (i < topCount - 1) {
                    i++;
                    for (int j = topCount - 2; j >= i; j--) {
                        topDiff[j + 1] = topDiff[j];
                        topInfo[j + 1] = topInfo[j];
                    }
                    topDiff[i] = currentDistance;
                    topInfo[i] = info;
                }

                info = info.previous;
                if (info.t > t1)
                    newRound = true;
            }
        }

        double dists[] = new double[topCount];
        for (int i = 0; i < topCount; i++)
            dists[i] = topDiff[i];

        double[][] angles = new double[topCount][4];
        for (int i = 0; i < topCount; i++)
            angles[i][0] = 1000;
        int angCount = 0;
        for (int i = 0; i < topCount && angCount < angMax; i++) {
            double ang = getGunAngle(topInfo[i]);
            if (ang < 1000) {
                ang = BotUtils.normalizeBearing(ang - headOnAngle);
                angles[angCount][0] = ang;
                angles[angCount][1] = tolerance;
                angles[angCount][2] = 1;// topDiff[i];
                angles[angCount][3] = 1;// topDiff[i];
                for (int j = 0; j < angCount; j++) {
                    if (angles[j][0] < 1000) {
                        if (Math.abs(angles[j][0] - angles[angCount][0]) < angles[angCount][1]) {
                            angles[j][2] += angles[angCount][3];// 1;
                        }
                        if (Math.abs(angles[angCount][0] - angles[j][0]) < angles[j][1]) {
                            angles[angCount][2] += angles[j][3];// 1;
                        }
                    }
                }
                angCount++;
            }
        }
        double maxCount = 0;
        int maxIDX = 0;
        for (int j = 0; j < angCount; j++) {
            if (angles[j][2] > maxCount) {
                maxCount = angles[j][2];
                maxIDX = j;
            }
        }

        bestAngle = angles[maxIDX][0];
        if (bestAngle >= 1000)
            bestAngle = 0;

        return (headOnAngle + bestAngle);
    }

    public double getGunAngle(ScanInfo predictedInfo) {
        // traces the enemys path to get a firing angle
        tolerance = 0;
        ScanInfo currInfo = last;
        ScanInfo endInfo = predictedInfo;
        long timeDelta = (bot.getTime() - startTime) - currInfo.t;
        double predx = 0, predy = 0, predDist = 0, predAng;
        double bulletSpeed = (20 - 3 * bulletPower);
        long bulletTime;
        for (int i = 0; i < 50; i++) {
            predDist = BotUtils.distanceTo(endInfo.x, endInfo.y, predictedInfo.x, predictedInfo.y);
            predAng = Math.PI / 2 - Math.atan2(endInfo.y - predictedInfo.y, endInfo.x - predictedInfo.x)
                    - Math.toRadians(predictedInfo.d);
            predx = currInfo.x + predDist * Math.sin(Math.toRadians(currInfo.d) + predAng);
            predy = currInfo.y + predDist * Math.cos(Math.toRadians(currInfo.d) + predAng);
            predDist = BotUtils.distanceTo(predx, predy, nextX, nextY) - centerHitDist;
            bulletTime = (long) (predDist / bulletSpeed) + 1;// <-- +1
            if (Math.abs(endInfo.t - predictedInfo.t - timeDelta - bulletTime) <= 1)
                break;
            endInfo = predictedInfo;
            while (endInfo.next != null && endInfo.t >= predictedInfo.t
                    && (endInfo.t - predictedInfo.t - timeDelta) < bulletTime) {
                endInfo = endInfo.next;
            }
            if (endInfo.next == null || endInfo.t < predictedInfo.t)
                return (Double.POSITIVE_INFINITY);
        }
        if (predx < 0 || predx > battleFieldWidth
                || predy < 0 || predy > battleFieldHeight)
            return (Double.POSITIVE_INFINITY);
        tolerance = Math.toDegrees(/* Math.atan */(toleranceWidth / predDist));
        return (Math.toDegrees(Math.PI / 2 - Math.atan2(predy - nextY, predx - nextX)));
    }

    public class ScanInfo {
        public double x = 0, y = 0, d = 0;
        public long t = 0;
        public double v = 0;
        public double acc = 0;
        public double atm = 0;
        public double dtm = 0;
        public double dtwf = 0;
        public double dtwb = 0;
        public double runTime = 0;
        public double lastRunTime = 0;
        public double myGunHeat = 0;
        public boolean fired = false;
        public ScanInfo previous;
        public ScanInfo next;

        public ScanInfo(double x1, double y1, double d1, double v1, long t1) {
            x = x1;
            y = y1;
            d = d1;
            v = v1;
            t = t1;
        }
    }

    private static final double sqr(double x) {
        return (x * x);
    }
}