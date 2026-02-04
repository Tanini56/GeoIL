package com.example.geoil_geoguessrisraellearner;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private boolean isLogin = true;

    // UI Elements
    TextView titleText;
    Button loginToggle, signupToggle, actionButton, googleBtn;
    EditText emailInput, passwordInput, confirmPasswordInput, usernameInput;

    // Firebase & Google
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Find views
        titleText = findViewById(R.id.titleText);
        loginToggle = findViewById(R.id.loginToggle);
        signupToggle = findViewById(R.id.signupToggle);
        actionButton = findViewById(R.id.actionButton);
        googleBtn = findViewById(R.id.googleSignInButton);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        usernameInput = findViewById(R.id.usernameInput); // New field

        // Initialize toggle colors on first load
        initializeUIColors();

        // Click listeners
        loginToggle.setOnClickListener(v -> switchMode(true));
        signupToggle.setOnClickListener(v -> switchMode(false));

        actionButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLogin) {
                handleLogin(email, password);
            } else {
                handleSignup(email, password);
            }
        });

        googleBtn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    private void initializeUIColors() {
        int primary = getColor(R.color.primary);
        int white = getColor(R.color.white);

        loginToggle.setBackgroundTintList(ColorStateList.valueOf(primary));
        loginToggle.setTextColor(white);

        signupToggle.setBackgroundTintList(ColorStateList.valueOf(white));
        signupToggle.setTextColor(primary);

        actionButton.setBackgroundTintList(ColorStateList.valueOf(primary));
        actionButton.setTextColor(white);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Failed: " + e.getStatusCode());
                Toast.makeText(this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Check if user is new to create Firestore profile
                        boolean isNew = task.getResult().getAdditionalUserInfo().isNewUser();
                        if (isNew && user != null) {
                            saveUserToFirestore(user.getEmail(), user.getDisplayName());
                        } else {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        }
                    } else {
                        Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSignup(String email, String password) {
        String username = usernameInput.getText().toString().trim();
        String confirmPass = confirmPasswordInput.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(email, username);
                    } else {
                        Toast.makeText(this, "Signup Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String email, String username) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("username", username);
        user.put("score", 0);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void switchMode(boolean loginMode) {
        isLogin = loginMode;
        int primaryColor = getColor(R.color.primary);
        int whiteColor = getColor(R.color.white);

        if (loginMode) {
            loginToggle.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            signupToggle.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
            loginToggle.setTextColor(whiteColor);
            signupToggle.setTextColor(primaryColor);

            titleText.setText("");
            usernameInput.setVisibility(View.GONE);
            confirmPasswordInput.setVisibility(View.GONE);
            actionButton.setText("Log In");
        } else {
            signupToggle.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
            loginToggle.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
            signupToggle.setTextColor(whiteColor);
            loginToggle.setTextColor(primaryColor);

            titleText.setText("");
            usernameInput.setVisibility(View.VISIBLE);
            confirmPasswordInput.setVisibility(View.VISIBLE);
            actionButton.setText("Sign Up");
        }
    }
}