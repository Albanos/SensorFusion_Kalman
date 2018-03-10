package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;

import geodesy.GlobalPosition;
import model.EstimationFilter;

public class DrawLatAndLon extends AppCompatActivity {

    // Speichere Länge, Breite und Höhe, jeweills aktuell und auch als alten Wert, um beide zeichnen
    // zu können: den alten (im Ursprung) und die Verschiebung
    private static double latitude;
    private static double longitude;
    private static double altitude;

    private static double latitude_old = 0.0;
    private static double longitude_old = 0.0;
    private static double altitude_old = 0.0;

    private EstimationFilter filter = EstimationFilter.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_draw_lat_and_lon);

        setLatAndLonAndAlt();

        drawCoordinateSystem();

    }

    /**
     * Zeichnet das Koordinatensystem, zusammen mit seinen Punkten.
     */
    private void drawCoordinateSystem() {
        DrawView drawView = new DrawView(this);

        // Ermitteln der Display breite und höhe
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        drawView.setHeightOfScreen(height);
        drawView.setWidthOfScreen(width);

        drawView.setLatitude(latitude);
        drawView.setLongitude(longitude);
        drawView.setAltitude(altitude);

        if(latitude_old != 0 && longitude_old != 0) {
            GlobalPosition firstPosition = new GlobalPosition(latitude_old, longitude_old, altitude_old);
            GlobalPosition secondPosition = new GlobalPosition(latitude, longitude, altitude);

            double distance = filter.coordinateDistanceBetweenTwoPoints(firstPosition, secondPosition);
            double angle = filter.coordinateAngleBetweenTwoPoints(firstPosition, secondPosition);

            System.out.println();
        }

        // zeichnen
        setContentView(drawView);

    }

    /**
     * Setzt jeweils die aktuelle, als auch die alte Länge, Breite und Höhe
     */
    private void setLatAndLonAndAlt(){
        latitude_old = latitude;
        longitude_old = longitude;
        altitude_old = altitude;

        Bundle extras = getIntent().getExtras();
        latitude = extras.getDouble("latitude");
        longitude = extras.getDouble("longitude");
        altitude = extras.getDouble("altitude");
    }
}
