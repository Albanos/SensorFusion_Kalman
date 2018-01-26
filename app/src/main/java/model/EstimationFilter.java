package model;


import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter {
    private static EstimationFilter instance;

    private EstimationFilter(){

    }

    public static synchronized EstimationFilter getInstance(){
        if(instance == null){
            instance = new EstimationFilter();
        }
        return instance;
    }
}
