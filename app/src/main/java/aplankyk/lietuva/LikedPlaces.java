package aplankyk.lietuva;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class LikedPlaces extends AppCompatActivity implements ListAdapter.OnDirectionClickListener,
        ListAdapter.OnAboutObjectClickListener {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ListAdapter adapter;
    private List<Place> likedPlacesList;
    private Button addToLikedPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_places);

        TextView pageName = findViewById(R.id.pagename);
        pageName.setText("Išsaugotos vietos");
        findViewById(R.id.noEntriesTextView).setVisibility(View.INVISIBLE);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        likedPlacesList = new ArrayList<>();
        adapter = new ListAdapter(this, likedPlacesList, true); // Pass context to the adapter
        recyclerView.setAdapter(adapter);

        adapter.setOnDirectionClickListener(this);
        adapter.setOnAboutObjectClickListener(this);

        // Retrieve liked places data for the current user from Firestore
        retrieveLikedPlacesData();

        ImageView homeButton = findViewById(R.id.homeButton);
        ImageView mapButton = findViewById(R.id.mapButton);
        ImageView photoButton = findViewById(R.id.photoButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);

        mapButton.setOnClickListener(v -> startActivity(new Intent(LikedPlaces.this, PlanTripMap.class)));
        homeButton.setOnClickListener(v -> startActivity(new Intent(LikedPlaces.this, MainPage.class)));
        photoButton.setOnClickListener(v -> startActivity(new Intent(LikedPlaces.this, PhotoAlbum.class)));
        settingsButton.setOnClickListener(v -> startActivity(new Intent(LikedPlaces.this, Settings.class)));

        ImageView mapPlannerButton = findViewById(R.id.map);
        ImageView searchButton = findViewById(R.id.search);
        ImageView cardsButton = findViewById(R.id.card);
        ImageView likedPlacesButton = findViewById(R.id.liked);
        likedPlacesButton.setBackgroundColor(Color.parseColor("#F6F6EB"));

        mapPlannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LikedPlaces.this, PlanTripMap.class);
                startActivity(intent);
            }
        });

        overridePendingTransition(0, 0);
        cardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LikedPlaces.this, PlanTripCards.class);
                startActivity(intent);
            }
        });

        overridePendingTransition(0, 0);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LikedPlaces.this, PlanTripSearch.class);
                startActivity(intent);
            }
        });

        addToLikedPlaces = findViewById(R.id.addToList);
        if (addToLikedPlaces != null) {
            addToLikedPlaces.setVisibility(View.VISIBLE); // Show the button
        } else {
            // Log an error or handle the situation where the button is not found
            Log.e("LikedPlaces", "Button not found in layout");
        }
    }

    private void retrieveLikedPlacesData() {
        // Get the current user's ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Query Firestore to retrieve the liked places data for the current user
            db.collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            // Get the liked places data from the document snapshot
                            // You may need to adjust this based on your database structure
                            List<Place> likedPlaces = documentSnapshot.toObject(User.class).getLikedPlaces();

                            // Update the UI with the retrieved liked places data
                            if (likedPlaces != null) {
                                likedPlacesList.clear();
                                likedPlacesList.addAll(likedPlaces);
                                adapter.notifyDataSetChanged();
                                if (likedPlacesList.isEmpty()) {
                                    findViewById(R.id.noEntriesTextView).setVisibility(View.VISIBLE);
                                } else {
                                    findViewById(R.id.noEntriesTextView).setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle any errors
                        }
                    });
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
                        LikedPlaces.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
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

}
