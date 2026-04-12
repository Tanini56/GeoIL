package com.example.geoil_geoguessrisraellearner;



import android.content.Intent;

import android.os.Bundle;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.Button;

import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.appcompat.widget.SearchView;

import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;



import com.google.android.material.button.MaterialButton;

import com.google.android.material.button.MaterialButtonToggleGroup;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.Query;



import java.util.ArrayList;

import java.util.List;

public class ChallengesFragment extends Fragment {

    private View challengesContentView, guestView, containerOfficial, containerCommunity;
    private Button guestLoginBtn;
    private MaterialButton btnCreateMap;
    private RecyclerView rvCommunityMaps;
    private RecyclerView rvOfficialMaps; // Added
    private SearchView searchView;

    private CommunityMapAdapter adapter;
    private List<CommunityMap> mapList;

    private OfficialMapAdapter officialAdapter; // Added
    private List<OfficialMap> officialMapList;   // Added

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenges, container, false);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize Views
        challengesContentView = view.findViewById(R.id.challenges_content_view);
        guestView = view.findViewById(R.id.guest_view);
        containerOfficial = view.findViewById(R.id.container_official);
        containerCommunity = view.findViewById(R.id.container_community);
        guestLoginBtn = view.findViewById(R.id.guest_login_btn);
        btnCreateMap = view.findViewById(R.id.btn_create_map);
        rvCommunityMaps = view.findViewById(R.id.rv_community_maps);
        rvOfficialMaps = view.findViewById(R.id.rv_official_maps); // Initialize this!
        searchView = view.findViewById(R.id.search_view_community);
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);

        // 2. Check Login State
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            challengesContentView.setVisibility(View.VISIBLE);
            guestView.setVisibility(View.GONE);
            setupCommunityList();
            setupOfficialList(); // Setup the official list too
        } else {
            challengesContentView.setVisibility(View.GONE);
            guestView.setVisibility(View.VISIBLE);
        }

        // 3. Toggle Logic (Official vs Community)
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_official) {
                    containerOfficial.setVisibility(View.VISIBLE);
                    containerCommunity.setVisibility(View.GONE);
                    fetchOfficialMaps(); // Load official data
                } else if (checkedId == R.id.btn_community) {
                    containerOfficial.setVisibility(View.GONE);
                    containerCommunity.setVisibility(View.VISIBLE);
                    fetchCommunityMaps(); // Load community data
                }
            }
        });

        // 4. Click Listeners
        guestLoginBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        btnCreateMap.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MapCreatorActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void setupOfficialList() {
        officialMapList = new ArrayList<>();
        rvOfficialMaps.setLayoutManager(new LinearLayoutManager(getContext()));

        officialAdapter = new OfficialMapAdapter(officialMapList, map -> {
            Intent intent = new Intent(getActivity(), CommunityGameActivity.class);
            intent.putExtra("SELECTED_MAP_ID", map.getMapId());
            intent.putExtra("SELECTED_MAP_NAME", map.getMapName());
            intent.putExtra("IS_OFFICIAL", true); // Flag to tell the game it's an official map
            startActivity(intent);
        });

        rvOfficialMaps.setAdapter(officialAdapter);
        fetchOfficialMaps();
    }

    private void setupCommunityList() {
        mapList = new ArrayList<>();
        rvCommunityMaps.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommunityMapAdapter(getContext(), mapList, map -> {
            Intent intent = new Intent(getActivity(), CommunityGameActivity.class);
            intent.putExtra("SELECTED_MAP_NAME", map.getMapName());
            intent.putExtra("IS_OFFICIAL", false); // Community map
            startActivity(intent);
        });

        rvCommunityMaps.setAdapter(adapter);
        fetchCommunityMaps();
    }

    private void fetchOfficialMaps() {
        db.collection("official_maps")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    officialMapList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Create the object using data from Firestore
                        OfficialMap map = new OfficialMap(doc.getString("mapName"), doc.getId());
                        officialMapList.add(map);
                    }
                    officialAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchCommunityMaps() {
        db.collection("community_maps")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<CommunityMap> fetchedMaps = queryDocumentSnapshots.toObjects(CommunityMap.class);
                        mapList.clear();
                        mapList.addAll(fetchedMaps);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            fetchCommunityMaps();
            fetchOfficialMaps(); // Ensure both are fresh
        }
    }
}