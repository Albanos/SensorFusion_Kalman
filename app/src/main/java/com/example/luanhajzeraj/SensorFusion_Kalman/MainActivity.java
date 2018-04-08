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

import geodesy.GlobalPosition;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private double latitude;
    private double longitude;
    private double altitude;
    private float[] velocity = {0, 0};
    private LocationManager lm;
    private LocationListener locationListener;
    private SensorManager sensorManager;
    private EstimationFilter filter = EstimationFilter.getInstance();
    // Update-Zeit: 1s
    private final int UPDATE_TIME_LOCATION = 1000;
    private final int PERMISSION_REQUEST = 0;
    private static int demoGNSSCounter = 0;
    private static long oldTimestamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showValueOfPosition();
    }

    private void showValueOfPosition() {
        // Schaue nach, ob die aktuelle Permission gesetzt ist. Wenn gesetzt -> Springe drüber
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST);
        }
        //Berechtigungen wurden zuvor schon erteilt
        else {
            makeAnythingElseWithPermission();
        }
    }

    private void makeAnythingElseWithPermission() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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

//                filter.setPositionValues(latitude, longitude, altitude);
                createGlobalPositionForDrawLatAndLon(latitude, longitude, altitude);
                writeGPSValuesToScreen(latitude, longitude, altitude);
                Log.d("LH", "GPS-Update");
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
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_LOCATION, 0, locationListener);
    }

    private void createGlobalPositionForDrawLatAndLon(double latitude, double longitude, double altitude) {
        GlobalPosition globalPosition = new GlobalPosition(latitude, longitude, altitude);
        Service.getListOfPositions().add(globalPosition);
        Service.setFirstGlobalPositionOfList(globalPosition);
    }

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

    private void writeGPSValuesToScreen(double latitude, double longitude, double altitude) {
        TextView output_latitudeSystem = findViewById(R.id.tv_outputLatitudeSystem);
        output_latitudeSystem.setText(Double.toString(latitude));

        TextView output_longitudeSystem = findViewById(R.id.tv_outputLongitudeSystem);
        output_longitudeSystem.setText(Double.toString(longitude));

        TextView output_altitude = findViewById(R.id.tv_outputAltitude);
        output_altitude.setText(Double.toString(altitude));
    }

    private void registerLinAccelerometerListener() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float dT = oldTimestamp == 0 ? 0.1f : (event.timestamp - oldTimestamp) / 1000000000.0f;
            oldTimestamp = event.timestamp;

            float linAccelerometer_x = event.values[0];
            float linAccelerometer_y = event.values[1];
            writeAccelerometerValuesToScreen(linAccelerometer_x, linAccelerometer_y);

            // Reiche Werte an Filter weiter
            filter.calculateLinearVelocity(linAccelerometer_x, linAccelerometer_y, dT);
            velocity = filter.getLinVeloc();
            writeVelocityValuesToScreen(velocity[0], velocity[1]);
        }
    }

    private void writeVelocityValuesToScreen(float vel_x, float vel_y) {
        TextView output_vel_x = findViewById(R.id.tv_outputVelocity_X);
        output_vel_x.setText(Float.toString(vel_x));

        TextView output_vel_y = findViewById(R.id.tv_outputVelocity_Y);
        output_vel_y.setText(Float.toString(vel_y));
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
        if (lm != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_LOCATION, 1, locationListener);
            Log.d("LH", "In MainActivity, onResume!");
            Log.d("LH", "LocationListener reaktiviert");
        }
        registerLinAccelerometerListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LH", "in MainActivity, onDestroy");
    }

    public void inMainActivityOnButtonClick(View view) {
        // Plote Länge und Breite in Google-Maps
        if (view.getId() == R.id.btn_plotLocation) {
            String geoUri = "http://maps.google.com/maps?q=loc:" + latitude + "," + longitude + " (TADA!)";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            this.startActivity(intent);
        } else if (view.getId() == R.id.btn_toCoordinateScreen) {
            // Starte den Screen zum zeichnen der Breiten & Längengrade
            startActivity(new Intent(MainActivity.this, DrawLatAndLonActivity.class));
        }

        // Setze die Geschwindigkeit in x- und y-Richtung zurück
        else if (view.getId() == R.id.btn_resetVelocity) {
            velocity[0] = 0.0f;
            velocity[1] = 0.0f;
            filter.setLinVeloc(velocity);
            writeVelocityValuesToScreen(velocity[0], velocity[1]);
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

            if (++demoGNSSCounter > 4) {
                // Wenn counter größer als 3: mache den Button unsichtbar, also nicht drückbar
                findViewById(R.id.btn_generateTestGNSS).setVisibility(View.INVISIBLE);
            }

            createGlobalPositionForDrawLatAndLon(latitude, longitude, altitude);
            writeGPSValuesToScreen(latitude, longitude, altitude);
        }

        // Starte den GNSS-Listener, also mit live-Daten
        else if (view.getId() == R.id.btn_startGNSSListener) {
            //getLocationAndRegisterGnssListener();
            registerGPSListener();
        }
    }

    double[] getDemoCoordinates() {
        double[] d = new double[]{
                51.3120000 + (demoGNSSCounter * .00000666), // latitude
                9.4728500 + (demoGNSSCounter * .00000666) // longitude
        };
        demoGNSSCounter++;
        return d;
    }
}
