package dylanbruner.gun;

import java.util.Hashtable;

import dylanbruner.MyRobot;
import robocode.BulletHitEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class PatternGun extends Gun {
    static StringBuffer history_base = new StringBuffer("00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
    static Hashtable<String, StringBuffer> patterns = new Hashtable<String, StringBuffer>();
    static final int PATTERN_DEPTH = 60;

    static int shots = 0;
    static int hits = 0;

    public PatternGun(MyRobot parent) {
        super(parent);
    }
    
    public void onScannedRobot(ScannedRobotEvent e){
        double bulletPower = getGunPower(e);
        double bulletVelocity = 20 - bulletPower * 3;
        double distance = e.getDistance();
        double absBearing = parent.getHeadingRadians() + e.getBearingRadians();

        if (!patterns.containsKey(e.getName())){
            patterns.put(e.getName(), new StringBuffer(history_base));
        }
        StringBuffer history = patterns.get(e.getName());

        int matchLength = PATTERN_DEPTH;
        history.insert(0, (char)(int)(Math.sin(e.getHeadingRadians() - absBearing) * e.getVelocity()));

        patterns.put(e.getName(), history);

        int index;
        while((index = history.toString().indexOf(history.substring(0, matchLength--), 1)) < 0);

        matchLength = index - (int)(distance / bulletVelocity);
        while (index >= Math.max(0, matchLength)){
            absBearing += Math.asin((double)(byte)history.charAt(index--) / distance);
        }

        // turn the gun
        parent.setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - parent.getGunHeadingRadians()));
        // if the gun is cool and we're pointed at the target, shoot!
        if (parent.getGunHeat() == 0 ){
            parent.setFire(bulletPower);
            shots++;
        }
    }

    public void onBulletHit(BulletHitEvent e){
        hits++;
    }

    public void onRoundEnded(RoundEndedEvent e){
        System.out.println("Shots: " + shots + " Hits: " + hits + " Accuracy: " + (double) hits / shots);
    }
}