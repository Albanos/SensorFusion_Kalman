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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import geodesy.GlobalPosition;
import model.EstimationFilter;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final int PERMISSION_REQUEST = 0;

    private double latitude;
    private double longitude;
    private double altitude;

    private LocationManager lm;
    private LocationListener locationListener;
    private SensorManager sensorManager;

    private EstimationFilter filter = EstimationFilter.getInstance();

    // Zum test: speichere alte position und berechne differenz nur bei änderung
    private GlobalPosition globalPosition_old = null;
    private GlobalPosition globalPosition;

    // Update-Zeit: 5s
    private final int UPDATE_TIME_LOCATION = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //filter.tmp();
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

    public void makeAnythingElseWithPermission() {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Frage bei Änderung von 1m alle 5sek ab -->MERKE: kann auch null zurück liefern!!!
        registerGPSListener();

        //Prüfe, ob GPS aktiviert ist. Agiere nur dann, sonst Toast ausgeben
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            @SuppressLint("MissingPermission") Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();

                writeGPSValuesToScreen(latitude, longitude);

            }

        } else {
            Toast.makeText(this, "GPS is not avaiable! Activate GPS on the device and restart the application", Toast.LENGTH_LONG).show();
            finish();
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        registerLinAccelerometerListener();

    }

    @SuppressLint("MissingPermission")
    private void registerGPSListener() {
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                longitude = location.getLongitude();
                latitude = location.getLatitude();
                altitude = location.getAltitude();

                filter.setPositionValues(latitude, longitude, altitude);
                globalPosition = filter
                        .calculateCoordinatesOnLatLon(latitude, longitude, altitude);

                // Zum test: wenn positionen unterschiedlich, dann gab es location-update.
                // Berechne darum Abstand und winkel zwischen beiden positionen
                if (! globalPosition.equals(globalPosition_old)) {
                    double dist = filter.coordinateDistanceBetweenTwoPoints(globalPosition, globalPosition_old);

                    if (globalPosition_old != null) {
                        Log.d("LH", "Distanz zwischen " + globalPosition.getLatitude() + " , " + globalPosition.getLongitude()
                                + " & " + globalPosition_old.getLatitude() + " , " + globalPosition_old.getLongitude() + "ist:  "
                                + dist);
                    }
                    globalPosition_old = globalPosition;
                }

                writeGPSValuesToScreen(latitude, longitude);

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

        //Setze Listener bei Abweichung von einem Meter, innerhalb von 5 sek
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_LOCATION, 1, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void writeGPSValuesToScreen(double latitude, double longitude) {

        TextView output_latitudeSystem = (TextView) findViewById(R.id.tv_outputLatitudeSystem);
        output_latitudeSystem.setText(Double.toString(latitude));

        TextView output_longitudeSystem = (TextView) findViewById(R.id.tv_outputLongitudeSystem);
        output_longitudeSystem.setText(Double.toString(longitude));

    }

    public void onPlotLocationClick(View view) {

        String geoUri = "http://maps.google.com/maps?q=loc:" + latitude + "," + longitude + " (TADA!)";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        this.startActivity(intent);
    }


    private void registerLinAccelerometerListener() {
        sensorManager.registerListener((SensorEventListener) this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //updateOrientationAngles();
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float linAccelerometer_x = event.values[0];
            float linAccelerometer_y = event.values[1];

            // Reiche Werte an Filter weiter
            filter.setLinAccelerometerValues(linAccelerometer_x, linAccelerometer_y);

            writeAccelerometerValuesToScreen(linAccelerometer_x, linAccelerometer_y);

            float[] velocity = filter.getLinVeloc();
            writeVelocityValuesToScreen(velocity[0], velocity[1]);

        }
    }

    private void writeVelocityValuesToScreen(float vel_x, float vel_y) {
        TextView output_vel_x = (TextView) findViewById(R.id.tv_outputVelocity_X);
        output_vel_x.setText(Float.toString(vel_x));

        TextView output_vel_y = (TextView) findViewById(R.id.tv_outputVelocity_Y);
        output_vel_y.setText(Float.toString(vel_y));
    }


    private void writeAccelerometerValuesToScreen(float accelerometer_x, float accelerometer_y) {
        TextView output_Accelerometer_x = (TextView) findViewById(R.id.tv_outputAccelerometer_X);
        output_Accelerometer_x.setText(Float.toString(accelerometer_x));

        TextView output_Accelerometer_y = (TextView) findViewById(R.id.tv_outputAccelerometer_Y);
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
            Log.d("LH", "in onPause");
        }
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();

        if (lm != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_TIME_LOCATION, 1, locationListener);
        }
        registerLinAccelerometerListener();
    }

    /**
     * Rufe nach click auf den entsprechenden Button den Scrren zum Zeichnen von Latitude und
     * Longitude auf. Übergebe Länge, breite und Höhe an den Screen, via intent
     *
     * @param view
     */
    public void onToCoordinateScreenClick(View view) {
        Intent intent = new Intent(MainActivity.this, DrawLatAndLon.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("altitude", altitude);

        startActivity(intent);
    }

}
