package aplankyk.lietuva;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogIn extends AppCompatActivity {

    private static final String TAG = "LogInActivity";
    private GoogleSignInClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Button loginButton = findViewById(R.id.loginButton);

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        client = GoogleSignIn.getClient(this, options);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkConnected()) {
                    GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(LogIn.this, GoogleSignInOptions.DEFAULT_SIGN_IN);
                    googleSignInClient.signOut();
                    Intent signInIntent = client.getSignInIntent();
                    startActivityForResult(signInIntent, 1234);
                } else {
                    Toast.makeText(LogIn.this, "Nėra interneto ryšio", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Check network connectivity on activity creation
        if (!isNetworkConnected()) {
            Toast.makeText(this, "Nėra interneto ryšio", Toast.LENGTH_SHORT).show();
            loginButton.setEnabled(false);
        } else {
            loginButton.setEnabled(true);
            checkUserLoginStatus();
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
                        LogIn.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (requestCode == 1234) {
            if (resultCode == RESULT_OK) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null) {
                                    String userID = user.getUid();
                                    db.collection("Users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (!document.exists()) {
                                                    showUserNotFoundDialog(userID, user);
                                                } else {
                                                    startActivity(new Intent(LogIn.this, MainPage.class));
                                                    finish();
                                                }
                                            } else {
                                                Toast.makeText(LogIn.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(LogIn.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (ApiException e) {
                    Toast.makeText(LogIn.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void checkUserLoginStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    startActivity(new Intent(this, MainPage.class));
                    finish();
                } else {
                    FirebaseAuth.getInstance().signOut();
                }
            });
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void showUserNotFoundDialog(String userID, FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(LogIn.this);
        builder.setMessage("Vartotojas nerastas. Ar norite sukurti paskyrą?");
        builder.setPositiveButton("Taip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                User newUser = new User(userID);
                db.collection("Users").document(userID)
                        .set(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(LogIn.this, "Naudotojas sėkmingai sukurtas", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(LogIn.this, "Kažkas nutiko ne taip. Bandyk dar kartą", Toast.LENGTH_SHORT).show();
                        });
                startActivity(new Intent(LogIn.this, MainPage.class));
            }
        });
        builder.setNegativeButton("Ne", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth.getInstance().signOut();
                if (user != null) {
                    user.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(LogIn.this, LogIn.class);
                            startActivity(intent);
                        }
                    });
                }
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
