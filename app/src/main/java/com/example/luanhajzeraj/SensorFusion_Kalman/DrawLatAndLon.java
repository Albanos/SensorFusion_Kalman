package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import geodesy.GlobalPosition;
import model.EstimationFilter;
import model.Pair;

import static android.view.MotionEvent.INVALID_POINTER_ID;

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

    private static ArrayList<GlobalPosition> listOfPositions = new ArrayList<>();
    private DrawView drawView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Blende den Android-footer aus (im Entwicklunhs-handy vorhanden)
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        setLatAndLonAndAlt();

        setContentView(R.layout.activity_draw_lat_and_lon);

        drawCoordinateSystemWithPoints();

    }

    private void drawCoordinateSystemWithPoints(){

        drawView = new DrawView(this);

        // Ermitteln der Display breite und höhe
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        drawView.setHeightOfScreen(height);
        drawView.setWidthOfScreen(width);

        TextView tv_nothingToPlot = (TextView) findViewById(R.id.tv_Activity_drawLatAndLon);

        if(listOfPositions.size() >=2) {
            GlobalPosition firstGlobalPositionOfList = listOfPositions.get(0);

            // Da wir über dieses element nicht iterieren wollen, entfernen wir es aus der Liste
            listOfPositions.remove(0);

            // Berechne für den Rest die x- und y-Koordinaten
            for (GlobalPosition gp : listOfPositions) {
                tv_nothingToPlot.setVisibility(TextView.INVISIBLE);

                // Berechne Abstand der jeweiligen position zur ersten Position
                double distance = filter.coordinateDistanceBetweenTwoPoints(firstGlobalPositionOfList, gp);
                double angle = filter.coordinateAngleBetweenTwoPoints(firstGlobalPositionOfList, gp);

                // Berechne auf Basis von Distanz und Winkel die x- und y-Komponente des zweiten Punktes
                // Formeln:
                // x = dist. * sin(angle)
                // y = dist. * cos(angle)

                double xComponent = distance * Math.sin(angle);
                double yComponent = distance * Math.cos(angle);

                ArrayList<Double> temp = new ArrayList<>();
                temp.add(xComponent);
                temp.add(yComponent);

                drawView.getListOfPoints().add(temp);

                setContentView(drawView);

            }
        }
        else{

            tv_nothingToPlot.setVisibility(TextView.VISIBLE);
            tv_nothingToPlot.setText("Nothing to Plot");
        }

        // Code funktioniert; wir versuchen nur eine andere Variante
//        drawView.setLatitude(latitude);
//        drawView.setLongitude(longitude);
//        drawView.setAltitude(altitude);
//
//        drawView.setLatitude_old(latitude_old);
//        drawView.setLongitude_old(longitude_old);
//        drawView.setAltitude_old(altitude_old);
//
//
//        TextView tv_nothingToPlot = (TextView) findViewById(R.id.tv_Activity_drawLatAndLon);
//
//        if((latitude != latitude_old && latitude_old != 0) && (longitude != longitude_old && longitude_old != 0)){
//            tv_nothingToPlot.setVisibility(TextView.INVISIBLE);
//
//            GlobalPosition firstPosition = new GlobalPosition(latitude_old, longitude_old, altitude_old);
//            GlobalPosition secondPosition = new GlobalPosition(latitude, longitude, altitude);
//
//            double distance = filter.coordinateDistanceBetweenTwoPoints(firstPosition, secondPosition);
//            double angle = filter.coordinateAngleBetweenTwoPoints(firstPosition, secondPosition);
//
//            System.out.println();
//
//            // Berechne auf Basis von Distanz und Winkel die x- und y-Komponente des zweiten Punktes
//            // Formeln:
//            // x = dist. * sin(angle)
//            // y = dist. * cos(angle)
//
//            double xComponent = distance * Math.sin(angle);
//            double yComponent = distance * Math.cos(angle);
//
//            // Setze die entsprechenden Variablen in DraView, um sie zeichnen zu können
//            drawView.setxComponentOfSecondPosition(xComponent);
//            drawView.setyComponentOfSecondPosition(yComponent);
//
//            // zeichnen
//            setContentView(drawView);
//        }
//
//        else{
//            tv_nothingToPlot.setVisibility(TextView.VISIBLE);
//
//            tv_nothingToPlot.setText("Nothing to Plot");
//        }
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

    public ArrayList<GlobalPosition> getListOfPositions() {
        return listOfPositions;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("LH", "In DrawLatAndLon, onPause!");

        // Bereinige die Liste
        listOfPositions.clear();

        // Setze die alten Punkte aus der Klasse drawView
        ArrayList<ArrayList<Double>> listOfPoints = drawView.getListOfPoints();
        ArrayList<Pair> listOfOldPoints = DrawView.getListOfOldPoints();

        for (int i = 0; i < listOfPoints.size(); i++) {
            Pair currentPoint = new Pair(listOfPoints.get(i).get(0), listOfPoints.get(i).get(1));

            listOfOldPoints.add(currentPoint);
            Log.d("LH", "In DrawLatAndLon, onPause, oldPoint gesetzt");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("LH", "In DrawLatAndLon, onResume!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LH", "In DrawLatAndLon, onDestroy!");
    }

}
