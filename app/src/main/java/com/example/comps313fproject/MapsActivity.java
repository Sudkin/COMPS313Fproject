package com.example.comps313fproject;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference locationRef;
    private String userId;
    private long todayStartTimeMillis;
    private List<LocationData> todayLocationDataList = new ArrayList<>();
    private PolylineOptions polylineOptions;
    private Polyline polyline;
    private CalendarView calendarView;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://childlink-b5304-default-rtdb.asia-southeast1.firebasedatabase.app");
        locationRef = database.getReference("Users").child(userId).child("location");

        todayStartTimeMillis = getTodayStartTimeMillis();
        polylineOptions = new PolylineOptions().color(getResources().getColor(R.color.polyline_color, getTheme())).width(10);
        calendarView = findViewById(R.id.calendarView);

        // 初始化 Geocoder，使用預設 Locale
        geocoder = new Geocoder(this, Locale.getDefault());

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                long selectedStartTimeMillis = getStartTimeMillisForDate(year, month, dayOfMonth);
                long selectedEndTimeMillis = selectedStartTimeMillis + 24 * 60 * 60 * 1000 - 1;
                setupFirebaseListener(selectedStartTimeMillis, selectedEndTimeMillis);
                Toast.makeText(MapsActivity.this, "Selected date: " + year + "-" + (month + 1) + "-" + dayOfMonth, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private long getTodayStartTimeMillis() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getStartTimeMillisForDate(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));

        setupFirebaseListenerForToday();
    }

    private void setupFirebaseListenerForToday() {
        setupFirebaseListener(todayStartTimeMillis, todayStartTimeMillis + 24 * 60 * 60 * 1000 - 1);
    }

    private void setupFirebaseListener(long startTimeMillis, long endTimeMillis) {
        locationRef.orderByChild("timestamp")
                .startAt(startTimeMillis)
                .endBefore(endTimeMillis)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        todayLocationDataList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                double latitude = snapshot.child("latitude").getValue(Double.class);
                                double longitude = snapshot.child("longitude").getValue(Double.class);
                                Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                                if (timestamp != null && timestamp >= startTimeMillis && timestamp < endTimeMillis) {
                                    todayLocationDataList.add(new LocationData(latitude, longitude, timestamp));
                                }
                            } catch (Exception e) {
                                Log.e("FirebaseDataError", "Error parsing location data: ", e);
                                Toast.makeText(MapsActivity.this, "Error parsing location data.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        updateMapPolyline();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("FirebaseError", "Failed to read location data.", databaseError.toException());
                        Toast.makeText(MapsActivity.this, "Failed to load location data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateMapPolyline() {
        if (mMap == null) return;

        mMap.clear();
        polyline = null;

        if (!todayLocationDataList.isEmpty()) {
            List<LatLng> todayCoordinates = new ArrayList<>();
            polylineOptions.addAll(todayCoordinates);

            int markerInterval = 1;

            for (int i = 0; i < todayLocationDataList.size(); i++) {
                LocationData locationData = todayLocationDataList.get(i);
                LatLng point = new LatLng(locationData.latitude, locationData.longitude);
                todayCoordinates.add(point);

                if (i % markerInterval == 0 || i == todayLocationDataList.size() - 1 || i == 0) {
                    addIntermediateMarker(locationData, point, i);
                }
            }

            polylineOptions.addAll(todayCoordinates);
            polyline = mMap.addPolyline(polylineOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(todayCoordinates.get(0), 15));
        } else {
            Toast.makeText(MapsActivity.this, "No location data for selected date.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void addStartAndEndMarkers() {
        if (todayLocationDataList.isEmpty()) return;

        LocationData startLocationData = todayLocationDataList.get(0);
        LocationData endLocationData = todayLocationDataList.get(todayLocationDataList.size() - 1);

        LatLng startPoint = new LatLng(startLocationData.latitude, startLocationData.longitude);
        LatLng endPoint = new LatLng(endLocationData.latitude, endLocationData.longitude);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        String startTimeText = sdf.format(new Date(startLocationData.timestamp));
        String endTimeText = sdf.format(new Date(endLocationData.timestamp));


        MarkerOptions startMarkerOptions = new MarkerOptions()
                .position(startPoint)
                .title("StartPoint")
                .snippet("Time: " + startTimeText);
        mMap.addMarker(startMarkerOptions);


        MarkerOptions endMarkerOptions = new MarkerOptions()
                .position(endPoint)
                .title("EndPoint")
                .snippet("Time: " + endTimeText);
        mMap.addMarker(endMarkerOptions);
    }

    @SuppressLint("SimpleDateFormat")
    private void addIntermediateMarker(LocationData locationData, LatLng point, int index) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String timeText = sdf.format(new Date(locationData.timestamp));

        Executors.newSingleThreadExecutor().execute(() -> {
            String addressString = "Location checking...";
            try {
                if (geocoder != null) {
                    List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);

                        addressString = address.getAddressLine(0);
                        if (addressString == null) {
                            addressString = "getAddressLine(0) 返回 null";
                        } else {
                            Log.d("AddressString", "Success (getAddressLine(0)): " + addressString);
                        }
                    } else {
                        addressString = "Geocoder Empty";
                        Log.d("AddressString", "Geocoder Empty");
                    }
                } else {
                    addressString = "Geocoder fail";
                    Log.d("AddressString", "Geocoder fail");
                }
            } catch (IOException e) {
                Log.e("GeocodingError", "IOException in getFromLocation()", e);
                addressString = "fail (IOException)";
                Log.d("AddressString", "fail (IOException): " + e.getMessage());
            } catch (IllegalArgumentException e) {
                Log.e("GeocodingError", "IllegalArgumentException: " + point.latitude + ", " + point.longitude, e);
                addressString = "Error (IllegalArgumentException)";
                Log.d("AddressString", "Error (IllegalArgumentException): " + e.getMessage());
            }

            String finalAddressString = addressString;
            runOnUiThread(() -> {

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(point)
                        .title("Location #" + (index + 1))
                        .snippet("Time: " + timeText + "\n" + "Address: " + finalAddressString);

                mMap.addMarker(markerOptions);
            });
        });
    }

    class LocationData {
        public double latitude;
        public double longitude;
        public long timestamp;

        public LocationData() {
        }

        public LocationData(double latitude, double longitude, long timestamp) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }
    }
}