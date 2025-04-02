package com.example.comps313fproject;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class CaregiverActivity extends AppCompatActivity {

    private DatabaseReference dbRef;
    private TextView CuserNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver);

        CuserNameTextView = findViewById(R.id.cuserNameTextView);

        Button ClogoutButton = findViewById(R.id.clogoutButton);

        Button CviewProfileButton = findViewById(R.id.cviewProfileButton);

        Button CmapButton = findViewById(R.id.cmapButton);

        ClogoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent loginIntent = new Intent(CaregiverActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish();
        });

        CviewProfileButton.setOnClickListener(v -> {
            Intent profileIntent = new Intent(CaregiverActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        });

        CmapButton.setOnClickListener(v -> {
            Intent mapIntent = new Intent(CaregiverActivity.this, MapsActivity.class);
            startActivity(mapIntent);
        });


        FirebaseAuth user = FirebaseAuth.getInstance();
        if (user.getCurrentUser() != null) {
            String userId = user.getUid();
            assert userId != null;
            dbRef = FirebaseDatabase.getInstance("https://childlink-b5304-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Users").child(userId);
            loadUserName();
        }

    }
    private void loadUserName() {
        dbRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    CuserNameTextView.setText("Username: " + name);
                } else {
                    Toast.makeText(CaregiverActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CaregiverActivity.this, "DatabaseError" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }






}



