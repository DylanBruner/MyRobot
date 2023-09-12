package dylanbruner.move;

import java.util.Vector;

import robocode.AdvancedRobot;
import robocode.Event;
import robocode.ScannedRobotEvent;

public class EasyTester {
    private AdvancedRobot parent;
    public void setParent(AdvancedRobot parent){this.parent = parent;}

    public void onScannedRobot(ScannedRobotEvent e){System.out.println("Scanned Robot");}

    public void setAdjustGunForRobotTurn(boolean b) {this.parent.setAdjustGunForRobotTurn(b);}
    public void setAdjustRadarForGunTurn(boolean b) {this.parent.setAdjustRadarForGunTurn(b);}
    public void setTurnRadarLeftRadians(double d) {this.parent.setTurnRadarLeftRadians(d);}
    public void setTurnRadarRightRadians(double d) {this.parent.setTurnRadarRightRadians(d);}
    public double getHeadingRadians() {return this.parent.getHeadingRadians();}
    public double getRadarHeadingRadians() {return this.parent.getRadarHeadingRadians();}
    public double getX() {return this.parent.getX();}
    public double getY() {return this.parent.getY();}
    public long getTime() {return this.parent.getTime();}
    public void setTurnGunRightRadians(double d) {this.parent.setTurnGunRightRadians(d);}
    public void setFire(double d) {this.parent.setFire(d);}
    public void setAhead(double d) {this.parent.setAhead(d);}
    public void setTurnRightRadians(double d) {this.parent.setTurnRightRadians(d);}
    public void turnRadarRightRadians(double d) {this.parent.turnRadarRightRadians(d);}
    public void setTurnGunLeftRadians(double d) {this.parent.setTurnGunLeftRadians(d);}
    public double getGunHeadingRadians() {return this.parent.getGunHeadingRadians();}
    public double getVelocity() {return this.parent.getVelocity();}
    public void setColors(java.awt.Color c1, java.awt.Color c2, java.awt.Color c3) {this.parent.setColors(c1, c2, c3);}
    public double getBattleFieldHeight() {return this.parent.getBattleFieldHeight();}
    public double getBattleFieldWidth() {return this.parent.getBattleFieldWidth();}
    public int getRoundNum() {return this.parent.getRoundNum();}
    public void setAdjustRadarForRobotTurn(boolean b) {this.parent.setAdjustRadarForRobotTurn(b);}
    public double getRadarTurnRemainingRadians() {return this.parent.getRadarTurnRemainingRadians();}
    public double getRadarTurnRemaining() {return this.parent.getRadarTurnRemaining();}
    public int getOthers() {return this.parent.getOthers();}
    public void execute() {this.parent.execute();}
    public double getGunCoolingRate() {return this.parent.getGunCoolingRate();}
    public int getNumRounds() {return this.parent.getNumRounds();}
    public Vector<Event> getAllEvents() {return this.parent.getAllEvents();}
}