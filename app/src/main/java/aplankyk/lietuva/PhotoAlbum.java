package aplankyk.lietuva;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PhotoAlbum extends AppCompatActivity implements AddAlbumDialog.AddAlbumDialogListener, AddAlbumDialog.AlbumAddedListener {

    private RecyclerView recyclerView;
    private AlbumAdapter adapter;
    private List<String> albumList;
    private  AddAlbumDialog addAlbumDialog;
    public static final int REQUEST_CODE_PICK_IMAGE = 102;
    private boolean albumsFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoalbum);

        TextView pageName = findViewById(R.id.pagename);
        pageName.setText("Nuotraukų albumas");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumList = new ArrayList<>();
        adapter = new AlbumAdapter(albumList);
        recyclerView.setAdapter(adapter);

        ImageButton addAlbumButton = findViewById(R.id.addAlbumButton);
        addAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create and show the dialog using the context of the current activity
                addAlbumDialog = new AddAlbumDialog(PhotoAlbum.this, 900, 900);
                addAlbumDialog.setAlbumAddedListener(PhotoAlbum.this); // Set the listener
                addAlbumDialog.show();
            }
        });

        fetchAlbumsFromStorage();

        ImageView homeButton = findViewById(R.id.homeButton);
        ImageView mapButton = findViewById(R.id.mapButton);
        ImageView settingsButton = findViewById(R.id.settingsButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoAlbum.this, MainPage.class);
                startActivity(intent);
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoAlbum.this, PlanTripMap.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoAlbum.this, Settings.class);
                startActivity(intent);
            }
        });

        adapter.setOnItemClickListener(new AlbumAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String selectedAlbum = albumList.get(position);
                openGallery(selectedAlbum);
            }
        });
    }

    // Method to open the gallery
    private void openGallery(String selectedAlbum) {
        Intent intent = new Intent(PhotoAlbum.this, GalleryActivity.class);
        intent.putExtra("albumName", selectedAlbum);
        startActivity(intent);
    }

    // Method to fetch albums from storage
    private void fetchAlbumsFromStorage() {
        if (!albumsFetched) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference listRef = storage.getReference().child("users/" + userId);
                fetchItems(listRef);
                albumsFetched = true;
            }
        }
    }

    // Method to fetch photos
    private void fetchItems(final StorageReference reference) {
        reference.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        albumList.clear();
                        for (StorageReference prefix : listResult.getPrefixes()) {
                            String folderName = prefix.getName();
                            albumList.add(folderName);
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(PhotoAlbum.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to count the selected images
    @Override
    public void onPhotosSelected(List<Uri> selectedImageUris) {
        if (addAlbumDialog != null) {
            addAlbumDialog.setSelectedImageUris(selectedImageUris);
        }
    }

    // Method to handle systems "back" button press
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
                        PhotoAlbum.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            if (addAlbumDialog != null) {
                addAlbumDialog.handleImageSelectionResult(data);
            }
        }
    }

    // Method to refresh the activity when album was added
    @Override
    public void onAlbumAdded() {
        fetchAlbumsFromStorage();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                recreate();
            }
        }, 4000);
    }
}
