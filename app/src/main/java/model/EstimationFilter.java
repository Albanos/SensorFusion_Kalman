package model;


import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import geodesy.Ellipsoid;
import geodesy.GeodeticCalculator;
import geodesy.GeodeticMeasurement;
import geodesy.GlobalCoordinates;
import geodesy.GlobalPosition;


/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter {
    private static EstimationFilter instance;

    private double latitude;
    private double longitude;
    private double altitude;

    private float linAccel_x;
    private float linAccel_y;

    // Initial Geschwindigkeit in die Richtungen x,y
    private float[] init_linVeloc = {0,0};


    private float[] linVeloc = new float[2];

    private ProcessModel pm;
    private MeasurementModel mm;
    private KalmanFilter kf;
    private RealVector u;
    private RealVector x;
    private RealMatrix A;
    private RealMatrix B;
    private RealMatrix H;
    private RealMatrix Q;
    private RealMatrix R;

    private EstimationFilter(){
        initFilter();
    }

    // Initialisiere den Kalman-Filter
    private void initFilter(){
        // discrete time interval
        double dt = 0.1d;
        // position measurement noise (meter)
        double measurementNoise = 10d;
        // acceleration noise (meter/sec^2)
        double accelNoise = 0.2d;

        x = new ArrayRealVector(new double[] {0,0,0,0,0,0});
//        RealVector x = new ArrayRealVector(6);
//        x.setEntry(0, 0);
//        x.setEntry(1, 0);
//        x.setEntry(2, 0);
//        x.setEntry(3, 0);
//        x.setEntry(4, 0);
//        x.setEntry(5, 0);



        A = new BlockRealMatrix(6,6);
        A.setRow(0,new double[] {1 , 0, dt, 0, 0.5* Math.pow(dt,2),0});
        A.setRow(1,new double[] {0 , 1, 0, dt, 0, 0.5* Math.pow(dt,2)});
        A.setRow(2,new double[] {0 , 0, 1, 0, dt, 0});
        A.setRow(3,new double[] {0 , 0 , 0 , 1 , 0 , dt});
        A.setRow(4,new double[] {0 , 0 , 0 , 0 , 1 , 0});
        A.setRow(5,new double[] {0 , 0 , 0 , 0, 0 , 1});

        B = null;

        H = new BlockRealMatrix(2,6);
        H.setRow(0,new double[] {1 , 1 , 0 , 0 , 0 , 0});
        H.setRow(1,new double[] {0 , 0 , 0 , 0 , 1 , 1});

        RealMatrix G0 = new BlockRealMatrix(6,1);
        G0.setRow(0, new double [] {0.5*Math.pow(dt,2)});
        G0.setRow(1, new double [] {0.5*Math.pow(dt,2)});
        G0.setRow(2, new double [] {dt});
        G0.setRow(3, new double [] {dt});
        G0.setRow(4, new double [] {1});
        G0.setRow(5, new double [] {1});

        RealMatrix G1;
        G1 = G0.transpose().copy();

        RealMatrix G2;
        G2 = G0.multiply(G1);

        Q = G2.scalarMultiply(Math.pow(accelNoise,2));

        // Kovarianz, für Sicherheit des Anfangszustands
        // Ist man sich über diesen sicher, dann kleine Werte, sonst große (1 Mill oder größer). Hier: 100.000
        RealMatrix P0 = new BlockRealMatrix(6,6);
        P0.setColumn(0,new double[] {100000,0,0,0,0,0});
        P0.setColumn(1,new double[] {0,100000,0,0,0,0});
        P0.setColumn(2,new double[] {0,0,100000,0,0,0});
        P0.setColumn(3,new double[] {0,0,0,100000,0,0});
        P0.setColumn(4,new double[] {0,0,0,0,100000,0});
        P0.setColumn(5,new double[] {0,0,0,0,0,100000});

        // Muss gemäß Gleichung: zk = H * xk + R eine 2x1-Matrix sein
        R = new Array2DRowRealMatrix(new double[] {10,0});


        u = new ArrayRealVector(6);
        u.setEntry(0,latitude);
        u.setEntry(1,longitude);
        //u.setEntry(2,linVeloc_x);
        //u.setEntry(3,linVeloc_y);
        u.setEntry(4,linAccel_x);
        u.setEntry(5,linAccel_y);

        pm = new DefaultProcessModel(A,B,Q,x,P0);
        mm = new DefaultMeasurementModel(H,R);
        kf = new KalmanFilter(pm,mm);

    }

    public static synchronized EstimationFilter getInstance(){
        if(instance == null){
            instance = new EstimationFilter();
        }
        return instance;
    }

    // Berechnung der Geschwindigkeit, auf Basis der jeweilligen Beschleunigung (linear)
    private void calculateLinearVelocity(float accelerometer_x, float accelerometer_y) {
        // Zeitänderung = 1 sek
        float dt =  0.1f;

        // Geschwindigkeit berechnen
        // Berechnung von linearer Geschwindigkeit, siehe wikipedia: "Gleichmässig beschleunigte Bew."
        linVeloc[0] = init_linVeloc[0] + (accelerometer_x * dt);
        linVeloc[1] = init_linVeloc[1] + (accelerometer_y * dt);

        // Neue "Ausgangsgeschwindigkeit" setzen
        init_linVeloc[0] = linVeloc[0];
        init_linVeloc[1] = linVeloc[1];
    }

    public void setLinAccelerometerValues(float x, float y){
        this.linAccel_x = x;
        this.linAccel_y = y;

        calculateLinearVelocity(linAccel_x,linAccel_y);
    }

    public void setPositionValues(double latitude, double longitude, double altitude){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public float[] getLinVeloc() {
        return linVeloc;
    }

    public GlobalPosition calculateCoordinatesOnLatLon(double lat, double lon, double alt){
        if(lat != 0 && lon != 0){
            return new GlobalPosition(lat,lon, alt);
        }
        return null;
    }

    public double coordinateDistanceBetweenTwoPoints(GlobalPosition g1, GlobalPosition g2){
        if(g1 != null && g2 != null){
            GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

            GeodeticMeasurement gm = geodeticCalculator
                    .calculateGeodeticMeasurement(Ellipsoid.WGS84, g1, g2);

            return gm.getEllipsoidalDistance();
        }
        return 0;
    }

    public double coordinateAngleBetweenTwoPoints(GlobalPosition g1, GlobalPosition g2){
        if(g1 != null && g2 != null){
            GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

            GeodeticMeasurement gm = geodeticCalculator
                    .calculateGeodeticMeasurement(Ellipsoid.WGS84, g1, g2);

            return gm.getAzimuth();
        }
        return 0;
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
}
