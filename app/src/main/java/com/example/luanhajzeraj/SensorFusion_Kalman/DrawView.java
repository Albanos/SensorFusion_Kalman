package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import model.Pair;

import static android.view.MotionEvent.INVALID_POINTER_ID;


public class DrawView extends View {
    private static Paint paint = new Paint();
    private static Paint paint2 = new Paint();

    private int heightOfScreen = 0;
    private int widthOfScreen = 0;

    private static double latitude;
    private static double longitude;
    private static double altitude;

    private static ArrayList<Pair> listOfOldPoints = new ArrayList<>();

    private ArrayList<ArrayList<Double>> listOfPoints = new ArrayList<>();
    private static ArrayList<Pair> tempList = new ArrayList<>();

    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.f;

    public DrawView(Context context) {
        super(context);

        // zum zoomen
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        init();
    }

    private void init() {
        paint.setColor(Color.BLACK);
    }

    // Für den zoom
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        // Let the ScaleGestureDetector inspect all events.
//        scaleGestureDetector.onTouchEvent(ev);
//
//        return true;
//    }


    @Override
    public void onDraw(Canvas canvas) {
        Log.d("LH", "in DrawView, onDraw");


        // für den zoom: beginn
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);

        Toast.makeText(getContext(), "Anzahl, Positionen:  " + listOfPoints.size(),
                Toast.LENGTH_SHORT).show();

        // Berechne die Abstände zwischen dem ersten und allen weiteren Punkten
        Pair pointOfMaxDistance = calculateMaxDistanceToInitialPoint();

        // Berechne die Achsen-Abschnitte
        Pair pixelPerStep = setPixelPerStep(pointOfMaxDistance);

        // Zeichne x-Achse
        canvas.drawLine((widthOfScreen + 100) / 2, 0, (widthOfScreen + 100) / 2, heightOfScreen + 100, paint);

        // Zecihne y-Achse
        canvas.drawLine(0, (heightOfScreen + 100) / 2, widthOfScreen + 100, (heightOfScreen + 100) / 2, paint);

        // Berechne die neuen Koordinaten anhand von besprochener Formel und zeichne
        calculateCoordinatesOfPoints(canvas, pixelPerStep);


        // Wenn alte Punkte existieren: Zeichne auch diese in Blau ein
//        if(listOfPointsToDraw.size() > 0){
//            for(Pair p : listOfPointsToDraw){
//                double x = p.getX();
//                double y = p.getY();
//
//                // Zeichne die alten Punkte
//                Paint paint2 = new Paint();
//
//                paint2.setColor(Color.BLUE);
//
//                canvas.drawCircle((float) x, (float) y, 15, paint2);
//
//            }
//        }

//        // Zeichne alle (neuen) Punkte
//        for (ArrayList<Double> a : listOfPoints) {
//            double x = a.get(0);
//            double y = a.get(1);
//
//            // Berechne Koordinaten, gemäß KoordinatenSystem
//            x *= oneStepOfXAxes;
//            y *= oneStepOfYAxes;
//
//            // Berechne die Verschiebung, aus Null-Punkt heraus
//            float xComponent = (float) x + NULL_POINT[0];
//            float yComponent = (float) y + NULL_POINT[1];
//
//            // Speichere die gezeichneten Punkte in Map
//            //oldCoordinates.put(xComponent,yComponent);
//            listOfPointsToDraw.add(new Pair(xComponent,yComponent));
//
//            // Zeichne den Punkt
//            canvas.drawCircle(xComponent, yComponent, 15, paint);
//
//            // Beschrifte den Punkt
//            paint.setTextSize(28);
//            //canvas.drawText("( " + xComponent + " ; " + yComponent + " )",xComponent + 10,yComponent -4, paint);
//        }

