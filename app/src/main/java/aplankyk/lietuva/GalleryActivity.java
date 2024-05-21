package aplankyk.lietuva;

import static java.security.AccessController.getContext;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

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

    private List<String> imageUrls = new ArrayList<>();
    private int currentIndex = 0;
    private Toast loadingToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        String albumName = getIntent().getStringExtra("albumName");
        fetchImagesFromStorage(albumName);
        enableButtons();
    }

    // Method to fetch images from the store by album name
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
                                item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        imageUrls.add(uri.toString());

                                        if (imageUrls.size() == listResult.getItems().size()) {
                                            enableButtons();

                                            if (!imageUrls.isEmpty()) {
                                                loadImageAtIndex(currentIndex);
                                            }
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        loadingToast = Toast.makeText(GalleryActivity.this, "Kažkas nutiko ne taip... Bandyk dar kartą.", Toast.LENGTH_SHORT);
                                        loadingToast.show();
                                    }
                                });
                            }
                        }
                    })


                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            loadingToast = Toast.makeText(GalleryActivity.this, "Kažkas nutiko ne taip... Bandyk dar kartą.", Toast.LENGTH_SHORT);
                            loadingToast.show();
                        }
                    });
        }
    }

    // Method to enable buttons to navigate the photos
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

    // Method to load previous image
    private void loadPreviousImage() {
        if (currentIndex > 0) {
            currentIndex--;
            loadImageAtIndex(currentIndex);
        }
    }

    // Method to load next image
    private void loadNextImage() {
        if (currentIndex < imageUrls.size() - 1) {
            currentIndex++;
            loadImageAtIndex(currentIndex);
        }
    }

    // Method to load image at specific index
    private void loadImageAtIndex(int index) {
        String imageUrl = imageUrls.get(index);
        ImageView image = findViewById(R.id.photoImageView);
        Picasso.get().load(imageUrl).into(image);
    }

}
