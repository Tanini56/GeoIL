package com.example.geoil_geoguessrisraellearner;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button loginButton, guestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.loginButton);
        guestButton = findViewById(R.id.guestButton);

        // Apply palette colors immediately
        loginButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.primary)));
        guestButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.secondary)));

        // Set click actions
        loginButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LoginActivity.class)));
        guestButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GameActivity.class)));
    }
}
