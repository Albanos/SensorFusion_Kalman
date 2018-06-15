package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.instacart.library.truetime.TrueTimeRx;

import java.sql.Timestamp;

import geodesy.GlobalPosition;
import io.reactivex.schedulers.Schedulers;
import model.Coordinates;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private double latitude;
    private double longitude;
    private double altitude;
    private float speed;
    private float[] velocity = {0, 0};
    private LocationManager lm;
    private LocationListener locationListener;
    private SensorManager sensorManager;
    private SensorManager sensorManagerGravity;
    private SensorManager sensorManagerAccelerometer;
    private SensorManager sensorManagerMagnetic;
    // Update-Zeit: 1s
    private final int UPDATE_TIME_LOCATION = 1000;
    // Minimale Distanz der Änderung: null meter
    private final int UPDATE_MIN_DISTANCE = 0;

    private final int PERMISSION_REQUEST = 0;
    private static int demoGNSSCounter = 0;
    private static long oldTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeTimeNowFrameworkForRealTime();
        showValueOfPosition();
    }

    private void initializeTimeNowFrameworkForRealTime() {
        TrueTimeRx.build()
                .initializeRx("time.google.com")
                .subscribeOn(Schedulers.io())
                .subscribe(date -> {
                    Log.v("HI", "TrueTime was initialized and we have a time: " + date);
                }, throwable -> {
                    throwable.printStackTrace();
                });
    }

    private void showValueOfPosition() {
        // Schaue nach, ob die aktuelle Permission gesetzt ist. Wenn gesetzt -> Springe drüber
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        }
        //Berechtigungen wurden zuvor schon erteilt
        else {
            makeAnythingElseWithPermission();
        }
    }

    private void makeAnythingElseWithPermission() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManagerAccelerometer = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManagerMagnetic = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManagerGravity = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerLinAccelerometerListener();
    }

    @SuppressLint("MissingPermission")
    private void registerGPSListener() {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                altitude = location.getAltitude();

                // Merke dir alle Koordinaten die rein kommen
                Service.getListOfWGSCoordinates().add(new Coordinates(latitude,longitude,altitude));

                // Setze die Geschwindigkeitsgenauigkeit, um diese im Filter nutzen zu können --> NICHT VERFÜGBAR!!
                //Service.setSpeedAccurancy_wgs(location.getSpeedAccuracyMetersPerSecond());

                // Berechne die WGS-Geschwindigkeit, auf Basis des Betrages der Geschwindigkeit:
                // Berechnung erfolgt ähnlich den kartesischen Koordinaten
                // speed_x = speed_location * sin( rad(bearing) )
                // speed_y = speed_location * cos( rad(bearing) )
                float speedOfGnss = location.hasSpeed() ? location.getSpeed() : 0;
                float bearing = location.hasBearing() ? location.getBearing() : 1;

                Service.setSpeed_x_wgs(speedOfGnss * Math.sin(Math.toRadians(bearing)));
                Service.setSpeed_y_wgs(speedOfGnss * Math.cos(Math.toRadians(bearing)));

                // Setze die locationgenauigkeit im Service, um diese später für den Filter zu verwenden
                Service.setLocationAccurancy(location.getAccuracy());

                // Als Geschwindigkeit wird zunächst die GNSS-Geschwindigkeit genutzt (in m/s)
                speed = location.hasSpeed() ? location.getSpeed() : -1;

                createGlobalPositionForDrawLatAndLon(latitude, longitude, altitude);
                writeGPSValuesToScreen(latitude, longitude, altitude, speed);
                Log.d("LH", "GPS-Update, time:  " + new Timestamp(System.currentTimeMillis()));

                // TEST
                Service.calculateCartesianPointForLastKnownPosition();
                if(Service.getListOfPoints().size() >= 1) {
                    Service.getThread().start();
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
                Toast.makeText(getApplicationContext(), "GPS is on, everything ok", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText(getApplicationContext(), "GPS is not avaiable! Activate GPS on the device", Toast.LENGTH_LONG).show();
            }
        };

        //Setze Listener bei Abweichung von 0 Meter, innerhalb von 1 sek
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_LOCATION, UPDATE_MIN_DISTANCE, locationListener);
    }

    /**
     * Erzeuge für Länge, Breite & Höhe GlobalPositions und speichere Sie im Service (wichtig für
     * das zeichnen der Koordinaten in der Ebene)
     * @param latitude
     * @param longitude
     * @param altitude
     */
    private void createGlobalPositionForDrawLatAndLon(double latitude, double longitude, double altitude) {
        GlobalPosition globalPosition = new GlobalPosition(latitude, longitude, altitude);
        Service.getListOfPositions().add(globalPosition);
        Service.setFirstGlobalPositionOfList(globalPosition);
    }

    /**
     * Frage die notwendigen Zugrifsrechte des Mobil-telefons ab
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    makeAnythingElseWithPermission();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "You must accept the GPS-use!!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void writeGPSValuesToScreen(double latitude, double longitude, double altitude, float speed) {
        TextView output_latitudeSystem = findViewById(R.id.tv_outputLatitudeSystem);
        output_latitudeSystem.setText(Double.toString(latitude));

        TextView output_longitudeSystem = findViewById(R.id.tv_outputLongitudeSystem);
        output_longitudeSystem.setText(Double.toString(longitude));

        TextView output_altitude = findViewById(R.id.tv_outputAltitude);
        output_altitude.setText(Double.toString(altitude));

        TextView output_speed = findViewById(R.id.tv_outputSpeed);

        // Setze Text, falls Geschwindigkeit nicht vorliegen sollte
        if(speed == -1){
            output_speed.setText("Speed not aviable");
        }
        else{
            output_speed.setText(Float.toString(speed));
        }
    }

    private void registerLinAccelerometerListener() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        // Sensoren für die Berechnung der Beschleunigung auf Basis des WGS84-Systems
        sensorManagerAccelerometer.registerListener(this,
                sensorManagerAccelerometer.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        sensorManagerMagnetic.registerListener(this,
                sensorManagerMagnetic.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

        sensorManagerGravity.registerListener(this,
                sensorManagerGravity.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH);

    }

    /**
     * Berechne die Geschwindigkeit aus der Linearen Beschleunigung heraus. Großes Problem ist
     * hier das Sensorrauschen, was für starke Sprünge in der Geschwindigkeitsberechnung sorgt...
     *
     * @param event
     */

    private static float[] gravityValues = null;
    private static float[] magneticValues = null;
    @Override
    public void onSensorChanged(SensorEvent event) {

        // Berechnung der Geschwindigkeit --> Aus einer früheren Version
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            // Berechne dt, mit berücksichtigung der Größen: timestamp sind nanosekunden!
            // Umrechnung ergibt eine zeitliche änderung in sekunden
            float dT = oldTimestamp == 0 ? 0.1f : (event.timestamp - oldTimestamp) / 1000000000.0f;
            oldTimestamp = event.timestamp;
            float linAccelerometer_x = event.values[0];
            //float linAccelerometer_y = event.values[1];

            // Wenn die Bildschirmaxen relativ zum gerät sind (laut developer-seite),
            // muss die z-Axe verwendet werden. Die Ache zeigt aus dem Bildschirm heraus, deshalb
            // kehren wir die Richtung mit *-1 um
            float linAccelerometer_y = event.values[2];
            //writeAccelerometerValuesToScreen(linAccelerometer_x, linAccelerometer_y);

            // Berechne auf Basis der Lin.-Beschleunigung die lineare Geschwindigkeit
            Service.calculateLinearVelocity(linAccelerometer_x, linAccelerometer_y, dT);
            velocity = Service.getLinVeloc();
            //writeVelocityValuesToScreen(velocity[0], velocity[1]);
        }

        // Berechnung der Beschleunigung im Erd-Koordinatensystem (WGS84), auf Basis der
        // Gerätebeschleunigung
        // Von: https://stackoverflow.com/a/36477630
        if ((gravityValues != null) && (magneticValues != null)
                //&& (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
                && (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)) {

            // Setze dt im Service und greife im Filter darauf zu
            Service.setDt(Service.getOldDt() == 0 ? 0.1f : (event.timestamp - Service.getOldDt()) / 1000000000.0f);
            Service.setOldDt(event.timestamp);

            float[] deviceRelativeAcceleration = new float[4];
            deviceRelativeAcceleration[0] = event.values[0];
            deviceRelativeAcceleration[1] = event.values[1];
            deviceRelativeAcceleration[2] = event.values[2];
            deviceRelativeAcceleration[3] = 0;

            // Change the device relative acceleration values to earth relative values
            // X axis -> East
            // Y axis -> North Pole
            // Z axis -> Sky

            float[] R = new float[16], I = new float[16], earthAcc = new float[16];

            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

            float[] inv = new float[16];

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
            writeAccelerometerValuesToScreen(earthAcc[0], earthAcc[1]);
            //writeAccelerometerValuesToScreen(earthAcc[1], earthAcc[2]);

            // Aktualisiere die Werte im Service
//            Service.setAccel_x_wgs(earthAcc[0]);
//            Service.setAccel_y_wgs(earthAcc[2]);
            Service.setAccel_x_wgs(earthAcc[0]);
            Service.setAccel_y_wgs(earthAcc[1]);


            //Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");
            Service.calculateLinearVelocity(earthAcc[0],earthAcc[1], (float) Service.getDt());
            //Service.calculateLinearVelocity(earthAcc[0],earthAcc[2], (float) Service.getDt());


        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values;
        }
    }


    private void writeAccelerometerValuesToScreen(float accelerometer_x, float accelerometer_y) {
        TextView output_Accelerometer_x = findViewById(R.id.tv_outputAccelerometer_X);
        output_Accelerometer_x.setText(Float.toString(accelerometer_x));

        TextView output_Accelerometer_y = findViewById(R.id.tv_outputAccelerometer_Y);
        output_Accelerometer_y.setText(Float.toString(accelerometer_y));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onPause() {
        if (lm != null) {
            lm.removeUpdates(locationListener);
            sensorManager.unregisterListener(this);
            Log.d("LH", "in Main Activity, onPause");
        }
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
        Log.d("HI", "In onResume von MainActivity");
        if (lm != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_LOCATION, UPDATE_MIN_DISTANCE, locationListener);
            Log.d("LH", "In MainActivity, onResume!");
            Log.d("LH", "LocationListener reaktiviert");
        }
        // Ist der sensorManager null, wird die app vermutlich gerade installiert, deshalb registrierung überspringen
        if(sensorManager != null) {
            registerLinAccelerometerListener();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LH", "in MainActivity, onDestroy");
    }

    /**
     * Handelt alle "Button-Aktivitäten" in der Main-Activity
     * @param view
     */
    public void inMainActivityOnButtonClick(View view) {
        // Plote Länge und Breite in Google-Maps
        if (view.getId() == R.id.btn_plotLocation) {
            Service.calculateWGSCoordinatesForAllCartesianPoints();

            String geoUri = "http://maps.google.com/maps?q=loc:"
                    + Service.getListOfWGSDestinationPoints().getLast().getLatitude() + ","
                    + Service.getListOfWGSDestinationPoints().getLast().getLongitude() + " (TADA!)";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            this.startActivity(intent);
        }

        else if (view.getId() == R.id.btn_toCoordinateScreen) {
            Toast.makeText(this,
                    "Anzahl, echte Punkte:  " + Service.getListOfPoints().size() + "\n\n" +
                            "Anzahl der geschätzten Punkte:  " + Service.getEstimatedPoints().size(),
                    Toast.LENGTH_SHORT).show();


            Service.getThread().stop();
            // Starte den Screen zum zeichnen der Breiten & Längengrade
            startActivity(new Intent(MainActivity.this, DrawLatAndLonActivity.class));
        }

        // Im Test-Modus: Setze breite und länge wie statisch gesetzt, im Test-Modus
        else if (view.getId() == R.id.btn_generateTestGNSS) {
            // Koordinaten:
            // Der erste Punkt ist vor der Uni; der zweite, dritte und vierte rechts daneben
            // (In Richtung osten ; Rathaus), jeweills mit 5m, 10m und 15m vom ersten Punkt entfernt
            // der letzte, fünfte Punkt, befindet sich in 180 grad unter Punkt 1, 10m Abstand

            latitude = new double[]{51.311996, 51.311991, 51.312006, 51.312000, 51.311902}[demoGNSSCounter];
            longitude = new double[]{9.473645, 9.473719, 9.473798, 9.473864, 9.473643}[demoGNSSCounter];
            altitude = 211;
            speed = 0f;

            if (++demoGNSSCounter > 4) {
                // Wenn counter größer als 3: mache den Button unsichtbar, also nicht drückbar
                findViewById(R.id.btn_generateTestGNSS).setVisibility(View.INVISIBLE);
            }

            createGlobalPositionForDrawLatAndLon(latitude, longitude, altitude);
            writeGPSValuesToScreen(latitude, longitude, altitude, speed);
        }

        // Starte den GNSS-Listener, also mit live-Daten
        else if (view.getId() == R.id.btn_startGNSSListener) {
            //getLocationAndRegisterGnssListener();
            registerGPSListener();
        }

        // Führe eine Schätzung der Position durch
        else if (view.getId() == R.id.btn_exportToExcel) {
//            Toast.makeText(this,
//                    "Letzter, echter Punkt:  " + Math.round(Service.getListOfPoints().getLast().getX())
//                            + " ; " +  + Math.round(Service.getListOfPoints().getLast().getY()) + "("+Service.getListOfPoints().size() +")"+ "\n\n"
//                            + "Letzter geschätzter Punkt:  "
//                            + Service.getEstimatedPoints().getLast().getX()
//                            + " ; " + Service.getEstimatedPoints().getLast().getY() + "(" + Service.getEstimatedPoints().size() +")"+ "\n\n"
//                    + "Geschätzte Geschw.:  " + Service.getEstimatedVelocity().getLast().getX() + " ; " + Service.getEstimatedVelocity().getLast().getY(),
//                    Toast.LENGTH_LONG).show();

            // Zum Test den Button zweck-entfremdet: Zeichne den Plot, indem du die jeweilige Activity "activity_test_with_..." startest
            //startActivity(new Intent(MainActivity.this, testActivityWithPlotFramework.class));

//            Toast.makeText(this, "Geschwindigkeit, x:  " + Service.getLinVeloc()[0] +
//                            "\n" + "Geschwindigkeit, y:  " + Service.getLinVeloc()[1],
//                    Toast.LENGTH_LONG).show();
            Service.getThread().stop();

//            ExcelFileCreator export = new ExcelFileCreator();
//            export.createExcelFile();
//            Toast.makeText(getApplicationContext(),"Export erfolgreich",Toast.LENGTH_LONG).show();


            CsvFileCreator export = new CsvFileCreator();
            export.exportOriginalCartesianPoints(getApplicationContext());
            Toast.makeText(getApplicationContext(),"Export erfolgreich",Toast.LENGTH_LONG).show();
            Service.getThread().start();

        }

        // Test von Zeichnungsframeworks
        else if(view.getId() == R.id.btn_testFrameworkPlot){
            //startActivity(new Intent(MainActivity.this, testActivityWithPlotFramework.class));
//            Toast.makeText(this,
//                    "Letzter, echter Punkt:  " + Math.round(Service.getListOfPoints().getLast().getX())
//                            + " ; " +  + Math.round(Service.getListOfPoints().getLast().getY()) + "("+Service.getListOfPoints().size() +")"+ "\n\n"
//                            + "Letzter geschätzter Punkt:  "
//                            + Service.getEstimatedPoints().getLast().getX()
//                            + " ; " + Service.getEstimatedPoints().getLast().getY() + "(" + Service.getEstimatedPoints().size() +")"+ "\n\n"
//                    + "Geschätzte Geschw.:  " + Service.getEstimatedVelocity().getLast().getX() + " ; " + Service.getEstimatedVelocity().getLast().getY(),
//                    Toast.LENGTH_LONG).show();

            Toast.makeText(this,
                    "Erster kartsischer Punkt:  " + Service.getListOfPoints().getFirst().getX()
                            + " ; " + Service.getListOfPoints().getFirst().getY(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
