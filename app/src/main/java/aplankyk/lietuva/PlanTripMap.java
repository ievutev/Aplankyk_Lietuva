package aplankyk.lietuva;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PlanTripMap extends AppCompatActivity implements OnMapReadyCallback, ClusterManager.OnClusterItemClickListener<Place>, ClusterManager.OnClusterClickListener<Place> {

    private GoogleMap googleMap;
    private MapView mapView;
    private ClusterManager<Place> clusterManager;
    private Toast loadingToast;
    private CardView cardView;
    private TextView placeNameTextView;
    private Button directionsButton;
    private Button aboutObjectButton;
    private Button addToListButton;
    private LatLng coordinates;
    private Place place;
    View overlay;

    private FetchPlacesTask fetchPlacesTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tripplannermap);
        ImageView mapButton = findViewById(R.id.map);
        mapButton.setBackgroundColor(Color.parseColor("#F6F6EB"));

        // Check network connectivity
        if (!isNetworkConnected()) {
            Toast.makeText(this, "Nėra interneto ryšio", Toast.LENGTH_SHORT).show();
        }
        else {
            mapView = findViewById(R.id.mapView);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);

            initializeViews();
            setupListeners();
        }
    }

    // Method for initializing views
    private void initializeViews() {
        TextView pageName = findViewById(R.id.pagename);
        pageName.setText("Planuoti kelionę");

        overlay = findViewById(R.id.overlay);
        overlay.setVisibility(View.INVISIBLE);
        cardView = findViewById(R.id.card_view);
        placeNameTextView = findViewById(R.id.placeName);
        addToListButton = findViewById(R.id.addToList);
        directionsButton = findViewById(R.id.directions);
        aboutObjectButton = findViewById(R.id.aboutObject);
        mapView = findViewById(R.id.mapView);
    }

    // Method for setting up the listeners for buttons
    private void setupListeners() {
        ImageView searchButton = findViewById(R.id.search);
        ImageView cardsButton = findViewById(R.id.card);
        ImageView likedPlacesButton = findViewById(R.id.liked);
        ImageView homeButton = findViewById(R.id.homeButton);
        ImageView photoButton = findViewById(R.id.photoButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);

        searchButton.setOnClickListener(v -> startActivity(new Intent(PlanTripMap.this, PlanTripSearch.class)));
        cardsButton.setOnClickListener(v -> startActivity(new Intent(PlanTripMap.this, PlanTripCards.class)));
        likedPlacesButton.setOnClickListener(v -> startActivity(new Intent(PlanTripMap.this, LikedPlaces.class)));
        homeButton.setOnClickListener(v -> startActivity(new Intent(PlanTripMap.this, MainPage.class)));
        photoButton.setOnClickListener(v -> startActivity(new Intent(PlanTripMap.this, PhotoAlbum.class)));
        settingsButton.setOnClickListener(v -> startActivity(new Intent(PlanTripMap.this, Settings.class)));

        directionsButton.setOnClickListener(v -> {
            String locationName = placeNameTextView.getText().toString();
            String uri = "https://www.google.com/maps/dir/?api=1&destination=" + locationName;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        });

        aboutObjectButton.setOnClickListener(v -> {
            String locationName = placeNameTextView.getText().toString();
            Intent intent = new Intent(PlanTripMap.this, SearchResultWebActivity.class);
            intent.putExtra("placeName", locationName);
            startActivity(intent);
        });

        addToListButton.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                DocumentReference userRef = FirebaseFirestore.getInstance().collection("Users").document(userId);
                userRef.update("likedPlaces", FieldValue.arrayUnion(place))
                        .addOnSuccessListener(aVoid -> Toast.makeText(PlanTripMap.this, "Objektas pridėtas į Jūsų sąrašą", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(PlanTripMap.this, "Nepavyko pridėti objekto į Jūsų sąrašą", Toast.LENGTH_SHORT).show());
            }
        });

        overlay.setOnClickListener(v -> {
            cardView.setVisibility(View.INVISIBLE);
            overlay.setVisibility(View.INVISIBLE);
            TextView placeNameTextView = findViewById(R.id.placeName);
            placeNameTextView.setText("");
        });
    }

    // Method with actions to do, when map is ready
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        clusterManager = new ClusterManager<>(this, googleMap);
        clusterManager.setOnClusterItemClickListener(this);
        clusterManager.setOnClusterClickListener(this);
        googleMap.setOnCameraIdleListener(clusterManager);

        LatLng location = new LatLng(55.1694, 23.8813);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 7));

        Toast.makeText(PlanTripMap.this, "Žemėlapio duomenys kraunami. Tai gali užtrukti.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            fetchPlacesTask = new FetchPlacesTask(this);
            fetchPlacesTask.execute();
        }, 10);
    }

    private static class FetchPlacesTask extends AsyncTask<Void, Void, String> {
        private WeakReference<PlanTripMap> activityReference;

        FetchPlacesTask(PlanTripMap context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... voids) {
            PlanTripMap activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            if (isCancelled()) return null;  // Check if the task is cancelled

            return activity.fetchDataFromOpenStreetMapAPI();
        }

        @Override
        protected void onPostExecute(String placesJson) {
            PlanTripMap activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            if (!isCancelled() && placesJson != null) {  // Check if the task is cancelled
                activity.processPlacesJson(placesJson);
            } else {
                Toast.makeText(activity, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            PlanTripMap activity = activityReference.get();
            if (activity != null && !activity.isFinishing()) {
            }
        }
    }

    // Method for getting data from OpenStreetMap API
    private String fetchDataFromOpenStreetMapAPI() {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String placesJson = null;

        try {
            String overpassQuery = "[out:json];(node['tourism'](53.7,20.9,56.4,26.8););out;";

            URL url = new URL("https://overpass-api.de/api/interpreter?data=" + overpassQuery);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }
                if (buffer.length() == 0) {
                    return null;
                }
                placesJson = buffer.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return placesJson;
    }

    // Method to save get places info from JSON
    private void processPlacesJson(String placesJson) {
        if (placesJson == null || placesJson.isEmpty()) {
            return;
        }

        try {
            JSONObject response = new JSONObject(placesJson);
            JSONArray placesArray = response.getJSONArray("elements");
            List<Place> places = new ArrayList<>();
            for (int i = 0; i < placesArray.length(); i++) {
                JSONObject placeObject = placesArray.getJSONObject(i);
                if (placeObject.has("lat") && placeObject.has("lon")) {
                    if (isPlaceInLithuania(placeObject.getDouble("lon"), placeObject.getDouble("lat"))) {
                        Place place = createPlaceFromJson(placeObject);
                        if (place != null) {
                            places.add(place);
                        }
                    }
                }
            }
            addPlacesToMap(places);
            Toast.makeText(PlanTripMap.this, "Žemėlapio duomenys užkrauti", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(PlanTripMap.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to create place object from JSON
    private Place createPlaceFromJson(JSONObject placeObject) {
        JSONObject tagsObject = placeObject.optJSONObject("tags");
        String placeName = tagsObject.optString("name");
        double latitude = placeObject.optDouble("lat", 0);
        double longitude = placeObject.optDouble("lon", 0);

        if (!placeName.isEmpty() && latitude != 0 && longitude != 0) {
            return new Place(latitude, longitude, placeName);
        }
        return null;
    }

    // Method to add the place to the map
    private void addPlacesToMap(List<Place> places) {
        for (Place place : places) {
            clusterManager.addItem(place);
        }
        clusterManager.cluster();
    }

    // Method to check if the place is within Lithuania
    static boolean isPlaceInLithuania(double longitude, double latitude) {
        return ((longitude > 21.116236 && longitude < 22.754387 && latitude > 55.181514 && latitude < 56.325199) ||
                (longitude > 22.621825 && longitude < 26.293709 && latitude > 55.131299 && latitude < 56.037815) ||
                (longitude > 22.896388 && longitude < 25.534867 && latitude > 54.370459 && latitude < 55.150137) ||
                (longitude > 22.851318 && longitude < 24.889282 && latitude > 55.880492 && latitude < 55.960617) ||
                (longitude > 23.512033 && longitude < 24.842267 && latitude > 53.965276 && latitude < 54.752666));
    }

    // Method to handle the click on the marker
    @Override
    public boolean onClusterItemClick(@NonNull Place clickedPlace) {
        String placeName = clickedPlace.getTitle();
        placeNameTextView.setText(placeName);
        coordinates = clickedPlace.getPosition();
        place = clickedPlace;
        cardView.setVisibility(View.VISIBLE);
        View overlay = findViewById(R.id.overlay);
        overlay.setVisibility(View.VISIBLE);
        return false;
    }

    @Override
    public boolean onClusterClick(@NonNull Cluster<Place> cluster) {
        return false;
    }

    // Method for system "back" button press
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Ar tikrai norite uždaryti programėlę?")
                .setPositiveButton("Ne", (dialogInterface, i) -> dialogInterface.dismiss())
                .setNegativeButton("Taip", (dialogInterface, i) -> {
                    PlanTripMap.this.finishAffinity();
                    System.exit(0);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (fetchPlacesTask != null) {
            fetchPlacesTask.cancel(true);  // Cancel the AsyncTask
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Checking if network is connected
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
