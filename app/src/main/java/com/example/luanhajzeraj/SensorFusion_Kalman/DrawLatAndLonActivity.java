package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class DrawLatAndLonActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activateFullscreen();
        setContentView(R.layout.activity_draw_lat_and_lon);
        drawCoordinateSystemWithPoints();
    }

    private void activateFullscreen() {
        // Blende den Android-footer aus (im Entwicklunhs-handy vorhanden)
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void drawCoordinateSystemWithPoints() {
        DrawView drawView = new DrawView(this);

        // Ermitteln der Display breite und höhe
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        drawView.setHeightOfScreen(height);
        drawView.setWidthOfScreen(width);

        TextView tv_nothingToPlot = findViewById(R.id.tv_Activity_drawLatAndLon);

        if (Service.calculateCartesianCoordinats()) {
            tv_nothingToPlot.setVisibility(TextView.INVISIBLE);
            setContentView(drawView);
        } else {
            tv_nothingToPlot.setVisibility(TextView.VISIBLE);
            tv_nothingToPlot.setText("Nothing to Plot");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("LH", "In DrawLatAndLonActivity, onPause!");
        // Setze die alten Punkte aus der Klasse drawView
        Service.getListOfOldPoints().addAll(Service.getListOfPoints());
        // Bereinige die Liste
        Service.getListOfPoints().clear();
        Service.getListOfPositions().clear();
        Log.d("LH", "In DrawLatAndLonActivity, onPause, oldPoint gesetzt");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("LH", "In DrawLatAndLonActivity, onResume!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LH", "In DrawLatAndLonActivity, onDestroy!");
    }

}
