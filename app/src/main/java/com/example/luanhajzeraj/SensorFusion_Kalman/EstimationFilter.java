package com.example.luanhajzeraj.SensorFusion_Kalman;


import android.util.Log;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import model.Pair;

/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter{
    private static EstimationFilter instance;

    private KalmanFilter filter;
    // Frage nach dt... Erstmal statisch festlegen, mit 0.1
    final double dt = 0.1d;
    //final double dt = 1d;

    // Statische Variablen, von der doku-page
    // position measurement noise (meter)
    double measurementNoise = 10d;
    // acceleration noise (meter/sec^2)
    double accelNoise = 0.2d;

    // Vektoren und Matrizen (nach Notation von Apache Math):
    // x: Zustandsvektor
    // u: Eingabevektor
    // A: Transitionsmatrix
    // B: Eingabematrix
    // Q: Prozessrauschkovarianz
    // R: Messrauschkovarianz
    // P: Kovarianz
    // H: Messmatrix
    private RealVector x;
    private RealVector u;
    private RealMatrix A;
    private RealMatrix B;
    private RealMatrix Q;
    private RealMatrix R;
    private RealMatrix P;
    private RealMatrix H;

    public EstimationFilter() {
        float locationAccurancy = Service.getLocationAccurancy();
        // Standardabweichung der Beschleunigung (statisch festgelegt), für Prozessrauschen
        final double sigmaAccel = 1f;

        double coordinate_x = Service.getFirstCartasianPoint().getX();
        double coordinate_y = Service.getFirstCartasianPoint().getY();

        double speed_x = 0;
        double speed_y = 0;
        float accel_x = Service.getAccel_x_wgs();
        float accel_y = Service.getAccel_y_wgs();

        x = new ArrayRealVector(new double[]{coordinate_x, coordinate_y, speed_x, speed_y});
        u = new ArrayRealVector(new double[]{accel_x, accel_y});
        A = new Array2DRowRealMatrix(new double[][]{
                {1, 0, dt, 0},
                {0, 1, 0, dt},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });

        B = new Array2DRowRealMatrix(new double[][]{
                {(Math.pow(dt, 2) / 2), 0},
                {0, (Math.pow(dt, 2) / 2)},
                {dt, 0},
                {0, dt}
        });

        H = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0}
        });

        // Standardabweichung für Beschleunigung ist statisch 1, deshalb ignoriert
        Q = new Array2DRowRealMatrix(new double[][]{
                {1 / 4 * Math.pow(dt, 4), 1 / 4 * Math.pow(dt, 4), 1 / 2 * Math.pow(dt, 3), 1 / 2 * Math.pow(dt, 3)},
                {1 / 4 * Math.pow(dt, 4), 1 / 4 * Math.pow(dt, 4), 1 / 2 * Math.pow(dt, 3), 1 / 2 * Math.pow(dt, 3)},
                {1 / 2 * Math.pow(dt, 3), 1 / 2 * Math.pow(dt, 3), Math.pow(dt, 2), Math.pow(dt, 2)},
                {1 / 2 * Math.pow(dt, 3), 1 / 2 * Math.pow(dt, 3), Math.pow(dt, 2), Math.pow(dt, 2)}
        });


        double locationVarianz = Math.pow(locationAccurancy, 2);
        R = new Array2DRowRealMatrix(new double[][]{
                {locationVarianz, 0},
                {0, locationVarianz}
        });

        P = new Array2DRowRealMatrix(new double[][]{
                {10, 0, 0, 0},
                {0, 10, 0, 0},
                {0, 0, 10, 0},
                {0, 0, 0, 10}
        });

        //updateMeasuerement();
        ProcessModel pm = new DefaultProcessModel(A, B, Q, x, P);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        filter = new KalmanFilter(pm, mm);

    }

    private void updateMeasuerement() {
        // Locationgenauigkeit für Messrauschkovarianz
        //float locationAccurancy = Service.getLocationAccurancy();
        // Standardabweichung der Beschleunigung (statisch festgelegt), für Prozessrauschen
        //final double sigmaAccel = 1f;

        //double coordinate_x = Service.getFirstCartasianPoint().getX();
        //double coordinate_y = Service.getFirstCartasianPoint().getY();

        //double speed_x = 0;
        //double speed_y = 0;
        float accel_x = Service.getAccel_x_wgs();
        float accel_y = Service.getAccel_y_wgs();

    }

    public void makeEstimation() {

        // Zwei Vektoren von der Doku-seite: tmpNoise wurde um zwei erweitert, da wir das rauschen
        // eben in zwei Dimensionen haben
        RealVector tmpPNoise = new ArrayRealVector(new double[]
                {Math.pow(dt, 2d) / 2d, dt, Math.pow(dt, 2d) / 2d, dt});

        RealVector mNoise = new ArrayRealVector(2);

        //for(int i =0; i < 300; i++) {
        int i =0;
        for (; ; ) {
            if(Service.getThread().isInterrupted()){
                break;
            }
            //updateMeasuerement();
//            float accel_x = Service.getAccel_x_wgs();
//            float accel_y = Service.getAccel_y_wgs();
//
//            u.setEntry(0,accel_x);
//            u.setEntry(1,accel_y);

            filter.predict(u);

            // simulate the process (von doku-page)
//            RealVector pNoise = tmpPNoise.mapMultiply(accelNoise * rand.nextGaussian());
            RealVector pNoise = tmpPNoise.mapMultiply(accelNoise);

            // x = A * x + B * u + pNoise --> rauschvariante wurde von der doku-page übernommen
            x = A.operate(x).add(B.operate(u)).add(pNoise);

            // simulate the measurement (von doku-page)
            mNoise.setEntry(0, measurementNoise * Service.getAccel_x_wgs());
            mNoise.setEntry(1, measurementNoise * Service.getAccel_y_wgs());


            // z = H * x + m_Noise
            RealVector z = H.operate(x).add(mNoise);

            filter.correct(z);

            double estimatedPosition_x = filter.getStateEstimation()[0];
            double estimatedPosition_y = filter.getStateEstimation()[1];

            Log.d("HI", "Punkte:  " + estimatedPosition_x + " ; " + estimatedPosition_y);

            // Füge die geschätzten Punkte der Liste hinzu, um sie später zeichnen zu können
            Service.getEstimatedPoints().add(new Pair(estimatedPosition_x, estimatedPosition_y));

            // Füge die geschätzte Geschwindigkeit der liste hinzu
            Service.getEstimatedVelocity().add(new Pair(filter.getStateEstimation()[2], filter.getStateEstimation()[3]));

        }
//    double estimatedPosition_x = filter.getStateEstimation()[0];
//    double estimatedPosition_y = filter.getStateEstimation()[1];
//
//    // Füge die geschätzten Punkte der Liste hinzu, um sie später zeichnen zu können
//    Service.getEstimatedPoints().add(new Pair(estimatedPosition_x,estimatedPosition_y));


//        ArrayList<Double> returnList = new ArrayList<>();
//        returnList.add(position);
//        returnList.add(velocity);
//
//        return returnList;
    }

    public static synchronized EstimationFilter getInstance() {
        if (instance == null) {
            instance = new EstimationFilter();
        }
        return instance;
    }
}
