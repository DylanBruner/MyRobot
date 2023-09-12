package dylanbruner.utils;

public class RollingAverage {
    private static final int MAX_HISTORY = 100;
    private int[] history;
    private int currentIndex;
    private int sum;

    public RollingAverage() {
        history = new int[MAX_HISTORY];
        currentIndex = 0;
        sum = 0;
    }

    public void addValue(int value) {
        sum -= history[currentIndex];
        history[currentIndex] = value;
        sum += value;
        currentIndex = (currentIndex + 1) % MAX_HISTORY;
    }

    public double getAverage() {
        if (currentIndex == 0) {
            return sum / (double) MAX_HISTORY;
        } else {
            return sum / (double) currentIndex;
        }
    }
}