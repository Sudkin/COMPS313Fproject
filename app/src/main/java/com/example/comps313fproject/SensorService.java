package com.example.comps313fproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SensorService extends Service {

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;

    private static final double FALL_THRESHOLD = 20.0;
    private static final long SMS_SEND_INTERVAL = 5000; // 簡訊間隔5秒
    private long lastSmsSentTime = 0;
    private String emergencyContactPhoneNumber = "";

    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private Handler locationUpdateHandler;
    private Runnable locationUpdateRunnable;
    private static final long LOCATION_UPDATE_INTERVAL = 600000; // 10 分鐘更新一次位置
    private DatabaseReference dbRef;


    @Override
    public void onCreate() {
        super.onCreate();


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer == null) {
            stopSelf();
            return;
        }

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float ax = event.values[0];
                float ay = event.values[1];
                float az = event.values[2];
                double svm = Math.sqrt(ax * ax + ay * ay + az * az);

                if (svm > FALL_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSmsSentTime >= SMS_SEND_INTERVAL) {
                        lastSmsSentTime = currentTime;
                        Toast.makeText(SensorService.this, "Fall detection", Toast.LENGTH_SHORT).show();
                        getEmergencyContactAndSendSMS();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Detecting")
                .setContentText("Detecting and Monitoring...")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        startForeground(NOTIFICATION_ID, notification);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationUpdateHandler = new Handler();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            dbRef = FirebaseDatabase.getInstance("https://childlink-b5304-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Users").child(userId);
            checkBatteryOptimizationWhitelist();
            startLocationUpdates();
        }

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
        stopForeground(true);
        stopLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void getEmergencyContactAndSendSMS() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance("https://childlink-b5304-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Users").child(userId).child("emergencyContact");

            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        emergencyContactPhoneNumber = snapshot.getValue(String.class);
                        if (emergencyContactPhoneNumber != null && !emergencyContactPhoneNumber.isEmpty()) {
                            sendSMS(emergencyContactPhoneNumber, "Fall detected");
                        }
                    } else {
                        Toast.makeText(SensorService.this, "No Number", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(SensorService.this, "No Number" + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

    }


    private void sendSMS(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            CallSMS callSMS = new CallSMS(this);
            callSMS.sendSMS(phoneNumber, message);
        } else {
            Toast.makeText(this, "No permission", Toast.LENGTH_LONG).show();
        }
    }


    private void startLocationUpdates() {
        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                getLocationAndUpdateDatabase();
                locationUpdateHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        };
        locationUpdateHandler.postDelayed(locationUpdateRunnable, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates() {
        if (locationUpdateHandler != null && locationUpdateRunnable != null) {
            locationUpdateHandler.removeCallbacks(locationUpdateRunnable);
        }
    }


    @SuppressLint("MissingPermission")
    private void getLocationAndUpdateDatabase() {
        Log.d("SensorService", "getLocationAndUpdateDatabase: Starting location retrieval (requestLocationUpdates)");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SensorService", "getLocationAndUpdateDatabase: Location permission NOT granted");
            return;
        }
        Log.d("SensorService", "getLocationAndUpdateDatabase: Location permission IS granted");

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30* 60 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        locationRequest.setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w("SensorService", "requestLocationUpdates: onLocationResult: LocationResult is null"); // Added Log
                    Log.w("SensorService", " (requestLocationUpdates)");
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d("SensorService", "requestLocationUpdates: onLocationResult: Location retrieved successfully"); // Added Log
                    updateLocationInDatabase(location);
                    fusedLocationClient.removeLocationUpdates(this);
                } else {
                    Log.w("SensorService", "requestLocationUpdates: onLocationResult: Location is null"); // Added Log
                    Log.w("SensorService", " (requestLocationUpdates)");
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Log.d("SensorService", "getLocationAndUpdateDatabase: requestLocationUpdates() called"); // Added Log
    }

    private void updateLocationInDatabase(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("timestamp", timestamp);

        if (dbRef != null) {
            DatabaseReference locationRef = dbRef.child("location").push();
            locationRef.setValue(locationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("SensorService", "Location Updated " + latitude + ", " + longitude);

                    })
                    .addOnFailureListener(e -> {
                        Log.e("SensorService", "Location Update failed" + e.getMessage());

                    });
        }


    }


    private void checkBatteryOptimizationWhitelist() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {

                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        }
    }


}
