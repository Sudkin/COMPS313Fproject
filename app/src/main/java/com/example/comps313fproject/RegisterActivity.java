package com.example.comps313fproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private EditText nameEditText, phoneNumberEditText, emailEditText, passwordEditText, ageEditText, emergencyContactEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        mAuth = FirebaseAuth.getInstance();

        dbRef = FirebaseDatabase.getInstance("https://childlink-b5304-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();


        nameEditText = findViewById(R.id.nameEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        ageEditText = findViewById(R.id.ageEditText);
        emergencyContactEditText = findViewById(R.id.emergencyContactEditText);
        Button registerButton = findViewById(R.id.registerButton);


        TextView loginRedirectTextView = findViewById(R.id.loginRedirectTextView);
        loginRedirectTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });


        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = nameEditText.getText().toString().trim();
        String phoneNumberStr = phoneNumberEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String emergencyContactStr = emergencyContactEditText.getText().toString().trim();


        if (name.isEmpty() || phoneNumberStr.isEmpty() || email.isEmpty() || password.isEmpty() || ageStr.isEmpty() || emergencyContactStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserInfoToRealtimeDatabase(user.getUid(), name, Integer.parseInt(phoneNumberStr), email, Integer.parseInt(ageStr), emergencyContactStr);
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserInfoToRealtimeDatabase(String userId, String name, int phoneNumber, String email, int age, String emergencyContact) {

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", name);
        userInfo.put("phonenumber", phoneNumber);
        userInfo.put("email", email);
        userInfo.put("age", age);
        userInfo.put("emergencyContact", emergencyContact);

        dbRef.child("Users").child(userId).setValue(userInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, ChoiceActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
