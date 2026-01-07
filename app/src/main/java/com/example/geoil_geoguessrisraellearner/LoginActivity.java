package com.example.geoil_geoguessrisraellearner;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private boolean isLogin = true;

    TextView titleText;
    Button loginToggle, signupToggle, actionButton;
    EditText emailInput, passwordInput, confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find views
        titleText = findViewById(R.id.titleText);
        loginToggle = findViewById(R.id.loginToggle);
        signupToggle = findViewById(R.id.signupToggle);
        actionButton = findViewById(R.id.actionButton);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        // Initialize toggle colors on first load
        loginToggle.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.primary)));
        loginToggle.setTextColor(getColor(R.color.white));

        signupToggle.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
        signupToggle.setTextColor(getColor(R.color.primary));

        // Action button
        actionButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.primary)));
        actionButton.setTextColor(getColor(R.color.white));

        // Click listeners
        loginToggle.setOnClickListener(v -> switchMode(true));
        signupToggle.setOnClickListener(v -> switchMode(false));

        actionButton.setOnClickListener(v -> {
            if (isLogin) {
                // TODO: Login logic
            } else {
                // TODO: Sign Up logic
            }
        });
    }

    private void switchMode(boolean loginMode) {
        isLogin = loginMode;

        if (loginMode) {
            // LOGIN active
            loginToggle.setBackgroundResource(R.drawable.toggle_selected_rect);
            loginToggle.setTextColor(getColor(R.color.white));

            signupToggle.setBackgroundResource(R.drawable.toggle_unselected_rect);
            signupToggle.setTextColor(getColor(R.color.primary));

            titleText.setText("Login");
            confirmPasswordInput.setVisibility(View.GONE);
            actionButton.setText("Login");

        } else {
            // SIGN UP active
            signupToggle.setBackgroundResource(R.drawable.toggle_selected_rect);
            signupToggle.setTextColor(getColor(R.color.white));

            loginToggle.setBackgroundResource(R.drawable.toggle_unselected_rect);
            loginToggle.setTextColor(getColor(R.color.primary));

            titleText.setText("Sign Up");
            confirmPasswordInput.setVisibility(View.VISIBLE);
            actionButton.setText("Sign Up");
        }
    }

}
