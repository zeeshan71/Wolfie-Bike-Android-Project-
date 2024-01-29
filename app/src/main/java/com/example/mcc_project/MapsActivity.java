package com.example.mcc_project;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
//import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;

import android.Manifest;

import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mcc_project.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private TextView text2, emergencyText;
    int LOCATION_REQUEST_CODE = 10001;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    Marker userLocationMarker;
    FirebaseDatabase fAuth;
    DatabaseReference reference;
    FirebaseFirestore firebaseFirestore;
    ProgressDialog progressDialog;
    Button reserveButton;
    List<CycleStand> cycleStandList = new ArrayList<>();
    HashMap<String, Integer> map = new HashMap<>();
    State state = new State();
    EmergencyNotification emergencyNotification = new EmergencyNotification();

    static String currentLatitude;
    static String currentLongitude;


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                Log.d(TAG, "onLocationResult: " + location.toString());
                if (mMap != null) {
                    setUserLocationMarker(locationResult.getLastLocation());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        text2 = findViewById(R.id.idTVtextTwo);

        fAuth = FirebaseDatabase.getInstance();

        progressDialog = new ProgressDialog(this);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();

        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            getLastLocation();
            checkSettingsAndStartLocationUpdates();
        } else {
            askLocationPermission();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void setUserLocationMarker(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentLatitude  = String.valueOf(location.getLatitude());
        currentLongitude  = String.valueOf(location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);

        if (state.isCycleReserved()) {

            if (userLocationMarker == null) {
                //WE CREATE A NEW MARKER

                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.cycleafterres));

                markerOptions.rotation(location.getBearing());
                userLocationMarker = mMap.addMarker(markerOptions);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            } else {
                //USE PREVIOUSLY CREATED MARKER
                userLocationMarker.setPosition(latLng);
                userLocationMarker.setRotation(location.getBearing());
            }
        }
    }

    private void checkSettingsAndStartLocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Settings of device are satisfied and can start location updates
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(MapsActivity.this, 1001); //////////CHECK WITH FAIZ
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                        //throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();

        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    //WE HAVE A LOCATION
                    Log.d(TAG, "onSuccess: " + location.toString());
                    Log.d(TAG, "onSuccess: " + location.getLatitude());
                    Log.d(TAG, "onSuccess: " + location.getLongitude());
                } else {
                    Log.d(TAG, "onSuccess: Location was null...");
                }
            }
        });
        locationTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: " + e.getLocalizedMessage());
            }
        });
    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "askLocationPermission: You Should Show an Alert Dialog...");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //PERMISSION GRANTED
