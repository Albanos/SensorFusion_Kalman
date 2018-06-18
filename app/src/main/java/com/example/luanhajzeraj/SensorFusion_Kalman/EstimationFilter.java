package com.example.luanhajzeraj.SensorFusion_Kalman;


import android.util.Log;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.MathUtils;

import java.sql.Timestamp;

import model.Pair;

/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter {
    private static EstimationFilter instance;

    private KalmanFilter filter;

    final double dt = Service.getDt();

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
    // z: Messvektor
    private RealVector x;
    private RealVector u;
    private RealMatrix A;
    private RealMatrix B;
    private RealMatrix Q;
    private RealMatrix R;
    private RealMatrix P;
    private RealMatrix H;
    private RealVector z;
    private RealVector currentMeasurment;

    private ProcessModel pm;
    private MeasurementModel mm;

    private static double oldMeasurementX;
    private static double oldMeasurementY;

    // Variable zum steuern der Erzeugung von Punkten, pro sekunde (in ms).
    // Bsp.: TIME_TO_SLEEP=100 --> 10 Punkte/sek --> 10 Hz
    private static final int TIME_TO_SLEEP = 100;
    private static long timestamp = System.currentTimeMillis();

    public EstimationFilter() {
        float locationAccurancy = Service.getLocationAccurancy();
        // Standardabweichung der Beschleunigung (statisch festgelegt), für Prozessrauschen
        final double sigmaAccel = 1f;

        double coordinate_x = Service.getFirstCartasianPoint().getX();
        double coordinate_y = Service.getFirstCartasianPoint().getY();

        double speed_x = Service.getSpeed_x_wgs();
        double speed_y = Service.getSpeed_y_wgs();
        float accel_x = Service.getAccel_x_wgs();
        float accel_y = Service.getAccel_y_wgs();

        x = new ArrayRealVector(new double[]{coordinate_x, coordinate_y, speed_x, speed_y});
        u = new ArrayRealVector(new double[]{accel_x, accel_y});
        //u = new ArrayRealVector(new double[]{coordinate_x, coordinate_y});
        A = new Array2DRowRealMatrix(new double[][]{
                {1, 0, dt, 0},
                {0, 1, 0, dt},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });


//        B = new Array2DRowRealMatrix(new double[][]{
//                {(Math.pow(dt, 2) / 2), 0},
//                {0, (Math.pow(dt, 2) / 2)},
//                {dt, 0},
//                {0, dt}
//        });

        B = new Array2DRowRealMatrix(new double[][]{
                {0, 0},
                {0, 0},
                {dt, 0},
                {0, dt}
        });

//        B = new Array2DRowRealMatrix(new double[][]{
//                {1, 0},
//                {0, 1},
//                {0, 0},
//                {0, 0}
//        });

        H = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0}
        });
//        H = new Array2DRowRealMatrix(new double[][]{
//                {1, 0, 0, 0},
//                {0, 1, 0, 0},
//                {0, 0, 1, 0},
//                {0, 0, 0, 1}
//        });

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
//        double locationVarianz = Math.pow(locationAccurancy, 2);
//        double speedVarianz = Math.pow(5,2); // speedVarianz wird statisch festgelegt, da Geschw.-Genauigkeit nicht verfügbar
//        R = new Array2DRowRealMatrix(new double[][]{
//                {locationVarianz, 0},
//                {0, locationVarianz},
//                {speedVarianz, 0},
//                {0, speedVarianz}
//        });

        P = new Array2DRowRealMatrix(new double[][]{
                {10, 0, 0, 0},
                {0, 10, 0, 0},
                {0, 0, 10, 0},
                {0, 0, 0, 10}
        });

