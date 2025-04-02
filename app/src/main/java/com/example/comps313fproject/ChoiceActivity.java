package com.example.comps313fproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class ChoiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);


        ImageButton childButton = findViewById(R.id.childButton);

        ImageButton caregiverButton = findViewById(R.id.caregiverButton);


        childButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChoiceActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


        caregiverButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChoiceActivity.this, CaregiverActivity.class);
            startActivity(intent);
            finish();
        });
    }
}