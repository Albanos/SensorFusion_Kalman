package model;

import java.sql.Timestamp;

/**
 * Created by Luan Hajzeraj on 18.03.2018.
 */

public class Pair {
    private double x;
    private double y;
    private Timestamp timestamp;

    public Pair(double x, double y){
        this.x = x;
        this.y = y;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
