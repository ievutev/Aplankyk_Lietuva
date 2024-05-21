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
            Toast.makeText(this, "Nėra interneto ryšio", Toast.LENGTH_SHORT).show();
            loadingProgressBar.setVisibility(View.INVISIBLE);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Requesting location
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setNumUpdates(2);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocation(location, tempText, weatherIcon);
                    try {
                        fetchNearbyPlacesFromApi(location.getLatitude(), location.getLongitude());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        startLocationUpdates();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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

    // Checking if location request was successful
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == location_permission) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                Toast.makeText(this, "Nesuteikta vietovės prieiga", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    // Method to update location
    private void updateLocation(Location location, TextView tempText, ImageView weatherIcon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                cityText.setText(city);

                String weatherApi_link = getWeatherApiLink(city);

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
                try {
                    JSONObject jsonResponse = new JSONObject(response);

                    // Getting temperature
                    JSONObject mainObject = jsonResponse.getJSONObject("main");
                    double temperature = mainObject.getDouble("temp");

                    // Getting weather icon
                    JSONArray weatherArray = jsonResponse.getJSONArray("weather");
                    JSONObject weatherObject = weatherArray.getJSONObject(0);
                    String iconCode = weatherObject.getString("icon");

                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    // Load the weather icon into the ImageView using Picasso
                    Picasso.get().load(iconUrl).into(weatherIcon, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            loadingProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            loadingProgressBar.setVisibility(View.INVISIBLE);
                        }
                    });

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
                Toast.makeText(MainPage.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    // Method to check location settings
    private void checkLocationSettings() {
        // Checking for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, requesting
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    location_permission);
            return;
        }

        // Location permission granted, proceeding with location settings check
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainPage.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
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
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                Toast.makeText(this, "Reikalinga vietovės prieiga", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    // Method for systems "back" button pressed
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

    // Checking if network is connected
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Method to handle "direction" button click
    @Override
    public void onDirectionClick(String placeName) {
        String uri = "https://www.google.com/maps/dir/?api=1&destination=" + placeName;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps"); // Specify package to ensure Google Maps is used
        startActivity(intent);
    }

    // Method to handle "about" button click
    @Override
    public void onAboutObjectClick(String placeName) {
        Intent intent = new Intent(MainPage.this, SearchResultWebActivity.class);
        intent.putExtra("placeName", placeName);
        startActivity(intent);
    }

    // Method to handle "add to list" button click
    public void onAddToListClick(Place place) {
        savePlaceToFirestore(place);
    }

    // Method to fetch nearby places using api
    private void fetchNearbyPlacesFromApi(double latitude, double longitude) throws UnsupportedEncodingException {
        String overpassQuery = "node['tourism'](around:15000," + latitude + "," + longitude + ");" +
                "way['tourism'](around:15000," + latitude + "," + longitude + ");" +
                "relation['tourism'](around:15000," + latitude + "," + longitude;
        String encodedQuery = URLEncoder.encode(overpassQuery, "UTF-8");
        String apiUrl = "https://overpass-api.de/api/interpreter?data=[out:json];" + encodedQuery + ");out;";

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
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
                                    latitude = 0;
                                    longitude = 0;
                                }

                                Place place = new Place(latitude, longitude, placeName); // Declare place locally
                                if (!placeName.isEmpty()) {
                                    nearbyPlaces.add(place);
                                }

                            }

                            saveNearbyPlacesToFirestore(nearbyPlaces);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        queue.add(jsonObjectRequest);
    }

    // Method to save place info to database
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

    // Method to store nearby places to database
    private void saveNearbyPlacesToFirestore(List<Place> nearbyPlaces) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference userRef = db.collection("Users").document(userId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(userRef);

                    if (snapshot.contains("personalizedData")) {
                        transaction.update(userRef, "personalizedData", FieldValue.delete());
                    }

                    transaction.update(userRef, "personalizedData", nearbyPlaces);

                    return nearbyPlaces;
                })
                .addOnSuccessListener(result -> {
                })
                .addOnFailureListener(e -> {
                });

        displayNearbyPlaces();
    }

    // Method to display nearby places in the app
    private void displayNearbyPlaces() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            List<Place> personalizedData = user.getPersonalizedData();
                            if (personalizedData != null) {
                                adapter = new ListAdapter(this, personalizedData, false);
                                recyclerView.setAdapter(adapter);
                                adapter.setOnDirectionClickListener(this);
                                adapter.setOnAboutObjectClickListener(this);
                                adapter.setOnAddToListClickListener(this);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainPage.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
                });
    }

}
