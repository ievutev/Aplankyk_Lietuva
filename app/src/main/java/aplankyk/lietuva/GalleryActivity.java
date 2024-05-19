package aplankyk.lietuva;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";

    private List<String> imageUrls = new ArrayList<>();

    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        String albumName = getIntent().getStringExtra("albumName");

        // Fetch images from Firebase Storage
        fetchImagesFromStorage(albumName);

        // Enable buttons and set click listeners
        enableButtons();
    }

    private void fetchImagesFromStorage(String albumName) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference albumRef = storage.getReference().child("users/" + userId + "/" + albumName);

            albumRef.listAll()
                    .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                        @Override
                        public void onSuccess(ListResult listResult) {
                            for (StorageReference item : listResult.getItems()) {
                                // Get download URL for each image and add it to the list
                                item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        imageUrls.add(uri.toString());
                                        Log.d(TAG, "Image URL: " + uri.toString());

                                        // Check if all images are loaded
                                        if (imageUrls.size() == listResult.getItems().size()) {
                                            // Enable buttons after images are loaded
                                            enableButtons();

                                            // Load first image only if the list is not empty
                                            if (!imageUrls.isEmpty()) {
                                                loadImageAtIndex(currentIndex);
                                            }
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Error getting download URL", e);
                                    }
                                });
                            }
                        }
                    })


                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Error fetching images", e);
                        }
                    });
        }
    }

    private void enableButtons() {
        ImageButton prevButton = findViewById(R.id.prevButton);
        ImageButton nextButton = findViewById(R.id.nextButton);

        prevButton.setEnabled(true);
        nextButton.setEnabled(true);

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPreviousImage();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNextImage();
            }
        });
    }

    private void loadPreviousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            loadImageAtIndex(currentIndex);
        }
    }

    private void loadNextImage() {
        if (currentIndex < imageUrls.size() - 1) {
            currentIndex++;
            loadImageAtIndex(currentIndex);
        }
    }

    private void loadImageAtIndex(int index) {
        String imageUrl = imageUrls.get(index);
        ImageView image = findViewById(R.id.photoImageView);
        Picasso.get().load(imageUrl).into(image);
    }

}
