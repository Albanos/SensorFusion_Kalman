package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import model.Pair;

import static android.view.MotionEvent.INVALID_POINTER_ID;

public class DrawView extends View {
    private static Paint paint = new Paint();
    private static Paint paint2 = new Paint();
    private int heightOfScreen = 0;
    private int widthOfScreen = 0;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.f;

    // Dateikonfiguration für das Schreiben der Koordinaten in eine Datei
    private static File file = null;
    private FileOutputStream stream = null;

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

//        Toast.makeText(getContext(), "Anzahl, Positionen:  " + listOfPoints.size(),
//                Toast.LENGTH_SHORT).show();

        // Berechne die Abstände zwischen dem ersten und allen weiteren Punkten
        Pair pointOfMaxDistance = calculateMaxDistanceToInitialPoint();

        // Berechne die Achsen-Abschnitte
        Pair pixelPerStep = setPixelPerStep(pointOfMaxDistance);

        // Zeichne x-Achse
        canvas.drawLine((widthOfScreen + 100) / 2, 0, (widthOfScreen + 100) / 2, heightOfScreen + 100, paint);

        // Zecihne y-Achse
        canvas.drawLine(0, (heightOfScreen + 100) / 2, widthOfScreen + 100, (heightOfScreen + 100) / 2, paint);

        // Berechne die neuen Koordinaten anhand von besprochener Formel und zeichne
        // Das try-catch ist für die Eintragung der Koordinaten in eine Datei

        try {
            calculateCoordinatesOfPoints(canvas, pixelPerStep);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // zum zoomen: ende
        canvas.restore();

    }

    float mLastTouchX = 0;
    float mLastTouchY = 0;
    float mPosX = 0;
    float mPosY = 0;
    // The ‘active pointer’ is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        scaleGestureDetector.onTouchEvent(ev);


        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = ev.getActionIndex();

                final float x = ev.getX(pointerIndex);

                final float y = ev.getY(pointerIndex);


