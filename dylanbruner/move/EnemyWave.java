package dylanbruner.move;

import java.awt.geom.Point2D;
import java.util.ArrayList;

class EnemyWave {
    Point2D.Double fireLocation;
    long fireTime;
    double bulletVelocity, directAngle, distanceTraveled;
    int direction;
    ArrayList<Point2D.Double> safePoints;

    boolean isInvalid = false;
}
