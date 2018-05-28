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

import model.Pair;

/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter{
    private static EstimationFilter instance;

    private KalmanFilter filter;
    // Frage nach dt... Erstmal statisch festlegen, mit 0.1
    //final double dt = 0.1d;
//    final double dt = 0.000001d;
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
    private RealVector x;
    private RealVector u;
    private RealMatrix A;
    private RealMatrix B;
    private RealMatrix Q;
    private RealMatrix R;
    private RealMatrix P;
    private RealMatrix H;

    private ProcessModel pm;
    private MeasurementModel mm;

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
        Log.d("DIM", "Zeilendimension, R:  " + R.getRowDimension());
        Log.d("DIM", "Spaltendimension, R:  " + R.getColumnDimension());

        P = new Array2DRowRealMatrix(new double[][]{
                {10, 0, 0, 0},
                {0, 10, 0, 0},
                {0, 0, 10, 0},
                {0, 0, 0, 10}
        });

        pm = new DefaultProcessModel(A, B, Q, x, P);
        mm = new DefaultMeasurementModel(H, R);
        filter = new KalmanFilter(pm, mm);

    }

    public void makeEstimation() {

        // Zwei Vektoren von der Doku-seite: tmpNoise wurde um zwei erweitert, da wir das rauschen
        // eben in zwei Dimensionen haben
        RealVector tmpPNoise = new ArrayRealVector(new double[]
                {Math.pow(dt, 2d) / 2d, dt, Math.pow(dt, 2d) / 2d, dt});

        RealVector mNoise = new ArrayRealVector(2);

        for (; ; ) {
            if(Service.getThread().isInterrupted()){
                break;
            }

            filter.predict();

            // simulate the process (von doku-page)
//            RealVector pNoise = tmpPNoise.mapMultiply(accelNoise * rand.nextGaussian());
            //RealVector pNoise = tmpPNoise.mapMultiply(accelNoise);

            // x = A * x + B * u + pNoise --> rauschvariante wurde von der doku-page übernommen
            //x = A.operate(x).add(B.operate(u)).add(pNoise);

            // simulate the measurement (von doku-page)
//            mNoise.setEntry(0, measurementNoise * rand.nextGaussian());
//            mNoise.setEntry(1, measurementNoise *  rand.nextGaussian());
            mNoise.setEntry(0, Service.getAccel_x_wgs());
            mNoise.setEntry(1, Service.getAccel_y_wgs());

            //RealVector z = new ArrayRealVector(new double[] {Service.getAccel_x_wgs(), Service.getAccel_y_wgs()});
            RealVector z = new ArrayRealVector(new double[] {Service.getListOfPoints().getLast().getX(),
                    Service.getListOfPoints().getLast().getY()});



            // z = H * x + m_Noise
            //RealVector z = H.operate(x).add(mNoise);

            filter.correct(z);
            //filter.correct(mNoise);

            double estimatedPosition_x = filter.getStateEstimation()[0];
            double estimatedPosition_y = filter.getStateEstimation()[1];

            Log.d("HI", "Geschätzter Punkt:  " + estimatedPosition_x + " ; " + estimatedPosition_y);
            Log.d("HI", "Echter Punkt:  " + Service.getListOfPoints().getLast().getX() + " ; " + Service.getListOfPoints().getLast().getY());

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

    public void correct(final RealVector z)
            throws NullArgumentException, DimensionMismatchException, SingularMatrixException {

        // sanity checks
        MathUtils.checkNotNull(z);
        if (z.getDimension() != H.getRowDimension()) {
            throw new DimensionMismatchException(z.getDimension(),
                    H.getRowDimension());
        }

        // S = H * P(k) * H' + R
        RealMatrix s = H.multiply(P)
                .multiply(H.transpose())
                .add(mm.getMeasurementNoise());

        // Inn = z(k) - H * xHat(k)-
        RealVector innovation = z.subtract(H.operate(x));

        // calculate gain matrix
        // K(k) = P(k)- * H' * (H * P(k)- * H' + R)^-1
        // K(k) = P(k)- * H' * S^-1

        // instead of calculating the inverse of S we can rearrange the formula,
        // and then solve the linear equation A x X = B with A = S', X = K' and B = (H * P)'

        // K(k) * S = P(k)- * H'
        // S' * K(k)' = H * P(k)-'
//        RealMatrix kalmanGain = new CholeskyDecomposition(s).getSolver()
//                .solve(measurementMatrix.multiply(errorCovariance.transpose()))
//                .transpose();
        RealMatrix kalmanGain = P.multiply(H.transpose());

        // update estimate with measurement z(k)
        // xHat(k) = xHat(k)- + K * Inn
        x = x.add(kalmanGain.operate(innovation));

        // update covariance of prediction error
        // P(k) = (I - K * H) * P(k)-
        RealMatrix identity = MatrixUtils.createRealIdentityMatrix(kalmanGain.getRowDimension());
        P = identity.subtract(kalmanGain.multiply(H)).multiply(P);
    }
}