//        z = new ArrayRealVector(new double[]{Service.getListOfPoints().getLast().getX(),
//                                            Service.getListOfPoints().getLast().getY(),
//                                            Service.getSpeed_x_wgs(), Service.getSpeed_y_wgs()});

        z = new ArrayRealVector(new double[]{Service.getListOfPoints().getLast().getX(),
                Service.getListOfPoints().getLast().getY()});

        currentMeasurment = z;

        pm = new DefaultProcessModel(A, B, Q, x, P);
        mm = new DefaultMeasurementModel(H, R);
        filter = new KalmanFilter(pm, mm);

    }

    public void makeEstimation() {

        int i = 1;
        //for (; ; ) {
        while(! Service.getThread().isInterrupted()){

            // LAZY-WAITING:
            // Prüfe, ob TIME_TO_SLEEP vergangen ist oder nicht. Wenn nicht, springe zur nächsten Iteration
            if ((System.currentTimeMillis() - timestamp) < TIME_TO_SLEEP) {
                continue;
            }

            timestamp = System.currentTimeMillis();

            Log.d("HI", "Iteration, Nr.:  " + i++);
            //filter.predict(u);
            filter.predict();


            currentMeasurment = new ArrayRealVector(new double[]{Service.getListOfPoints().getLast().getX(),
                    Service.getListOfPoints().getLast().getY()});

            // Nur wenn eine neue Messung vorliegt, wird ein Korrektur-schritt vorgenommen
            if (!currentMeasurment.equals(z)) {
                filter.correct(currentMeasurment);
                z = currentMeasurment;
                Log.d("HI", "New measurement updated");
            }

            double estimatedPosition_x = filter.getStateEstimation()[0];
            double estimatedPosition_y = filter.getStateEstimation()[1];

            // Enzerre die Punkte, da die Liste leer, wenn der Zeichen-Screen verlassen wird
            // (nur für Log-Ausgabe...)
            double cartesianX = Service.getListOfPoints().size() == 0 ? 0 : Service.getListOfPoints().getLast().getX();
            double cartesianY = Service.getListOfPoints().size() == 0 ? 0 : Service.getListOfPoints().getLast().getY();
            //String timestamp = (String) Service.getListOfPoints().getLast().getTimestamp();

            Log.d("HI", "Geschätzter Punkt:  " + estimatedPosition_x + " ; " + estimatedPosition_y + " Zur Zeit (jetzt):  " + new Timestamp(System.currentTimeMillis()));
            Log.d("HI", "Echter Punkt:  " + cartesianX + " ; " + cartesianY);

            Pair estimatedPoint = new Pair(estimatedPosition_x, estimatedPosition_y);

            // Füge die geschätzten Punkte der Liste hinzu, um sie später zeichnen zu können
            //Service.getEstimatedPoints().add(new Pair(estimatedPosition_x, estimatedPosition_y));
            Service.getEstimatedPoints().add(estimatedPoint);

            // Berechne Lat/Lon nur für die Punkte, die ungleich dem ersten sind
            Pair first = Service.getListOfPoints().getFirst();
            if(estimatedPoint.getX() != first.getX() && estimatedPoint.getY() != first.getY()) {
                // Berechne Winkel und Distanz des geschätzten Punktes, zum Startpunkt (für spätere Rückrichtung in Lat/Lon)
                Service.calculateAngleAndDistanceByPoint(estimatedPoint);

                // Bestimme Lat/Lon von dem berechneten cartesischen Punkt
                Service.calculateWGSCoordinateByCartesianPoint(estimatedPoint);
            }

            // Füge die geschätzte Geschwindigkeit der liste hinzu
            Service.getEstimatedVelocity().add(new Pair(filter.getStateEstimation()[2], filter.getStateEstimation()[3]));


        }

        //Log.d("HI", "=============================Letzter, echter:  " + Service.getListOfPoints().getLast().getX() + " ; " + Service.getListOfPoints().getLast().getY());
        //Log.d("HI", "=============================Letzter, geschätzter:  " + Service.getEstimatedPoints().getLast().getX() + " ; " + Service.getEstimatedPoints().getLast().getY());

    }

    public static synchronized EstimationFilter getInstance() {
        if (instance == null) {
            instance = new EstimationFilter();
        }
        return instance;
    }

    public RealVector getU() {
        return u;
    }
}
