package model;


/**
 * Created by Luan Hajzeraj on 13.01.2018.
 */

//Nutzung von einem thread-gesicherten singelton-pattern
public class EstimationFilter {
    private static EstimationFilter instance;

    private double latitude;
    private double longitude;

    private float linAccel_x;
    private float linAccel_y;

    private float orient_x;
    private float orient_y;

    private EstimationFilter(){
        initFilter();
    }

    // Initialisiere de Kalman-Filter
    private void initFilter(){

    }

    public static synchronized EstimationFilter getInstance(){
        if(instance == null){
            instance = new EstimationFilter();
        }
        return instance;
    }

    public void setLinAccelerometerValues(float x, float y){
        this.linAccel_x = x;
        this.linAccel_y = y;
    }

    public void setPositionValues(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setOrientationValues(float x, float y){
        this.orient_x = x;
        this.orient_y = y;
    }
}
