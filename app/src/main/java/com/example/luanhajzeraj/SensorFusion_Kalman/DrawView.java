package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import geodesy.GlobalPosition;

public class DrawView extends View {
    private Paint paint = new Paint();

    private int heightOfScreen =0;
    private int widthOfScreen =0;

    private static double latitude;
    private static double longitude;
    private static double altitude;

    private int[] NULL_POINT = new int[2];

    private GlobalPosition firstPosition = new GlobalPosition(latitude,longitude,altitude);


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
        // Variable wird erst hier gesetzt, da zuvor breite und höhe noch unbekannt/ungesetzt
        NULL_POINT[0] = widthOfScreen/2; // NULL_POINT[0] = in Richtung x-Achse
        NULL_POINT[1] = heightOfScreen/2; // NULL_POINT[1] = in Richtung y-Achse


        // Zeichne x-Achse
        canvas.drawLine(NULL_POINT[0], 0, NULL_POINT[0], heightOfScreen, paint);

        // Zecihne y-Achse
        canvas.drawLine(0, NULL_POINT[1], widthOfScreen, NULL_POINT[1], paint);

        // Zeichne einen Punkt im Null-punkt und bescrifte ihn mit Lat & Lon
        canvas.drawCircle(NULL_POINT[0],NULL_POINT[1],15,paint);

        paint.setTextSize(28);
        canvas.drawText("( " + latitude + " ; " + longitude + " )",
                (NULL_POINT[0])+10,(NULL_POINT[1])-4,paint);


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
}