        // zum zoomen: ende
        canvas.restore();

    }

    float mLastTouchX = 0;
    float mLastTouchY = 0;
    float mPosX = 0;
    float mPosY= 0;
    // The ‘active pointer’ is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        scaleGestureDetector.onTouchEvent(ev);


        final int action = MotionEventCompat.getActionMasked(ev);




        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = MotionEventCompat.getActionIndex(ev);

                final float x = MotionEventCompat.getX(ev, pointerIndex);

                final float y = MotionEventCompat.getY(ev, pointerIndex);


                // Remember where we started (for dragging)
                mLastTouchX = x;
                mLastTouchY = y;
                // Save the ID of this pointer (for dragging)
                mActivePointerId = MotionEventCompat.getPointerId(ev,0);

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);


                final float x = MotionEventCompat.getX(ev, pointerIndex);

                final float y = MotionEventCompat.getY(ev, pointerIndex);

                // Calculate the distance moved
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                mPosX += dx;
                mPosY += dy;

                invalidate();

                // Remember this touch position for the next move event
                mLastTouchX = x;
                mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {

                final int pointerIndex = MotionEventCompat.getActionIndex(ev);


                final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);


                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = MotionEventCompat.getX(ev, newPointerIndex);
                    mLastTouchY = MotionEventCompat.getY(ev, newPointerIndex);
                    mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
                }
                break;
            }
        }
        return true;
    }


    private void calculateCoordinatesOfPoints(Canvas canvas, Pair pixelPerAxes) {

        canvas.drawCircle((widthOfScreen / 2)+50, (heightOfScreen / 2)+50, 15, paint);

        Pair initialPoint = new Pair(listOfPoints.get(0).get(0), listOfPoints.get(0).get(1));

        paint2.setColor(Color.BLUE);

        // Zeichne zunächst die alten Punkte und anschließend die neuen

        for (int i = 0; i < listOfOldPoints.size(); i++) {
            Pair currentPoint = listOfOldPoints.get(i);

            int pixel_x = (int) (Math.abs(currentPoint.getX() - initialPoint.getX()) * pixelPerAxes.getX());
            pixel_x = currentPoint.getX() >= initialPoint.getX() ? pixel_x + (widthOfScreen / 2) : widthOfScreen - pixel_x - (widthOfScreen / 2);
            pixel_x = pixel_x + 50;

            int pixel_y = (int) (Math.abs(currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            pixel_y = currentPoint.getY() >= initialPoint.getY() ? (heightOfScreen / 2) + pixel_y : pixel_y;
            pixel_y = pixel_y + 50;

            canvas.drawCircle(pixel_x, pixel_y, 15, paint2);
        }

        for (int i = 1; i < listOfPoints.size(); i++) {
            Pair currentPoint = new Pair(listOfPoints.get(i).get(0), listOfPoints.get(i).get(1));

            int pixel_x = (int) (Math.abs(currentPoint.getX() - initialPoint.getX()) * pixelPerAxes.getX());
            pixel_x = currentPoint.getX() >= initialPoint.getX() ? pixel_x + (widthOfScreen / 2) : widthOfScreen - pixel_x - (widthOfScreen / 2);
            pixel_x = pixel_x + 50;

            int pixel_y = (int) (Math.abs(currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            pixel_y = currentPoint.getY() >= initialPoint.getY() ? (heightOfScreen / 2) + pixel_y : pixel_y;
            pixel_y = pixel_y + 50;

            paint.setColor(Color.BLACK);
            canvas.drawCircle(pixel_x, pixel_y, 15, paint);

            // Schreibe auch die Koordinaten auf den Screen
            paint.setTextSize(28);
            //canvas.drawText("( " + pixel_x + " ; " + pixel_y + " )",pixel_x + 10,pixel_y -4, paint);

            tempList.add(new Pair(pixel_x,pixel_y));

            //checkIfComponentsOfCurrentPointInsideTempList(new Pair(pixel_x,pixel_y));
            //checkIfComponentsOfCurrentPointInsideTempList(currentPoint);
            //listOfOldPoints.add(currentPoint);
            //listOfOldPoints.add(new Pair(pixel_x, pixel_y));
        }

    }

    private Pair calculateMaxDistanceToInitialPoint() {

        double max_x = 1;
        double max_y = 1;

        double initPoint_x = listOfPoints.get(0).get(0);
        double initPoint_y = listOfPoints.get(0).get(1);

        for (ArrayList<Double> entry : listOfPoints) {
            //find max x and max y distance
            double x = entry.get(0);
            double y = entry.get(1);

            // Berechne die Abstände zwischen dem initialen Punkt und dem aktuell betrachteten (als Betrag)
            double distance_x = Math.abs(initPoint_x - x);
            double distance_y = Math.abs(initPoint_y - y);


            // Wenn aktuelle Differenz größer: setze neu
            if (distance_x > max_x) {
                max_x = distance_x;
            }

            if (distance_y > max_y) {
                max_y = distance_y;
            }
        }

        return new Pair(max_x, max_y);


        //speichere anzahl der axen-steps
        //setze nullpoint
        //setze initPoint auf nullposition und hinterlege originalcoordinaten des punktes für die verschiebung der anderen Punkte
        /*berechne koordinaten von jedem punkt mit der Formel:
                x = (breite * betrag(A_x - B_x) / steps_x)
                x = A_x > B_x ? breite - x : x;

                genau so für y
        */

    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            invalidate();

            return true;
        }
    }

//    @Override
//    public void onDraw(Canvas canvas) {
//        // Wird hier gesetzt, weil an der Stelle höhe und breite gesetzt ist
//        setPixelPerStep();
//
//        // Variable wird erst hier gesetzt, da zuvor breite und höhe noch unbekannt/ungesetzt
//        NULL_POINT[0] = widthOfScreen / 2; // NULL_POINT[0] = in Richtung x-Achse
//        NULL_POINT[1] = heightOfScreen / 2; // NULL_POINT[1] = in Richtung y-Achse
//
//        // Zeichne x-Achse
//        canvas.drawLine(NULL_POINT[0], 0, NULL_POINT[0], heightOfScreen, paint);
//
//        // Zecihne y-Achse
//        canvas.drawLine(0, NULL_POINT[1], widthOfScreen, NULL_POINT[1], paint);
//
//        // Zeichne einen Punkt im Null-punkt und bescrifte ihn mit Lat & Lon
//        canvas.drawCircle(NULL_POINT[0], NULL_POINT[1], 15, paint);
//
//
//        for (ArrayList<Double> a : listOfPoints) {
//            double x = a.get(0);
//            double y = a.get(1);
//
//            // Berechne Koordinaten, gemäß KoordinatenSystem
//            x *= oneStepOfXAxes;
//            y *= oneStepOfYAxes;
//
//            // Berechne die Verschiebung, aus Null-Punkt heraus
//            float xComponent = (float) x + NULL_POINT[0];
//            float yComponent = (float) y + NULL_POINT[1];
//
//            // Zeichne den Punkt
//            canvas.drawCircle(xComponent, yComponent, 15, paint);
//
//            // Beschrifte den Punkt
//            paint.setTextSize(28);
//            //canvas.drawText("( " + xComponent + " ; " + yComponent + " )",xComponent + 10,yComponent -4, paint);
//        }
//    }

//    @Override
//    public void onDraw(Canvas canvas) {
//        // Wird hier gesetzt, weil an der Stelle höhe und breite gesetzt ist
//        setPixelPerStep();
//
//        // Variable wird erst hier gesetzt, da zuvor breite und höhe noch unbekannt/ungesetzt
//        NULL_POINT[0] = widthOfScreen / 2; // NULL_POINT[0] = in Richtung x-Achse
//        NULL_POINT[1] = heightOfScreen / 2; // NULL_POINT[1] = in Richtung y-Achse
//
//        // Passe die x- und y-Komponente an das Koordinatensystem an (verfügt über 20 Stellen)
//        xComponentOfSecondPosition *= oneStepOfXAxes;
//        yComponentOfSecondPosition *= oneStepOfYAxes;
//
//        // Berechne nun den neuen Punkt
//        float xOfNewPosition = NULL_POINT[0] + (float) xComponentOfSecondPosition;
//        float yOfNewPosition = NULL_POINT[1] + (float) yComponentOfSecondPosition;
//
//
//        // Zeichne x-Achse
//        canvas.drawLine(NULL_POINT[0], 0, NULL_POINT[0], heightOfScreen, paint);
//
//        // Zecihne y-Achse
//        canvas.drawLine(0, NULL_POINT[1], widthOfScreen, NULL_POINT[1], paint);
//
//        // Zeichne einen Punkt im Null-punkt und bescrifte ihn mit Lat & Lon
//        //canvas.drawCircle(NULL_POINT[0], NULL_POINT[1], 15, paint);
//
//        //paint.setTextSize(28);
//        //canvas.drawText("( " + latitude + " ; " + longitude + " )",
//        //      (NULL_POINT[0]) + 10, (NULL_POINT[1]) - 4, paint);
//
//
//        // Zeichne Punkte
//
//        // die erste Position ist im Nullpunkt zu zeichnen
//        canvas.drawCircle(NULL_POINT[0], NULL_POINT[1], 15, paint);
//
//        paint.setTextSize(28);
//        canvas.drawText("( " + latitude_old + " ; " + longitude_old + " )",
//                (NULL_POINT[0]) + 10, (NULL_POINT[1]) - 4, paint);
//
//        // Zeichne den neuen Punkt
//        //canvas.drawCircle(NULL_POINT[0] + (float) xComponentOfSecondPosition, NULL_POINT[1] + (float) yComponentOfSecondPosition, 15, paint);
//        canvas.drawCircle(xOfNewPosition, yOfNewPosition, 15, paint);
//
//
//        canvas.drawText("( " + latitude + " ; " + longitude + " )",
//                (xOfNewPosition) + 10, (yOfNewPosition) - 4, paint);
//
//
//    }

    private Pair setPixelPerStep(Pair pointOfMaxDistance) {
        return new Pair(widthOfScreen / (pointOfMaxDistance.getX() * 2),
                heightOfScreen / (pointOfMaxDistance.getY() * 2));
    }

    public void setHeightOfScreen(int heightOfScreen) {
        this.heightOfScreen = heightOfScreen - 100;
    }

    public void setWidthOfScreen(int widthOfScreen) {
        this.widthOfScreen = widthOfScreen - 100;
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

    public ArrayList<ArrayList<Double>> getListOfPoints() {
        return listOfPoints;
    }

    public static ArrayList<Pair> getListOfOldPoints() {
        return listOfOldPoints;
    }

    public static ArrayList<Pair> getTempList() {
        return tempList;
    }
}