//                getLastLocation();
                checkSettingsAndStartLocationUpdates();
            } else {
                //PERMISSION NOT GRANTED
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        fetchData();
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Object markerData = marker.getTag();
                if (markerData != null) {
                    String cycleStandName = markerData.toString();
                    displayBottomSheet(cycleStandName);
                }
                return true;
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

    private void displayBottomSheet(String cycleStandName) {

        // creating a variable for our bottom sheet dialog.
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_layout);


        // passing a layout file for our bottom sheet dialog.
        ImageView image1 = bottomSheetDialog.findViewById(R.id.idIVimage);
        TextView text1 = bottomSheetDialog.findViewById(R.id.idTVtext);
        TextView text2 = bottomSheetDialog.findViewById(R.id.idTVtextTwo);

        Drawable res = getResources().getDrawable(R.drawable.cycleimage);
        image1.setImageDrawable(res);

        int availableQuantity = map.get(cycleStandName);

        text1.setText(cycleStandName + " Bike Stand");
        text2.setText(availableQuantity + " Cycles Available");

        reserveButton = bottomSheetDialog.findViewById(R.id.reserveBtn);
        if (!state.isCycleReserved) {
            reserveButton.setText("Reserve Cycle");
        } else {
            reserveButton.setText("Drop Off Cycle");
        }
        reserveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Step 4 : On Clicking Reserve Button :
                //i) Check count cycle count >0
                //ii) If true, then decrease count in DB by 1
                //iii) Maintain State that the cycle is reserved
                //iv) Show Timer
                //v) Change Bottom Sheet layout (Drop Cycle)
                System.out.println("Reserve Button is Clicked");
                System.out.println("Available Quantity : " + availableQuantity);
                if (!state.isCycleReserved) {
                    if (availableQuantity == 0) {
                        Toast.makeText(MapsActivity.this, "No Cycle in current location", Toast.LENGTH_LONG).show();
                    }
                    if (availableQuantity > 0) {
                        reserveCycle();
                        updateDb(cycleStandName, availableQuantity-1, "pickup");
                        progressDialog.setTitle("Picking Cycle...");
                        progressDialog.show();
                        showTimer();
                        showCycleOnMap(); // get current location from user and update cycle marker on that map position

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                bottomSheetDialog.dismiss();
                                Toast.makeText(MapsActivity.this, "Cycle is Reserved", Toast.LENGTH_LONG).show();
                            }
                        }, 1000);
                    }
                } else {
                    dropCycle();
                    progressDialog.setTitle("Dropping Cycle...");
                    progressDialog.show();
                    updateDb(cycleStandName, availableQuantity+1, "drop");
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            bottomSheetDialog.dismiss();
                            Toast.makeText(MapsActivity.this, "Cycle is Dropped", Toast.LENGTH_LONG).show();
                        }
                    }, 1000);

                }
            }
        });

        emergencyText = bottomSheetDialog.findViewById(R.id.emergencyID);
        emergencyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MapsActivity.this, "ADMIN is notified!", Toast.LENGTH_LONG).show();
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                String currentUser = sharedPreferences.getString("currentUser", "");
                String phone = sharedPreferences.getString("phone", "");
                String email = sharedPreferences.getString("email", "");
                emergencyNotification.setAlertInDatabase(currentUser, currentLatitude, currentLongitude, phone, email);
            }
        });
        bottomSheetDialog.show();
    }

    private void dropCycle() {
        state.setCycleReserved(false);
    }

    private void showCycleOnMap() {

    }

    private void showTimer() {
    }

    private void updateDb(String cycleStandName, int availableQuantity, String mode) {
        updateCycleQuantity(cycleStandName, availableQuantity);
        addTransactionLogs(cycleStandName, mode);
    }

    private void addTransactionLogs(String cycleStandName, String mode) {

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String currentUser = sharedPreferences.getString("currentUser", "");
        System.out.println("currentUser : " + currentUser);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users/" + currentUser);

        // Generate a new unique child key for the transaction node
        String transactionKey = userRef.child("transactionList").push().getKey();

        // Create a new map to hold the values of the "location", "mode", and "timestamp" fields
        Map<String, Object> transactionValues = new HashMap<>();
        transactionValues.put("location", cycleStandName);
        transactionValues.put("mode", mode);
        transactionValues.put("timestamp", System.currentTimeMillis());
        //adding dateTime only for demo purposes, not required in DB, timestamp is sufficient
        transactionValues.put("dateTime", DateFormat.getDateTimeInstance().format(new Date(System.currentTimeMillis())));

        // Set the values of the "location", "mode", and "timestamp" fields for the new child node
        Task<Void> setValueTask = userRef.child("transactionList").child(transactionKey).setValue(transactionValues);

// Add success and failure listeners to the task
        setValueTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Handle successful write operation
                System.out.println("Transaction is success");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle failed write operation
                System.out.println("Transaction has failed");
            }
        });

    }

    private void updateCycleQuantity(String cycleStandName, int availableQuantity) {
        DatabaseReference cyclesRef = FirebaseDatabase.getInstance().getReference("cycles");
        Map<String, Object> updates = new HashMap<>();
        String path = cycleStandName + "/" + "quantity";
        System.out.println("The path " + path);
        updates.put(path, availableQuantity);




        cyclesRef.updateChildren(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // The values were successfully updated
                        System.out.println("Value update is success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There was an error updating the values
                        System.out.println("Value update is Failed");
                    }
                });
    }

    private void reserveCycle() {
        state.setCycleReserved(true);
    }

    public void fetchData() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("cycles");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    CycleStand cycleStand = new CycleStand();
                    String key = snapshot.getKey();
                    cycleStand.setName(key);
                    Object value = snapshot.getValue();

                    if (value instanceof Map) {
                        Map<String, Object> mapValue = (Map<String, Object>) value;
                        for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                            String childKey = entry.getKey();
                            Object childValue = entry.getValue();
                            if (childKey.equals("quantity")) {
                                cycleStand.setQuantity(Integer.parseInt(childValue.toString()));
                            }
                            if (childKey.equals("latitude")) {
                                cycleStand.setLatitude(childValue.toString());
                            }
                            if (childKey.equals("longitude")) {
                                cycleStand.setLongitude(childValue.toString());
                            }
                        }
                    }
                    cycleStandList.add(cycleStand);
                    map.put(key, cycleStand.getQuantity());
                    createCycleMap(cycleStandList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void createCycleMap(List<CycleStand> cycleStandList) {
        for (CycleStand cycleStand : cycleStandList) {
            LatLng coordinates = new LatLng(Double.parseDouble(cycleStand.getLatitude()), Double.parseDouble(cycleStand.getLongitude()));
            String cycleStandName = cycleStand.getName();
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(coordinates)
                    .title(cycleStandName);

            Marker marker = mMap.addMarker(markerOptions);
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cycleimage));
            marker.setTag(cycleStandName);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 17));
        }
    }
}