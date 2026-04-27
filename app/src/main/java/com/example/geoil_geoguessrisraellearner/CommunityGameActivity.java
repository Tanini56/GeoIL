package com.example.geoil_geoguessrisraellearner;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Added OnMapReadyCallback here
public class CommunityGameActivity extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback, OnMapReadyCallback {

    private StreetViewPanorama mStreetView;
    private TextView tvScore, tvRound, tvMapTitle, tvDifficulty;
    private boolean isMusicOn = true;

    private int currentRound = 1;
    private final int TOTAL_ROUNDS = 5;
    private int totalScore = 0;
    private boolean canMove = false;

    private CardView guessMapContainer;
    private boolean isMapVisible = false;
    private GoogleMap mGuessMap;
    private Marker userGuessMarker;
    private final LatLng ISRAEL_CENTER = new LatLng(31.0461, 34.8516);

    private List<LatLng> gamePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_game);

        // 1. Initialize UI
        tvScore = findViewById(R.id.tv_game_score);
        tvRound = findViewById(R.id.tv_game_round);
        tvMapTitle = findViewById(R.id.tv_map_title);
        tvDifficulty = findViewById(R.id.tv_difficulty);

        ImageButton btnSettings = findViewById(R.id.btn_game_settings);
        ImageButton btnReturn = findViewById(R.id.btn_return_to_start);
        guessMapContainer = findViewById(R.id.guess_map_container);
        FloatingActionButton btnOpenMap = findViewById(R.id.btn_open_guess_map);

        // Mini-map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_map_fragment);

        // 2. Get Data from Intent
        String mapId = getIntent().getStringExtra("SELECTED_MAP_ID");
        boolean isOfficial = getIntent().getBooleanExtra("IS_OFFICIAL", false);

        if (isOfficial && mapId != null) {
            fetchOfficialMapData(mapId);
        }

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 3. Initialize StreetView
        SupportStreetViewPanoramaFragment streetViewFragment = (SupportStreetViewPanoramaFragment)
                getSupportFragmentManager().findFragmentById(R.id.game_streetview);
        if (streetViewFragment != null) {
            streetViewFragment.getStreetViewPanoramaAsync(this);
        }

        // 4. Listeners
        btnReturn.setOnClickListener(v -> {
            if (mStreetView != null && !gamePoints.isEmpty()) {
                LatLng startPoint = gamePoints.get(currentRound - 1);
                mStreetView.setPosition(startPoint, 100, StreetViewSource.OUTDOOR);
            }
        });

        btnOpenMap.setOnClickListener(v -> {
            if (isMapVisible) {
                // HIDE IT
                guessMapContainer.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    guessMapContainer.setVisibility(View.GONE);
                });
                btnOpenMap.setImageResource(R.drawable.ic_map);
                isMapVisible = false;
            } else {
                // SHOW IT
                guessMapContainer.setAlpha(0f);
                guessMapContainer.setVisibility(View.VISIBLE);
                guessMapContainer.animate().alpha(1f).setDuration(200);
                btnOpenMap.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isMapVisible = true;
            }
        });

        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGuessMap = googleMap;
        mGuessMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGuessMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ISRAEL_CENTER, 7.2f));

        mGuessMap.setOnMapClickListener(latLng -> {
            if (userGuessMarker != null) userGuessMarker.remove();
            userGuessMarker = mGuessMap.addMarker(new MarkerOptions().position(latLng));
        });
    }

    private void fetchOfficialMapData(String id) {
        FirebaseFirestore.getInstance().collection("official_maps").document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 1. Get the movement flag
                        Boolean moveFlag = documentSnapshot.getBoolean("isMoveEnabled");
                        canMove = (moveFlag != null) ? moveFlag : false;

                        // 2. Update Labels
                        String name = documentSnapshot.getString("mapName");
                        String diff = documentSnapshot.getString("difficulty");
                        if (name != null) tvMapTitle.setText(name);
                        if (diff != null) tvDifficulty.setText(diff);

                        // 3. Parse Points
                        List<Object> pointsRaw = (List<Object>) documentSnapshot.get("points");
                        if (pointsRaw != null) {
                            gamePoints.clear();
                            for (Object p : pointsRaw) {
                                Map<String, Object> pointData = (Map<String, Object>) p;
                                double lat = ((Number) pointData.get("lat")).doubleValue();
                                double lng = ((Number) pointData.get("lng")).doubleValue();
                                gamePoints.add(new LatLng(lat, lng));
                            }
                        }

                        // Apply move setting immediately if panorama is ready
                        if (mStreetView != null) {
                            mStreetView.setUserNavigationEnabled(canMove);
                        }

                        // Load round 1
                        if (!gamePoints.isEmpty()) {
                            loadRound(1);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show());
    }

    private void loadRound(int roundNumber) {
        if (gamePoints == null || gamePoints.isEmpty()) return;

        LatLng target = gamePoints.get(roundNumber - 1);

        if (mStreetView != null) {
            mStreetView.setPosition(target, 1000, StreetViewSource.OUTDOOR);
            mStreetView.setUserNavigationEnabled(canMove);
            mStreetView.setZoomGesturesEnabled(true);
            mStreetView.setPanningGesturesEnabled(true);
        }
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        mStreetView = panorama;
        mStreetView.setUserNavigationEnabled(canMove);
        tvScore.setText("SCORE: " + totalScore);

        if (!gamePoints.isEmpty()) {
            loadRound(currentRound);
        }
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        Button btnResume = dialogView.findViewById(R.id.btn_resume);
        Button btnExit = dialogView.findViewById(R.id.btn_exit);
        Button btnMusic = dialogView.findViewById(R.id.btn_toggle_music);

        btnMusic.setText(isMusicOn ? "Music: ON" : "Music: OFF");

        btnResume.setOnClickListener(v -> dialog.dismiss());
        btnExit.setOnClickListener(v -> finish());
        btnMusic.setOnClickListener(v -> {
            isMusicOn = !isMusicOn;
            btnMusic.setText(isMusicOn ? "Music: ON" : "Music: OFF");
        });

        dialog.show();
    }
}