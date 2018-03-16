package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


public class DrawView extends View {
    private Paint paint = new Paint();

    private int heightOfScreen = 0;
    private int widthOfScreen = 0;

    private int oneStepOfXAxes;
    private int oneStepOfYAxes;

    private static double latitude;
    private static double longitude;
    private static double altitude;

    private static double latitude_old;
    private static double longitude_old;
    private static double altitude_old;

    private int[] NULL_POINT = new int[2];

    private double xComponentOfSecondPosition;
    private double yComponentOfSecondPosition;


    private static Boolean init = true;


    private void init() {
        paint.setColor(Color.BLACK);
    }

    public DrawView(Context context) {
        super(context);
        init();

    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Wird hier gesetzt, weil an der Stelle höhe und breite gesetzt ist
        setStepsOfAxes();

        // Variable wird erst hier gesetzt, da zuvor breite und höhe noch unbekannt/ungesetzt
        NULL_POINT[0] = widthOfScreen / 2; // NULL_POINT[0] = in Richtung x-Achse
        NULL_POINT[1] = heightOfScreen / 2; // NULL_POINT[1] = in Richtung y-Achse

        // Passe die x- und y-Komponente an das Koordinatensystem an (verfügt über 20 Stellen)
        xComponentOfSecondPosition *= oneStepOfXAxes;
        yComponentOfSecondPosition *= oneStepOfYAxes;

        // Berechne nun den neuen Punkt
        float xOfNewPosition = NULL_POINT[0] + (float) xComponentOfSecondPosition;
        float yOfNewPosition = NULL_POINT[1] + (float) yComponentOfSecondPosition;


        // Zeichne x-Achse
        canvas.drawLine(NULL_POINT[0], 0, NULL_POINT[0], heightOfScreen, paint);

        // Zecihne y-Achse
        canvas.drawLine(0, NULL_POINT[1], widthOfScreen, NULL_POINT[1], paint);

        // Zeichne einen Punkt im Null-punkt und bescrifte ihn mit Lat & Lon
        //canvas.drawCircle(NULL_POINT[0], NULL_POINT[1], 15, paint);

        //paint.setTextSize(28);
        //canvas.drawText("( " + latitude + " ; " + longitude + " )",
        //      (NULL_POINT[0]) + 10, (NULL_POINT[1]) - 4, paint);


        // Zeichne Punkte

        // die erste Position ist im Nullpunkt zu zeichnen
        canvas.drawCircle(NULL_POINT[0], NULL_POINT[1], 15, paint);

        paint.setTextSize(28);
        canvas.drawText("( " + latitude_old + " ; " + longitude_old + " )",
                (NULL_POINT[0]) + 10, (NULL_POINT[1]) - 4, paint);

        // Zeichne den neuen Punkt
        //canvas.drawCircle(NULL_POINT[0] + (float) xComponentOfSecondPosition, NULL_POINT[1] + (float) yComponentOfSecondPosition, 15, paint);
        canvas.drawCircle(xOfNewPosition, yOfNewPosition, 15, paint);


        canvas.drawText("( " + latitude + " ; " + longitude + " )",
                (xOfNewPosition) + 10, (yOfNewPosition) - 4, paint);


    }

    /**
     * Unterteile die x- und y-Achse in 20 Abschnitte, je nach Bildschirmgröße
     */
    private void setStepsOfAxes() {
        if (heightOfScreen != 0 && widthOfScreen != 0) {
            oneStepOfYAxes = heightOfScreen / 20;
            oneStepOfXAxes = widthOfScreen / 20;
        }
    }

    public void setHeightOfScreen(int heightOfScreen) {
        this.heightOfScreen = heightOfScreen;
    }

    public void setWidthOfScreen(int widthOfScreen) {
        this.widthOfScreen = widthOfScreen;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setLongitude_old(double longitude_old) {
        this.longitude_old = longitude_old;
    }

    public void setLatitude_old(double latitude_old) {
        this.latitude_old = latitude_old;
    }

    public void setAltitude_old(double altitude_old) {
        this.altitude_old = altitude_old;
    }

    public void setxComponentOfSecondPosition(double xComponentOfSecondPosition) {
        this.xComponentOfSecondPosition = xComponentOfSecondPosition;
    }

    public void setyComponentOfSecondPosition(double yComponentOfSecondPosition) {
        this.yComponentOfSecondPosition = yComponentOfSecondPosition;
    }
}