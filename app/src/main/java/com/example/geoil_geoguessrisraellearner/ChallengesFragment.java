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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChallengesFragment extends Fragment {

    private View challengesContentView, guestView, containerOfficial, containerCommunity;
    private Button guestLoginBtn;
    private MaterialButton btnCreateMap;
    private RecyclerView rvCommunityMaps;
    private SearchView searchView;

    private CommunityMapAdapter adapter;

    private List<CommunityMap> mapList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenges, container, false);

        // 1. Initialize Views
        challengesContentView = view.findViewById(R.id.challenges_content_view);
        guestView = view.findViewById(R.id.guest_view);
        containerOfficial = view.findViewById(R.id.container_official);
        containerCommunity = view.findViewById(R.id.container_community);
        guestLoginBtn = view.findViewById(R.id.guest_login_btn);

        btnCreateMap = view.findViewById(R.id.btn_create_map);
        rvCommunityMaps = view.findViewById(R.id.rv_community_maps);
        searchView = view.findViewById(R.id.search_view_community);
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);

        // 2. Check Login State
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            challengesContentView.setVisibility(View.VISIBLE);
            guestView.setVisibility(View.GONE);
            setupCommunityList();
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
                } else if (checkedId == R.id.btn_community) {
                    containerOfficial.setVisibility(View.GONE);
                    containerCommunity.setVisibility(View.VISIBLE);
                }
            }
        });

        // 4. Click Listeners
        guestLoginBtn.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        view.findViewById(R.id.btn_create_map).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MapCreatorActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void setupCommunityList() {
        mapList = new ArrayList<>();
        // Mock data for testing (Replace with Firebase fetch later)
        // Now passing both the category "City" AND the current time
        mapList.add(new CommunityMap("Haifa Secrets", "Nizar123", "City", System.currentTimeMillis()));

        // Sort by Newest (Timestamp descending)
        Collections.sort(mapList, (m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));

        rvCommunityMaps.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommunityMapAdapter(getContext(), mapList, map -> {
            // This code runs when a map is clicked
            // For now, let's just toast the name. Later, you'll start the game here.
            Toast.makeText(getContext(), "Starting: " + map.getMapName(), Toast.LENGTH_SHORT).show();
        });
        rvCommunityMaps.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                // adapter.getFilter().filter(newText);
                return true;
            }
        });
    }
}