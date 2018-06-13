package model;

import com.instacart.library.truetime.TrueTimeRx;

public class Coordinates {
    private double latitude;
    private double longitude;
    private double altitude;
    private long timestamp;

    public Coordinates(double latitude, double longitude, double altitude){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
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

    public Coordinates(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
