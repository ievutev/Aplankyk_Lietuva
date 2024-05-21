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

    private static final int REQUEST_CODE_PICK_IMAGE = 102;
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
    private AlbumAddedListener albumAddedListener;
    private Toast loadingToast;

    public AddAlbumDialog(@NonNull Activity activity, int width, int height) {
        super(activity);
        this.activity = activity;
        this.dialogWidth = width;
        this.dialogHeight = height;
        this.listener = (AddAlbumDialogListener) activity;
    }

    public interface AddAlbumDialogListener {

        // Method to count the selected images
        void onPhotosSelected(List<Uri> selectedImageUris);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        setContentView(R.layout.add_album_dialog);
        getWindow().setLayout(dialogWidth, dialogHeight);

        // Initializing views
        albumTitleEditText = findViewById(R.id.albumTitleEditText);
        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveButton);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        photoSelectedTextView = findViewById(R.id.photoSelectedTextView);

        // Click listeners
        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                activity.startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String albumTitle = albumTitleEditText.getText().toString();

                if (albumTitle.isEmpty()) {
                    Toast.makeText(getContext(), "Įveskite albumo pavadinimą", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedImageUris == null || selectedImageUris.isEmpty()) {
                    Toast.makeText(getContext(), "Pasirinkite bent vieną nuotrauką", Toast.LENGTH_SHORT).show();
                    return;
                }

                uploadPhotosToStorage(albumTitle);

                loadingToast = Toast.makeText(getContext(), "Albumas sukurtas, sąrašas atsinaujins netrukus", Toast.LENGTH_SHORT);
                loadingToast.show();

                dismiss();

                if (albumAddedListener != null) {
                    albumAddedListener.onAlbumAdded();
                }

            }
        });


    }

    // Method to inform user about selected photos
    public void setSelectedImageUris(List<Uri> selectedImageUris) {
        this.selectedImageUris = selectedImageUris;
        if (selectedImageUris != null && !selectedImageUris.isEmpty()) {
            photoSelectedTextView.setText("Pasirinktos " + selectedImageUris.size() + " nuotraukos");
        }
    }

    // Method to upload selected photos to storage
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
                                albumAddedListener.onAlbumAdded();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            loadingToast = Toast.makeText(getContext(), "Kažkas nutiko ne taip... Bandyk dar kartą.", Toast.LENGTH_SHORT);
                            loadingToast.show();
                        }
                    });
        }
    }

    // Method to count the selected images
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

