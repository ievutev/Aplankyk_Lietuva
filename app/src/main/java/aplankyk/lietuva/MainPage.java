package aplankyk.lietuva;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainPage extends AppCompatActivity implements ListAdapter.OnDirectionClickListener, ListAdapter.OnAboutObjectClickListener, ListAdapter.OnAddToListClickListener {

    private TextView cityText;
    private static final DecimalFormat df = new DecimalFormat("0");
    private static final int location_permission = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private ProgressBar loadingProgressBar;
    private RecyclerView recyclerView;
    private ListAdapter adapter;
    Place place;

    private static final int REQUEST_CHECK_SETTINGS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);

        cityText = findViewById(R.id.city);

        ImageView mapButton = findViewById(R.id.mapButton);
        ImageView photoButton = findViewById(R.id.photoButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);
        TextView tempText = findViewById(R.id.temp);
        ImageView weatherIcon = findViewById(R.id.weatherIcon);

        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingProgressBar.setVisibility(View.VISIBLE);

        // Check network connectivity
        if (!isNetworkConnected()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            loadingProgressBar.setVisibility(View.INVISIBLE);
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize location request
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setNumUpdates(2);

        // Initialize location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Handle location updates
                    updateLocation(location, tempText, weatherIcon);
                    try {
                        fetchNearbyPlacesFromApi(location.getLatitude(), location.getLongitude());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace(); // or handle the exception as needed
                    }
                }
            }
        };
        // Request location updates
        startLocationUpdates();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));




        // Set click listeners
        mapButton.setOnClickListener(v -> startActivity(new Intent(MainPage.this, PlanTripMap.class)));
        photoButton.setOnClickListener(v -> startActivity(new Intent(MainPage.this, PhotoAlbum.class)));
        settingsButton.setOnClickListener(v -> startActivity(new Intent(MainPage.this, Settings.class)));
    }

    // Method to start location updates
    private void startLocationUpdates() {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, check location settings
            checkLocationSettings();
        } else {
            // Request location permission
            requestLocationPermission();
        }
    }

    // Method to request location permission
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                location_permission);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop receiving location updates when the activity stops
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // Handle location permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == location_permission) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check location settings
                checkLocationSettings();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Nesuteikta vietovės prieiga", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    // Method to update location
    private void updateLocation(Location location, TextView tempText, ImageView weatherIcon) {
        // Handle the received location
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                cityText.setText(city);

                // Construct weather API URL with city name
                String weatherApi_link = getWeatherApiLink(city);

                // Fetch weather data
                fetchWeatherData(weatherApi_link, tempText, weatherIcon);
            }
        } catch (IOException e) {
            e.printStackTrace();
            loadingProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    // Method to construct weather API URL
    private String getWeatherApiLink(String cityName) {
        String encodedCity = Uri.encode(cityName);
        String weatherApiKey = BuildConfig.weatherApiKey;
        return "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity + "&appid=" + weatherApiKey + "&units=metric";
    }

    // Method to fetch weather data
    private void fetchWeatherData(String weatherApiLink, TextView tempText, ImageView weatherIcon) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, weatherApiLink, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Handle the successful response
                try {
                    JSONObject jsonResponse = new JSONObject(response);

                    // Get temperature
                    JSONObject mainObject = jsonResponse.getJSONObject("main");
                    double temperature = mainObject.getDouble("temp");

                    // Get weather icon
                    JSONArray weatherArray = jsonResponse.getJSONArray("weather");
                    JSONObject weatherObject = weatherArray.getJSONObject(0);
                    String iconCode = weatherObject.getString("icon");

                    // Construct the URL for the weather icon
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    // Load the weather icon into the ImageView using Picasso
                    Picasso.get().load(iconUrl).into(weatherIcon, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            // Image loaded successfully
                            loadingProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            // Failed to load image
                            loadingProgressBar.setVisibility(View.INVISIBLE);
                            e.printStackTrace();
                        }
                    });

                    // Display temperature
                    String temperatureString = df.format(temperature) + " °C";
                    tempText.setText(temperatureString);
                    loadingProgressBar.setVisibility(View.INVISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                    loadingProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Handle errors
                Toast.makeText(MainPage.this, "Failed to fetch weather data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        });

        // Add the request to the request queue
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }



    // Method to check location settings
    private void checkLocationSettings() {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    location_permission);
            return;
        }

        // Location permission granted, proceed with location settings check
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. Start location updates
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed by showing the user a dialog
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainPage.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error
                    }
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // User enabled location, start location updates
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                // User canceled or didn't enable location, handle accordingly
                Toast.makeText(this, "Location services are required for this app to function properly.", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Ar tikrai norite uždaryti programėlę?")
                .setPositiveButton("Ne", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("Taip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainPage.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onDirectionClick(String placeName) {
        String uri = "https://www.google.com/maps/dir/?api=1&destination=" + placeName;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps"); // Specify package to ensure Google Maps is used
        startActivity(intent);
    }

    @Override
    public void onAboutObjectClick(String placeName) {
        Intent intent = new Intent(MainPage.this, SearchResultWebActivity.class);
        intent.putExtra("placeName", placeName);
        startActivity(intent);
    }

    public void onAddToListClick(Place place) {
        savePlaceToFirestore(place);
    }

    private void fetchNearbyPlacesFromApi(double latitude, double longitude) throws UnsupportedEncodingException {
        // Construct the URL for the OpenStreetMap API request to fetch nearby places
        String overpassQuery = "node['tourism'](around:15000," + latitude + "," + longitude + ");" +
                "way['tourism'](around:15000," + latitude + "," + longitude + ");" +
                "relation['tourism'](around:15000," + latitude + "," + longitude;
        String encodedQuery = URLEncoder.encode(overpassQuery, "UTF-8");
        String apiUrl = "https://overpass-api.de/api/interpreter?data=[out:json];" + encodedQuery + ");out;";

        // Initialize a new RequestQueue instance
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Parse the JSON response
                            JSONArray elements = response.getJSONArray("elements");
                            List<Place> nearbyPlaces = new ArrayList<>();

                            for (int i = 0; i < elements.length(); i++) {
                                JSONObject placeObject = elements.getJSONObject(i);
                                JSONObject tags = placeObject.getJSONObject("tags");
                                String placeName = tags.optString("name", "");
                                double latitude;
                                double longitude;
                                if (placeObject.has("lat") && placeObject.has("lon")) {
                                    latitude = placeObject.getDouble("lat");
                                    longitude = placeObject.getDouble("lon");
                                } else {
                                    latitude = 0; // Default latitude
                                    longitude = 0; // Default longitude
                                }

                                Place place = new Place(latitude, longitude, placeName); // Declare place locally
                                if (!placeName.isEmpty()) {
                                    nearbyPlaces.add(place);
                                }

                            }

                            // Save nearby places to Firestore and display them
                            saveNearbyPlacesToFirestore(nearbyPlaces);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle errors
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue
        queue.add(jsonObjectRequest);
    }

    private void savePlaceToFirestore(Place place) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userRef = db.collection("Users").document(userId);

        userRef.update("likedPlaces", FieldValue.arrayUnion(place))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainPage.this, "Objektas pridėtas į Jūsų sąrašą", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainPage.this, "Nepavyko pridėti objekto į Jūsų sąrašą", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveNearbyPlacesToFirestore(List<Place> nearbyPlaces) {
        // Assuming you have initialized your Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get the current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Reference to the user document in Firestore
        DocumentReference userRef = db.collection("Users").document(userId);

        // Use a transaction to delete existing personalizedData and add new data
        db.runTransaction(transaction -> {
                    // Get the current data
                    DocumentSnapshot snapshot = transaction.get(userRef);

                    // Check if personalizedData field exists
                    if (snapshot.contains("personalizedData")) {
                        // Delete the personalizedData field
                        transaction.update(userRef, "personalizedData", FieldValue.delete());
                    }

                    // Add new data
                    transaction.update(userRef, "personalizedData", nearbyPlaces);

                    // Return the new data for this transaction
                    return nearbyPlaces;
                })
                .addOnSuccessListener(result -> {
                    // Handle success if needed
                })
                .addOnFailureListener(e -> {
                    // Handle failure if needed
                });

        displayNearbyPlaces();
    }


    private void displayNearbyPlaces() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve personalized data from Firestore document
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            List<Place> personalizedData = user.getPersonalizedData();
                            if (personalizedData != null) {
                                // Create and set adapter with personalized data
                                adapter = new ListAdapter(this, personalizedData, false);
                                recyclerView.setAdapter(adapter);
                                adapter.setOnDirectionClickListener(this);
                                adapter.setOnAboutObjectClickListener(this);
                                adapter.setOnAddToListClickListener(this);
                            } else {
                                Log.d(TAG, "Personalized data is null");
                            }
                        } else {
                            Log.d(TAG, "User object is null");
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting personalized data", e);
                });
    }

}
