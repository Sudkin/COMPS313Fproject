package com.example.comps313fproject;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private DatabaseReference dbRef;
    private TextView nameTextView, ageTextView, emailTextView, phoneTextView, emergencyContactTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameTextView = findViewById(R.id.nameTextView);
        ageTextView = findViewById(R.id.ageTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        emergencyContactTextView = findViewById(R.id.emergencyContactTextView);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            dbRef = FirebaseDatabase.getInstance("https://childlink-b5304-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("Users").child(userId);
            loadUserData();
        }

    }

    private void loadUserData() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    Long age = snapshot.child("age").getValue(Long.class);
                    String email = snapshot.child("email").getValue(String.class);
                    Long phoneNumber = snapshot.child("phonenumber").getValue(Long.class);
                    String emergencyContact = snapshot.child("emergencyContact").getValue(String.class);

                    nameTextView.setText(name != null ? name : "N/A");
                    ageTextView.setText(age != null ? age.toString() : "N/A");
                    emailTextView.setText(email != null ? email : "N/A");
                    phoneTextView.setText(phoneNumber != null ? phoneNumber.toString() : "N/A");
                    emergencyContactTextView.setText(emergencyContact != null ? emergencyContact : "N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
