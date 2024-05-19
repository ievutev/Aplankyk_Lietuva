package aplankyk.lietuva;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

public class Settings extends AppCompatActivity {

    private Button logOutButton;
    private Button deleteButton;
    private Button aboutApp;
    private Button howToUse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        TextView pageName = findViewById(R.id.pagename);
        pageName.setText("Nustatymai");

        aboutApp = findViewById(R.id.aboutApp);
        aboutApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });

        howToUse = findViewById(R.id.howToUse);
        howToUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHowToUseDialog();
            }
        });

        logOutButton = findViewById(R.id.logOut);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setMessage("Ar tikrai norite atsijungti?");
                builder.setPositiveButton("Atsijungti", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(Settings.this, GoogleSignInOptions.DEFAULT_SIGN_IN);
                        Intent i = new Intent(Settings.this, LogIn.class);
                        startActivity(i);
                        googleSignInClient.signOut();
                    }
                });
                builder.setNegativeButton("Atšaukti", null);
                builder.show();

            }
        });

        deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                builder.setMessage("Ar tikrai norite šalinti paskyrą? Visi jūsų duomenys bus ištrinti.");
                builder.setPositiveButton("Ištrinti", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Proceed with account deletion
                        deleteUserAccount();
                        deleteUserDataFromFirestore();
                    }
                });
                builder.setNegativeButton("Atšaukti", null);
                builder.show();
            }
        });

        ImageView homeButton = findViewById(R.id.homeButton);
        ImageView mapButton = findViewById(R.id.mapButton);
        ImageView photoButton = findViewById(R.id.photoButton);

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, MainPage.class);
                startActivity(intent);
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, PlanTripMap.class);
                startActivity(intent);
            }
        });

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, PhotoAlbum.class);
                startActivity(intent);
            }
        });
    }

    private void showAboutDialog() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the title and message
        builder.setTitle("Apie programėlę ir kūrėją");
        builder.setMessage("Mobilioji programėlė „Aplankyk Lietuvą“ yra skirta visiems, kurie nori keliauti po Lietuvą ir aplankyti kuo daugiau lankytinų vietų Lietuvoje. Naudotojai, naudodamiesi šia mobiliąja programėle gali ieškoti lankytinų vietų paieškoje ar žemėlapyje. Taip pat, yra galimybė pasirinkti vieną iš trijų atsitiktinių kortelių, kuriose yra užrašytos skirtingos ir atsitiktinės lankytinos vietos Lietuvoje. Naudotojas gali sukurti skaitmeninį nuotraukų albumą, kuriame yra galimybė saugoti kelionių metu įamžintas akimirkas.");

        // Set a button to close the dialog
        builder.setPositiveButton("Uždaryti", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Dismiss the dialog
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showHowToUseDialog() {
        // Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the title and message
        builder.setTitle("Kaip naudotis?");
        builder.setMessage("Prisijungimo/paskyros kūrimo lange pateikiamas mobiliosios programėlės logotipas, mygtukas skirtas prisijungimui/paskyros kūrimui ir pateikiami kūrimo metai.  Naudotojui paspaudus mygtuką [Prisijunkite naudodami „Google“]  išvedamas modalinis langas su „Google“ paskyros pasirinkimu. Kai naudotojas pasirenka paskyrą, su kuria nori prisijungti, sistema patikrina, ar yra registruotas vartotojas su pasirinktos paskyros duomenimis, jei nėra – išvedamas informacinis pranešimas „Vartotojas nerastas. Ar norite sukurti paskyrą?“. \n" +
                "Jei vartotojas randamas – iškart nukreipiamas į pagrindinį mobiliosios programėlės langą.\n" +
                "Naudotojui pasirinkus meniu punktą „Planuoti kelionę“ išvedamas žemėlapio langas. Kol žemėlapio duomenys kraunami – išvedamas laikinasis informacinis pranešimas apie kraunamus žemėlapio duomenis. Kai duomenys užkraunami – naudotojas apie tai informuojamas. \n" +
                "Lange naudotojas gali keisti žemėlapio mastelį ir pasirinkti norimą žymeklį žemėlapyje, paspaudus ant jo – išvedamas lankytinos vietos kortelės peržiūros modalinis langas. Kortelės peržiūros lange naudotojas gali paspausti mygtuką [Daugiau informacijos], tuomet naudotojui atveriamas naršyklės langas su pasirinkto objekto paieškos rezultatais „Google“ paieškos variklyje, o „Android“ sistemoje paspaudus mygtuką [Atgal] – grįžtama į pasirinkto objekto kortelės peržiūros modalinį langą.\n" +
                "Naudotojui paspaudus mygtuką [Nuorodos į objektą], atidaroma „Google Maps“ mobilioji programėlė su nuorodomis nuo naudotojo esamos vietos iki pasirinkto objekto, o „Android“ sistemoje paspaudus mygtuką [Atgal] – grįžtama į pasirinkto objekto kortelės peržiūros modalinį langą. Naudotojui paspaudus mygtuką [Pridėti objektą į norimų aplankyti vietų sąrašą]  - pasirinktas objektas pridedamas į naudotojo norimų aplankyti vietų sąrašą ir naudotojas apie tai informuojamas laikinuoju informaciniu pranešimu.\n" +
                "Naudotojas iš žemėlapio lango gali pereiti į paieškos langą spausdamas ant skirtuko [Paieška]. Kol duomenys kraunami, naudotojui rodoma besisukanti apskritimo animacija, kuri indikuoja apie kraunamus duomenis. Naudotojui įvedus norimą paieškos frazę ir paspaudus [Ieškoti], sąrašas užpildomas atitinkamais rezultatais pagal įvestą paieškos žodį.\n" +
                "Naudotojui perėjus į kortelės traukimo langą, t.y. paspaudus ant [Atsitiktinės kortelės pasirinkimas] skirtuko, matomas trijų kortelių vaizdas. Paspaudus ant bet kurios iš trijų kortelių – atveriamas lankytinos vietos kortelės peržiūros modalinis langas.\n" +
                "Naudotojui paspaudus ant [Norimų aplankyti vietų sąrašas] skirtuko – atveriamas išsaugotų vietų sąrašas.\n" +
                "Naudotojui paspaudus ant meniu punkto [Nuotraukų albumai] – atveriamas nuotraukų albumų sąrašas, kuriuos naudotojas yra įkėlęs. Paspaudus ant pasirinkto albumo – atidaromas nuotraukų peržiūros langas.\n" +
                "Nuotraukų albumų lange naudotojas gali pridėti albumą ir nuotraukas. Naudotojas paspaudžia ant mygtuko [Pridėti[ (pliuso ženklas apačioje ekrano) – išvedamas modalinis langas, kuriame naudotojas supildo informaciją. Paspaudus [Išsaugoti] – sąrašas atnaujinamas ir matosi naujas pridėtas albumas.\n" +
                "Naudotojui paspaudus ant [Nustatymai] meniu punkto – atveriamas nustatymų langas, kuriame naudotojas gali atlikti keturis veiksmus:\n" +
                "• Paspausti mygtuką [Apie programėlę ir kūrėją] ir peržiūrėti informaciją apie  programėlę ir kūrėją.\n" +
                "• Paspausti mygtuką [Kaip naudotis?] ir peržiūrėti informaciją, kaip naudotis programėle.\n" +
                "• Paspausti mygtuką [Atsijungti] ir tęsti arba atšaukti atsijungimo procesą. Sėkmingai atsijungęs naudotojas nukreipiamas į pagrindinį langą.\n" +
                "• Paspausti mygtuką [Šalinti paskyrą] ir tęsti arba atšaukti paskyros šalinimo procesą. Sėkmingai pašalinus paskyrą – naudotojas nukreipiamas į pagrindinį langą.");

        // Set a button to close the dialog
        builder.setPositiveButton("Uždaryti", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Dismiss the dialog
            }
        });

        // Create and show the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteUserAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Get the user's ID
            String userId = user.getUid();

            // Get a reference to the user's directory in Firebase Storage
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference userRef = storage.getReference().child("users/" + userId);

            // List all items (albums) in the user's directory
            userRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    // Iterate through each album directory
                    for (StorageReference albumRef : listResult.getPrefixes()) {
                        // List all items (photos) in the album directory
                        albumRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                            @Override
                            public void onSuccess(ListResult listResult) {
                                // Iterate through each photo in the album directory
                                for (StorageReference photoRef : listResult.getItems()) {
                                    // Delete the photo
                                    photoRef.delete().addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Handle photo deletion failure
                                            Toast.makeText(Settings.this, "Failed to delete photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Handle listing photos failure
                                Toast.makeText(Settings.this, "Failed to list photos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Handle listing albums failure
                    Toast.makeText(Settings.this, "Failed to list albums: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            // Proceed with deleting the user account from Firebase Authentication
            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        // User account deleted successfully
                        // Redirect to login screen
                        Intent intent = new Intent(Settings.this, LogIn.class);
                        startActivity(intent);
                    } else {
                        // Handle account deletion failure
                        //Toast.makeText(Settings.this, "Failed to delete account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Settings.this, LogIn.class);
                        startActivity(intent);
                    }
                }
            });
        } else {
            // User is already logged out or doesn't exist
            // Redirect to login screen
            Intent intent = new Intent(Settings.this, LogIn.class);
            startActivity(intent);
        }
    }



    private void deleteUserDataFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid())
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent intent = new Intent(Settings.this, LogIn.class);
                            startActivity(intent);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle Firestore deletion failure
                            Toast.makeText(Settings.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        Settings.this.finishAffinity();
                        System.exit(0);
                    }
                })
                .setCancelable(false)
                .show();
    }
}