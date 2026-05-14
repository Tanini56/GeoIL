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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private LinearLayout leaderboardContentView, guestView;
    private Button loginBtn;
    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<UserScore> userList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        leaderboardContentView = view.findViewById(R.id.leaderboard_content_view);
        guestView = view.findViewById(R.id.guest_view);
        loginBtn = view.findViewById(R.id.guest_login_btn);

        recyclerView = view.findViewById(R.id.leaderboard_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(userList);
        recyclerView.setAdapter(adapter);

        loginBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
        });

        updateUI();

        return view;
    }

    private void updateUI() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            leaderboardContentView.setVisibility(View.VISIBLE);
            guestView.setVisibility(View.GONE);
            fetchLeaderboardData();
        } else {
            leaderboardContentView.setVisibility(View.GONE);
            guestView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchLeaderboardData() {
        FirebaseFirestore.getInstance().collection("users")
                .orderBy("score", Query.Direction.DESCENDING) // image_765e95.png data structure
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    userList.addAll(queryDocumentSnapshots.toObjects(UserScore.class));
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
}