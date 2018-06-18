package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.util.Log;

import com.example.luanhajzeraj.SensorFusion_Kalman.EstimationFilter;

public class FilterThread implements Runnable {
    Thread backgroundThread;
    EstimationFilter filter;


    public void start() {
        if (backgroundThread == null) {
            backgroundThread = new Thread(this);
            backgroundThread.start();
        }
    }

    public void stop() {
        if (backgroundThread != null) {
            backgroundThread.interrupt();
            Log.d("HI", "Thread angehalten");
        }
    }

    public void run() {
        try {
            while (!backgroundThread.interrupted()) {
                filter = new EstimationFilter();
                filter.makeEstimation();
            }
        } finally {
            backgroundThread = null;
        }
    }

    public boolean isInterrupted() {
        return backgroundThread.isInterrupted() ? true : false;
    }

    public EstimationFilter getFilter() {
        return filter;
    }
}
