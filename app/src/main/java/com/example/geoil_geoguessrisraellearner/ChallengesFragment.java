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

public class ChallengesFragment extends Fragment {

    private LinearLayout challengesContentView, guestView;
    private Button loginBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenges, container, false);

        // Link IDs
        challengesContentView = view.findViewById(R.id.challenges_content_view);
        guestView = view.findViewById(R.id.guest_view);
        loginBtn = view.findViewById(R.id.guest_login_btn);

        // Redirect to Login
        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
        });

        updateUI();

        return view;
    }

    private void updateUI() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            challengesContentView.setVisibility(View.VISIBLE);
            guestView.setVisibility(View.GONE);
        } else {
            challengesContentView.setVisibility(View.GONE);
            guestView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}