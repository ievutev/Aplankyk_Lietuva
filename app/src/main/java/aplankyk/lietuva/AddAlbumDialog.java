package aplankyk.lietuva;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class AddAlbumDialog extends Dialog {

    // Define variables
    private static final int REQUEST_CODE_PICK_IMAGE = 102; // Define your request code here

    private EditText albumTitleEditText;
    private Button addPhotoButton;
    private TextView photoSelectedTextView;
    private Button cancelButton;
    private Button saveButton;
    private int dialogWidth;
    private int dialogHeight;
    private AddAlbumDialogListener listener;
    private Activity activity;
    private List<Uri> selectedImageUris;
    private int requestCode;
    private AlbumAddedListener albumAddedListener;
    private Toast loadingToast;

    public AddAlbumDialog(@NonNull Activity activity, int width, int height) {
        super(activity);
        this.activity = activity; // Store the activity reference
        this.dialogWidth = width;
        this.dialogHeight = height;
        this.listener = (AddAlbumDialogListener) activity; // Initialize the listener
    }

    // Define the interface for communication
    public interface AddAlbumDialogListener {
        void onPhotosSelected(List<Uri> selectedImageUris);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        setContentView(R.layout.add_album_dialog);
        getWindow().setLayout(dialogWidth, dialogHeight);

        // Initialize views
        albumTitleEditText = findViewById(R.id.albumTitleEditText);
        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveButton);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        photoSelectedTextView = findViewById(R.id.photoSelectedTextView);

        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                activity.startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
            }
        });

        // Set click listeners
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // Close the dialog when cancel button is clicked
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the album title from the EditText
                String albumTitle = albumTitleEditText.getText().toString();

                // Check if album title is empty
                if (albumTitle.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter album title", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if photos are selected
                if (selectedImageUris == null || selectedImageUris.isEmpty()) {
                    Toast.makeText(getContext(), "Please select photos", Toast.LENGTH_SHORT).show();
                    return;
                }


                // Upload photos to Firebase Storage (implement this part in your activity)
                uploadPhotosToStorage(albumTitle);

                loadingToast = Toast.makeText(getContext(), "Albumas sukurtas, sąrašas atsinaujins netrukus", Toast.LENGTH_SHORT);
                loadingToast.show();

                dismiss(); // Close the dialog

                if (albumAddedListener != null) {
                    albumAddedListener.onAlbumAdded();
                }

            }
        });


    }

    // Method to set selected image URIs
    public void setSelectedImageUris(List<Uri> selectedImageUris) {
        this.selectedImageUris = selectedImageUris;
        if (selectedImageUris != null && !selectedImageUris.isEmpty()) {
            photoSelectedTextView.setText("Pasirinktos " + selectedImageUris.size() + " nuotraukos");
        }
    }

    // Method to upload photos to Firebase Storage
    private void uploadPhotosToStorage(String albumTitle) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference userAlbumRef = FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child(albumTitle);
        for (Uri selectedImageUri : selectedImageUris) {
            String imageName = selectedImageUri.getLastPathSegment();
            StorageReference imageRef = userAlbumRef.child(imageName);
            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Photo uploaded successfully
                            if (albumAddedListener != null) {
                                albumAddedListener.onAlbumAdded(); // Notify the listener
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle upload failure
                        }
                    });
        }
    }

    // Method to handle the result of image selection
    public void handleImageSelectionResult(Intent data) {
        if (data != null) {
            List<Uri> selectedImageUris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    selectedImageUris.add(uri);
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }
            setSelectedImageUris(selectedImageUris);
        }
    }

    public interface AlbumAddedListener {
        void onAlbumAdded();
    }



    public void setAlbumAddedListener(AlbumAddedListener listener) {
        this.albumAddedListener = listener;
    }

}

