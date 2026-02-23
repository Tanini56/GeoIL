package com.example.geoil_geoguessrisraellearner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;

public class LeaderboardFragment extends Fragment {

    private LinearLayout leaderboardContentView, guestView;
    private Button loginBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        // Initialize views
        leaderboardContentView = view.findViewById(R.id.leaderboard_content_view);
        guestView = view.findViewById(R.id.guest_view);
        loginBtn = view.findViewById(R.id.guest_login_btn);

        // Click listener for the login button
        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        updateUI();

        return view;
    }

    private void updateUI() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // User is logged in
            leaderboardContentView.setVisibility(View.VISIBLE);
            guestView.setVisibility(View.GONE);
        } else {
            // User is a guest
            leaderboardContentView.setVisibility(View.GONE);
            guestView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh visibility in case the user just logged in
        updateUI();
    }
}