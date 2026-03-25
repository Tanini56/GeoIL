package com.example.geoil_geoguessrisraellearner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountFragment extends Fragment {

    private LinearLayout guestView, loggedInView;
    private TextView userNameTxt, userEmailTxt;
    private Button loginBtn, logoutBtn;
    private ImageView geoguessrBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Initialize UI Elements
        guestView = view.findViewById(R.id.guest_view);
        loggedInView = view.findViewById(R.id.logged_in_view);
        userNameTxt = view.findViewById(R.id.user_name_display);
        userEmailTxt = view.findViewById(R.id.user_email_display);
        loginBtn = view.findViewById(R.id.guest_login_btn);
        logoutBtn = view.findViewById(R.id.logout_btn);

        // The GeoGuessr Promo Button
        geoguessrBtn = view.findViewById(R.id.geoguessr_logo_btn);

        // 3. Set up listeners
        geoguessrBtn.setOnClickListener(v -> openGeoGuessr());

        // 4. Update UI based on auth state
        updateUI();

        return view;
    }

    private void updateUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            guestView.setVisibility(View.GONE);
            loggedInView.setVisibility(View.VISIBLE);

            // Set email immediately
            userEmailTxt.setText(currentUser.getEmail());

            // --- IMPROVED NAME LOGIC ---
            // Step 1: Set a default name immediately (Email prefix)
            String email = currentUser.getEmail();
            String fallbackName = (email != null && email.contains("@")) ? email.split("@")[0] : "Explorer";
            userNameTxt.setText(fallbackName);

            // Step 2: Try to get a custom name from Firestore if it exists
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("username")) {
                            String firestoreName = documentSnapshot.getString("username");
                            if (firestoreName != null && !firestoreName.isEmpty()) {
                                userNameTxt.setText(firestoreName);
                            }
                        }
                    });
            // ---------------------------

            logoutBtn.setOnClickListener(v -> handleLogout());
        } else {
            guestView.setVisibility(View.VISIBLE);
            loggedInView.setVisibility(View.GONE);
            loginBtn.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), LoginActivity.class));
            });
        }
    }

    private void openGeoGuessr() {
        String packageName = "com.geoguessr.app";
        // Check if app is installed
        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(packageName);

        if (intent != null) {
            // Found it! Open it.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            // Not found. Send to Play Store.
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            }
        }
    }

    private void handleLogout() {
        mAuth.signOut();
        Toast.makeText(getActivity(), "Logged out", Toast.LENGTH_SHORT).show();

        // Clear stack and send back to Login/Welcome page
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI(); // Refresh state if user logs in and comes back
    }
}