package aplankyk.lietuva;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

public class PlanTripCards extends AppCompatActivity {

    Button directionsButton;
    Button aboutObjectButton;
    Button addToListButton;
    Place place;
    CardView place_info;
    View overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tripplannercards);

        TextView pageName = findViewById(R.id.pagename);
        pageName.setText("Planuoti kelionę");

        ImageView mapButton = findViewById(R.id.map);
        ImageView searchButton = findViewById(R.id.search);
        ImageView cardsButton = findViewById(R.id.card);
        cardsButton.setBackgroundColor(Color.parseColor("#F6F6EB"));
        ImageView likedPlacesButton = findViewById(R.id.liked);

        overridePendingTransition(0, 0);
        likedPlacesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripCards.this, LikedPlaces.class);
                startActivity(intent);
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripCards.this, PlanTripSearch.class);
                startActivity(intent);
            }
        });

        overridePendingTransition(0, 0);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripCards.this, PlanTripMap.class);
                startActivity(intent);
            }
        });

        ImageView homeButton = findViewById(R.id.homeButton);
        ImageView photoButton = findViewById(R.id.photoButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripCards.this, MainPage.class);
                startActivity(intent);
            }
        });

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripCards.this, PhotoAlbum.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlanTripCards.this, Settings.class);
                startActivity(intent);
            }
        });

        CardView card1 = findViewById(R.id.card1);
        CardView card2 = findViewById(R.id.card2);
        CardView card3 = findViewById(R.id.card3);
        place_info = findViewById(R.id.card_view);
        overlay = findViewById(R.id.overlay);
        overlay.setVisibility(View.INVISIBLE);

        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRandomLocationName();
                TransitionManager.beginDelayedTransition(place_info); // parentLayout is the layout containing the card
                place_info.setVisibility(View.VISIBLE);
                overlay.setVisibility(View.VISIBLE);
            }
        });

        card2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRandomLocationName();
                TransitionManager.beginDelayedTransition(place_info); // parentLayout is the layout containing the card
                place_info.setVisibility(View.VISIBLE);
                overlay.setVisibility(View.VISIBLE);
            }
        });

        card3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRandomLocationName();
                TransitionManager.beginDelayedTransition(place_info); // parentLayout is the layout containing the card
                place_info.setVisibility(View.VISIBLE);
                overlay.setVisibility(View.VISIBLE);
            }
        });

        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                place_info.setVisibility(View.INVISIBLE);
                overlay.setVisibility(View.INVISIBLE);
                TextView placeNameTextView = findViewById(R.id.placeName);
                placeNameTextView.setText("");
            }
        });

        directionsButton = findViewById(R.id.directions);
        directionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected location name from the TextView
                TextView placeNameTextView = findViewById(R.id.placeName);
                String locationName = placeNameTextView.getText().toString();
                String uri = "https://www.google.com/maps/dir/?api=1&destination=" + locationName;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps"); // Specify package to ensure Google Maps is used
                startActivity(intent);
            }
        });

        aboutObjectButton = findViewById(R.id.aboutObject);
        aboutObjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView placeNameTextView = findViewById(R.id.placeName);
                String locationName = placeNameTextView.getText().toString();
                Intent intent = new Intent(PlanTripCards.this, SearchResultWebActivity.class);
                intent.putExtra("placeName", locationName);
                startActivity(intent);
            }
        });

        addToListButton = findViewById(R.id.addToList);
        addToListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    DocumentReference userRef = FirebaseFirestore.getInstance().collection("Users").document(userId);
                    userRef.update("likedPlaces", FieldValue.arrayUnion(place))
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(PlanTripCards.this, "Objektas pridėtas į Jūsų sąrašą", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(PlanTripCards.this, "Nepavyko pridėti objekto į Jūsų sąrašą", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Kažkas nutiko ne taip...", e);
                                }
                            });
                }
            }
        });

    }

    private void getRandomLocationName() {
        // Construct the Overpass API query to get all nodes with the tourism tag
        String overpassQuery = "[out:json];\n" +
                "(node[tourism](around: 90000, 55.1694,23.8813););\n" +
                "out;";

        ProgressBar loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingProgressBar.setVisibility(View.VISIBLE);

        try {
            // Encode the query string
            String encodedQuery = URLEncoder.encode(overpassQuery, "UTF-8");
            String apiUrl = "https://overpass-api.de/api/interpreter?data=" + encodedQuery;

            // Execute the HTTP request to fetch all nodes meeting the criteria
            new HttpRequestTask().execute(apiUrl);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    private class HttpRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            HttpURLConnection connection = null;
            try {
                // Create URL object from the API URL string
                URL url = new URL(urls[0]);
                // Open connection
                connection = (HttpURLConnection) url.openConnection();
                // Set request method
                connection.setRequestMethod("GET");
                // Connect to the API
                connection.connect();
                // Read input stream into a String
                // Handle response...
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
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    // Disconnect the connection
                    connection.disconnect();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // Parse the JSON response and extract a random location name

            String locationName = parseRandomLocationNameFromJson(result);

            // Populate the UI with the random location name
            if (locationName != "") {
                TextView placeNameTextView = findViewById(R.id.placeName);
                placeNameTextView.setText(locationName);
            }

            else {
                place_info.setVisibility(View.INVISIBLE);
                overlay.setVisibility(View.INVISIBLE);
                Toast.makeText(PlanTripCards.this, "Nepavyko gauti duomenų", Toast.LENGTH_SHORT).show();
            }

            ProgressBar loadingProgressBar = findViewById(R.id.loadingProgressBar);
            loadingProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private String parseRandomLocationNameFromJson(String json) {
        try {
            // Parse the JSON response
            JSONObject response = new JSONObject(json);
            JSONArray elements = response.getJSONArray("elements");

            // Check if elements array is not empty
            if (elements.length() > 0) {
                Random random = new Random();
                int attempt = 0;
                double latitude = 0;
                double longitude = 0;
                String name = "";
                while (attempt < elements.length()) {
                    // Get a random element from the array
                    int randomIndex = random.nextInt(elements.length());
                    JSONObject element = elements.getJSONObject(randomIndex);
                    JSONObject tags = element.getJSONObject("tags");

                    // Check if the element has name, latitude, and longitude tags
                    if (tags.has("name")) {
                        if (tags.has("lat") && tags.has("lon")) {
                            latitude = element.getDouble("lat");
                            longitude = element.getDouble("lon");
                            name = tags.getString("name");

                            // Check if the coordinates are within Lithuania's bounds
                            if (isWithinLithuania(latitude, longitude)) {
                                // Assign the 'place' variable here
                                place = new Place(latitude, longitude, name);
                                return name;
                            }
                        } else {
                            name = tags.getString("name");
                            // Assign the 'place' variable here
                            place = new Place(latitude, longitude, name);
                            return name;
                        }
                    }

                    attempt++;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Return default message if no valid location name is found
        return "";
    }


    // Check if the coordinates are within Lithuania's bounds
    private boolean isWithinLithuania(double latitude, double longitude) {
        boolean result = false;

        // Define the bounding boxes around Lithuania's borders
        if ((longitude > 21.116236 && longitude < 22.754387 && latitude > 55.181514 && latitude < 56.325199) ||
                (longitude > 22.621825 && longitude < 26.293709 && latitude > 55.131299 && latitude < 56.037815) ||
                (longitude > 22.896388 && longitude < 25.534867 && latitude > 54.370459 && latitude < 55.150137) ||
                (longitude > 23.512033 && longitude < 24.842267 && latitude > 53.965276 && latitude < 54.752666)) {
            result = true;
        }

        return result;
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
                        PlanTripCards.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
    }
}
