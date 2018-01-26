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

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private final int PERMISSION_REQUEST = 0;

    private double latitude;
    private double longitude;
    private double accelerometer_x;
    private double accelerometer_y;
    private double accelerometer_z;

    private float accurancyOfDevice;
    private float speedOfDevice;

    private LocationManager lm;
    private LocationListener locationListener;
    private SensorManager sensorManager;
    private Sensor sensor;

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

            // Sollten wir ein Pop-up für die Permission zeigen?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST);
            }
        }
        //Berechtigungen wurden zuvor schon erteilt
        else {
            makeAnythingElseWithPermission();
        }

    }

    @SuppressLint("MissingPermission")
    private void registerGPSListener() {
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {

                longitude = location.getLongitude();
                latitude = location.getLatitude();
                accurancyOfDevice = location.getAccuracy();
                speedOfDevice = location.getSpeed();
                Log.d("LH", "In update GPS-Listener");
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

        //Setze Listener bei Abweichung von einem Meter, innerhalb von 10 sek
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 1, locationListener);
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
        //String uri = String.format(Locale.ENGLISH, "geo:%f,%f", latitude, longitude);
        //String uri = String.format(Locale.ENGLISH, "geo:0,0");
        //String uri = "http://maps.google.com/maps?saddr=" + latitude + "," + longitude;
        //String uri = "http://maps.google.com/maps?daddr=51.3160691,9.3912614";
        //String uri = String.format(Locale.ENGLISH, "geo:51.3160691,9.3912614");
        //String uri = String.format(Locale.ENGLISH, "geo:,%f", latitude, longitude);
        //String uri = "geo:"+latitude+","+longitude;
        String geoUri = "http://maps.google.com/maps?q=loc:" + latitude + "," + longitude + " (TADA!)";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        this.startActivity(intent);
    }

    public void makeAnythingElseWithPermission() {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Frage bei Änderung von 1m alle 10 sek ab -->MERKE: kann auch null zurück liefern!!!
        registerGPSListener();

        //Prüfe, ob GPS aktiviert ist. Agiere nur dann, sonst Toast ausgeben
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            @SuppressLint("MissingPermission") Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                accurancyOfDevice = location.getAccuracy();
                speedOfDevice = location.getSpeed();

                writeGPSValuesToScreen(latitude, longitude);

            }

        } else {
            Toast.makeText(this, "GPS is not avaiable! Activate GPS on the device and restart the application", Toast.LENGTH_LONG).show();
            finish();
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        registerAccelerometerListener();


    }

    private void registerAccelerometerListener(){
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION){
            accelerometer_x=event.values[0];
            accelerometer_y=event.values[1];
            accelerometer_z=event.values[2];

            writeAccelerometerValuesToScreen(accelerometer_x,accelerometer_y,accelerometer_z);
        }
    }

    private void writeAccelerometerValuesToScreen(double accelerometer_x, double accelerometer_y, double accelerometer_z) {
        TextView output_Accelerometer_x = (TextView) findViewById(R.id.tv_outputAccelerometer_X);
        output_Accelerometer_x.setText(Double.toString(accelerometer_x));

        TextView output_Accelerometer_y = (TextView) findViewById(R.id.tv_outputAccelerometer_Y);
        output_Accelerometer_y.setText(Double.toString(accelerometer_y));

        TextView output_Aceelerometer_z = findViewById(R.id.tv_outputAccelerometer_Z);
        output_Aceelerometer_z.setText(Double.toString(accelerometer_z));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPause() {
        if(lm != null) {
            lm.removeUpdates(locationListener);
            Log.d("LH", "in onPause");
        }
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        if(lm != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 1, locationListener);
        }
        super.onResume();
    }
}