                // Remember where we started (for dragging)
                mLastTouchX = x;
                mLastTouchY = y;
                // Save the ID of this pointer (for dragging)
                mActivePointerId = ev.getPointerId(0);

                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // Find the index of the active pointer and fetch its position
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);


                final float x = ev.getX(pointerIndex);

                final float y = ev.getY(pointerIndex);

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

                final int pointerIndex = ev.getActionIndex();


                final int pointerId = ev.getPointerId(pointerIndex);


                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private void calculateCoordinatesOfPoints(Canvas canvas, Pair pixelPerAxes) throws IOException {
        List<Pair> listOfPoints = Service.getListOfPoints();
        List<Pair> listOfOldPoints = Service.getListOfOldPoints();

        //Pair initialPoint = new Pair(listOfPoints.get(0).getX(), listOfPoints.get(0).getY());

        // Der initialPoint ist der allererste, berechnete Punkt (auf Basis der firstGlobalPosition).
        // Es kann NICHT die firstGlobalPosition selbst sein, da diese eben eine Position ist und
        // keine x- & y-Koordinaten hat!
        Pair initialPoint = Service.getInitialPoint();
        paint2.setColor(Color.BLUE);

        // Zeichne die alten Punkte
        for (Pair currentPoint : listOfOldPoints) {
            int pixel_x = (int) (Math.abs(currentPoint.getX() - initialPoint.getX()) * pixelPerAxes.getX());
            pixel_x = currentPoint.getX() >= initialPoint.getX() ? pixel_x + (widthOfScreen / 2) : widthOfScreen - pixel_x - (widthOfScreen / 2);
            pixel_x = pixel_x + 50;

            int pixel_y = (int) (Math.abs(currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            pixel_y = currentPoint.getY() <= initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : heightOfScreen - pixel_y - (heightOfScreen / 2);
//            pixel_y = currentPoint.getY() <= initialPoint.getY() ? (heightOfScreen / 2) + pixel_y : pixel_y;
//            pixel_y = pixel_y + 50;
            //int pixel_y = (int) ((currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            //pixel_y = (heightOfScreen / 2) + pixel_y + 50;
//            pixel_y = currentPoint.getY() < initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : pixel_y;
            pixel_y += 50;

            canvas.drawCircle(pixel_x, pixel_y, 15, paint2);
        }

        // Zeichne die aktuellen Punkte
        for (int i = 0; i < listOfPoints.size(); i++) {

            Pair currentPoint = listOfPoints.get(i);

            int pixel_x = (int) (Math.abs(currentPoint.getX() - initialPoint.getX()) * pixelPerAxes.getX());
            pixel_x = currentPoint.getX() >= initialPoint.getX() ? pixel_x + (widthOfScreen / 2) : widthOfScreen - pixel_x - (widthOfScreen / 2);
            pixel_x = pixel_x + 50;

            int pixel_y = (int) (Math.abs(currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            pixel_y = currentPoint.getY() <= initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : heightOfScreen - pixel_y - (heightOfScreen / 2);
//            pixel_y = currentPoint.getY() <= initialPoint.getY() ? (heightOfScreen / 2) + pixel_y : pixel_y;
//            pixel_y = pixel_y + 50;
            //int pixel_y = (int) ((currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            //pixel_y = (heightOfScreen / 2) + pixel_y + 50;
//            pixel_y = currentPoint.getY() < initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : pixel_y;
            pixel_y += 50;

            paint.setColor(Color.BLACK);
            canvas.drawCircle(pixel_x, pixel_y, 15, paint);

            // Schreibe auch die Koordinaten auf den Screen
            paint.setTextSize(48);
            //canvas.drawText("" + i, pixel_x + 12, pixel_y - 2, paint);
        }

        // Zeichne die alten, GESCHÄTZTEN Punkte
        for (Pair currentPoint : Service.getOldEstimatedPoints()) {
            Paint paint4 = new Paint();
            paint4.setColor(Color.GREEN);
            int pixel_x = (int) (Math.abs(currentPoint.getX() - initialPoint.getX()) * pixelPerAxes.getX());
            pixel_x = currentPoint.getX() >= initialPoint.getX() ? pixel_x + (widthOfScreen / 2) : widthOfScreen - pixel_x - (widthOfScreen / 2);
            pixel_x = pixel_x + 50;

            int pixel_y = (int) (Math.abs(currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            pixel_y = currentPoint.getY() <= initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : heightOfScreen - pixel_y - (heightOfScreen / 2);
//            pixel_y = currentPoint.getY() <= initialPoint.getY() ? (heightOfScreen / 2) + pixel_y : pixel_y;
//            pixel_y = pixel_y + 50;
            //int pixel_y = (int) ((currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
            //pixel_y = (heightOfScreen / 2) + pixel_y + 50;
//            pixel_y = currentPoint.getY() < initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : pixel_y;
            pixel_y += 50;

            canvas.drawCircle(pixel_x, pixel_y, 15, paint4);
        }

        // Zeichne die "neu" geschätzten Punkte
        if(! Service.getEstimatedPoints().isEmpty()){
            Paint paint3 = new Paint();
            paint3.setColor(Color.RED);

            for (int i = 0; i < Service.getEstimatedPoints().size(); i++) {

                Pair currentPoint = Service.getEstimatedPoints().get(i);

                int pixel_x = (int) (Math.abs(currentPoint.getX() - initialPoint.getX()) * pixelPerAxes.getX());
                pixel_x = currentPoint.getX() >= initialPoint.getX() ? pixel_x + (widthOfScreen / 2) : widthOfScreen - pixel_x - (widthOfScreen / 2);
                pixel_x = pixel_x + 50;

                int pixel_y = (int) (Math.abs(currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
                pixel_y = currentPoint.getY() <= initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : heightOfScreen - pixel_y - (heightOfScreen / 2);
//            pixel_y = currentPoint.getY() <= initialPoint.getY() ? (heightOfScreen / 2) + pixel_y : pixel_y;
//            pixel_y = pixel_y + 50;
                //int pixel_y = (int) ((currentPoint.getY() - initialPoint.getY()) * pixelPerAxes.getY());
                //pixel_y = (heightOfScreen / 2) + pixel_y + 50;
//            pixel_y = currentPoint.getY() < initialPoint.getY() ? pixel_y + (heightOfScreen / 2) : pixel_y;
                pixel_y += 50;

                canvas.drawCircle(pixel_x, pixel_y, 15, paint3);
            }
        }
    }

    private void writeCoordinatesToFile(Pair currentPoint, int counterForCoordinates) throws IOException {
        double x = currentPoint.getX();
        double y = currentPoint.getY();
        String toWrite = "";

        if (file == null) {
            Log.d("Test", "Pfad:  " + this.getContext().getFilesDir());
            file = new File(this.getContext().getExternalFilesDir(null), "coordinatesOnScreen.txt");
            //file = new File(this.getContext().getFilesDir(), "coordinatesOnScreen.txt");

            toWrite = "P" + counterForCoordinates + ": (" + x + " ; " + y + ")\n";
        } else {
            String content = readDataFromFile(file.getName());
            toWrite = content + "\n" + ("P" + counterForCoordinates + ": (" + x + " ; " + y + ")\n");
        }
        stream = new FileOutputStream(file);
        //stream = stream == null ? new FileOutputStream(file) : stream;
        //stream = stream == null ? this.getContext().openFileOutput("coordinatesOnScreen.txt",Context.MODE_PRIVATE) : stream;

        try {
            //writeDataToFile(stream, toWrite);

            stream.write(toWrite.getBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDataToFile(FileOutputStream stream, String toWrite) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

            bufferedWriter.write(toWrite);

            bufferedWriter.flush();
            bufferedWriter.close();
            outputStreamWriter.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String readDataFromFile(String fileName) {
        try {
            FileInputStream inputStream = this.getContext().openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder stringBuilder = new StringBuilder();
            String oneLine;

            while ((oneLine = bufferedReader.readLine()) != null) {
                stringBuilder.append(oneLine);
            }

            bufferedReader.close();
            inputStream.close();
            inputStreamReader.close();

            return stringBuilder.toString();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Pair calculateMaxDistanceToInitialPoint() {
        double max_x = 1;
        double max_y = 1;
        double min_x = 1;
        double min_y = 1;
        List<Pair> allPoints = new ArrayList<>();
        allPoints.addAll(Service.getListOfOldPoints());
        allPoints.addAll(Service.getListOfPoints());

//        double initPoint_x = Service.getListOfPoints().get(Service.getListOfPoints().size()-1).getX();
//        double initPoint_y = Service.getListOfPoints().get(Service.getListOfPoints().size()-1).getY();
        for (Pair entry : allPoints) {
            if (entry.getY() < min_y) {
                min_y = entry.getY();
            }
            if (entry.getX() < min_x) {
                min_x = entry.getX();
            }
            if (entry.getY() > max_y) {
                max_y = entry.getY();
            }
            if (entry.getX() > max_x) {
                max_x = entry.getX();
            }
        }
        return new Pair(Math.abs(max_x - min_x), Math.abs(max_y - min_y));
//        for (Pair entry : allPoints) {
//            //find max x and max y distance
//            double x = entry.getX();
//            double y = entry.getY();
//
//            // Berechne die Abstände zwischen dem initialen Punkt und dem aktuell betrachteten (als Betrag)
//            double distance_x = Math.abs(initPoint_x - x);
//            double distance_y = Math.abs(initPoint_y - y);
//            // Wenn aktuelle Differenz größer: setze neu
//            if (distance_x > max_x) {
//                max_x = distance_x;
//            }
//            if (distance_y > max_y) {
//                max_y = distance_y;
//            }
//        }
//        return new Pair(max_x, max_y);
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
}