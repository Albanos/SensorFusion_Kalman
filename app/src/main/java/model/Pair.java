package model;

import com.instacart.library.truetime.TrueTimeRx;

/**
 * Created by Luan Hajzeraj on 18.03.2018.
 */

public class Pair {
    private double x;
    private double y;
    //private Timestamp timestamp;
    private long timestamp;

    public Pair(double x, double y) {
        this.x = x;
        this.y = y;
        //this.timestamp = new Timestamp(System.currentTimeMillis());
        //this.timestamp = System.currentTimeMillis();
        this.timestamp = calculateCurrentRealTime();
    }

    /**
     * Gibt die Google-Server-Zeit zurück (initialisierung in MainActivity.java): Für Messungen
     * wird eine syncrone Zeit benötigt
     *
     * @return
     */
    private long calculateCurrentRealTime() {
        return TrueTimeRx.now().getTime();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
