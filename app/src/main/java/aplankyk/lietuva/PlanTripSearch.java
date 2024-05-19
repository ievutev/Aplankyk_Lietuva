package aplankyk.lietuva;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PlanTripSearch extends AppCompatActivity implements ListAdapter.OnDirectionClickListener, ListAdapter.OnAboutObjectClickListener, ListAdapter.OnAddToListClickListener {

    private EditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private ListAdapter searchResultAdapter;
    private List<Place> searchResults;
    private ProgressBar loadingProgressBar;
    private TextView noEntriesText;
    Place place;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tripplannersearch);

        TextView pageName = findViewById(R.id.pagename);
        pageName.setText("Planuoti kelionę");
        ImageView searchButton = findViewById(R.id.search);
        searchButton.setBackgroundColor(Color.parseColor("#F6F6EB"));
        ImageView mapButton = findViewById(R.id.map);
        ImageView cardsButton = findViewById(R.id.card);
        Button searchButton2 = findViewById(R.id.search_button2);
        noEntriesText = findViewById(R.id.noEntries);

        searchEditText = findViewById(R.id.search_bar);

        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResults = new ArrayList<>();
        searchResultAdapter = new ListAdapter(this, searchResults, false);
        searchResultsRecyclerView.setAdapter(searchResultAdapter);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadingProgressBar = findViewById(R.id.loadingProgressBar2);
        searchButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                onSearchButtonClick(v);
            }
        });

        overridePendingTransition(0, 0);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripSearch.this, PlanTripMap.class);
                startActivity(intent);
            }
        });

        overridePendingTransition(0, 0);
        cardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripSearch.this, PlanTripCards.class);
                startActivity(intent);
            }
        });

        ImageView likedPlacesButton = findViewById(R.id.liked);

        overridePendingTransition(0, 0);
        likedPlacesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripSearch.this, LikedPlaces.class);
                startActivity(intent);
            }
        });

        ImageView homeButton = findViewById(R.id.homeButton);
        ImageView photoButton = findViewById(R.id.photoButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripSearch.this, MainPage.class);
                startActivity(intent);
            }
        });

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripSearch.this, PhotoAlbum.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripSearch.this, Settings.class);
                startActivity(intent);
            }
        });

        searchResultAdapter.setOnDirectionClickListener(this);
        searchResultAdapter.setOnAboutObjectClickListener(this);
        searchResultAdapter.setOnAddToListClickListener(this);
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
        Intent intent = new Intent(this, SearchResultWebActivity.class);
        intent.putExtra("placeName", placeName);
        startActivity(intent);
    }

    public void onAddToListClick(Place place) {
        // Update Firestore database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = FirebaseFirestore.getInstance().collection("Users").document(userId);
            userRef.update("likedPlaces", FieldValue.arrayUnion(place))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(PlanTripSearch.this, "Objektas pridėtas į Jūsų sąrašą", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(PlanTripSearch.this, "Nepavyko pridėti objekto į Jūsų sąrašą", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Kažkas nutiko ne taip...", e);
                        }
                    });
        }
    }

    public void onSearchButtonClick(View view) {
        String searchQuery = searchEditText.getText().toString().trim();
        // Perform search based on the search query
        performSearch(searchQuery);

    }

    private void performSearch(String query) {
        // Construct the Overpass API query
        double minLat = 53.7;
        double minLon = 20.9;
        double maxLat = 56.4;
        double maxLon = 26.8;

        // Combine multiple categories into a single query
        String overpassQuery = "[out:json];" +
                "(" +
                "node[\"name\"~\"" + query + "*\",i][tourism](" + minLat + "," + minLon + "," + maxLat + "," + maxLon + ");" +
                "way[\"name\"~\"" + query + "*\",i][tourism](" + minLat + "," + minLon + "," + maxLat + "," + maxLon + ");" +
                "relation[\"name\"~\"" + query + "*\",i][tourism](" + minLat + "," + minLon + "," + maxLat + "," + maxLon + ");" +
                ");" +
                "out;";

        try {
            // Encode the query string
            String encodedQuery = URLEncoder.encode(overpassQuery, "UTF-8");
            String apiUrl = "https://overpass-api.de/api/interpreter?data=" + encodedQuery;
            // Execute the AsyncTask to perform the HTTP request
            new SearchTask().execute(apiUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class SearchTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            try {
                // Create URL object from the API URL string
                URL url = new URL(urls[0]);

                // Create HttpURLConnection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set request method
                connection.setRequestMethod("GET");

                // Connect to the API
                connection.connect();

                // Read input stream into a String
                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                response = stringBuilder.toString();

                // Close resources
                inputStream.close();
                bufferedReader.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // Process the search results
            if (result != null) {
                processSearchResults(result);
            } else {
                // Handle error case
                handleSearchError();
            }
        }
    }
    private void processSearchResults(String json) {
        try {
            // Parse the JSON response
            JSONObject response = new JSONObject(json);
            JSONArray elements = response.getJSONArray("elements");

            // Clear existing search results
            searchResults.clear();

            // Process each search result
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                JSONObject tags = element.getJSONObject("tags");
                String name = tags.optString("name", "");
                double latitude;
                double longitude;
                if (element.has("lat") && element.has("lon")) {
                    latitude = element.getDouble("lat");
                    longitude = element.getDouble("lon");
                } else {
                    latitude = 0; // Default latitude
                    longitude = 0; // Default longitude
                }
                place = new Place(latitude, longitude, name);
                // Add the name to the search results list
                searchResults.add(place);
            }

            // Notify the adapter that the data set has changed
            searchResultAdapter.notifyDataSetChanged();
            loadingProgressBar.setVisibility(View.INVISIBLE);
            noEntriesText.setText("");
            // Update UI if no search results found
            if (searchResults.isEmpty()) {
                noEntriesText.setText("Įrašų nėra");
                loadingProgressBar.setVisibility(View.INVISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // Handle JSON parsing error
            handleSearchError();
        }
    }


    private void handleSearchError() {
        // Hide progress bar
        loadingProgressBar.setVisibility(View.INVISIBLE);
        // Show appropriate error message
        noEntriesText.setText("Error occurred while fetching data");
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
                        PlanTripSearch.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
    }
}