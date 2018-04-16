package com.example.luanhajzeraj.SensorFusion_Kalman;


/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter {
    private static EstimationFilter instance;
    // Initial Geschwindigkeit in die Richtungen x,y


    private EstimationFilter(){
//        initFilter();
    }

    // Initialisiere den Kalman-Filter
  /*  private void initFilter(){
        ProcessModel pm;
        MeasurementModel mm;
        KalmanFilter kf;
        RealVector u;
        RealVector x;
        RealMatrix A;
        RealMatrix B;
        RealMatrix H;
        RealMatrix Q;
        RealMatrix R;
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

    }*/

    public static synchronized EstimationFilter getInstance(){
        if(instance == null){
            instance = new EstimationFilter();
        }
        return instance;
    }
}
