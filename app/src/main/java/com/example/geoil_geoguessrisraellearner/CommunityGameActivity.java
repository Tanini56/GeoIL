package com.example.geoil_geoguessrisraellearner;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommunityGameActivity extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback {

    private StreetViewPanorama mStreetView;
    private TextView tvScore, tvRound, tvMapTitle, tvDifficulty;
    private boolean isMusicOn = true;

    private int currentRound = 1;
    private final int TOTAL_ROUNDS = 5;
    private int totalScore = 0;

    // List to hold the 5 locations from Firebase
    private List<LatLng> gamePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_game);

        // 1. Initialize UI
        tvScore = findViewById(R.id.tv_game_score);
        tvRound = findViewById(R.id.tv_game_round);
        tvMapTitle = findViewById(R.id.tv_map_title); // Add this to your XML if not there
        tvDifficulty = findViewById(R.id.tv_difficulty); // Add this to your XML if not there

        ImageButton btnSettings = findViewById(R.id.btn_game_settings);
        FloatingActionButton btnMap = findViewById(R.id.btn_open_guess_map);

        // 2. Get Data from Intent
        String mapId = getIntent().getStringExtra("SELECTED_MAP_ID");
        boolean isOfficial = getIntent().getBooleanExtra("IS_OFFICIAL", false);

        if (isOfficial && mapId != null) {
            fetchOfficialMapData(mapId);
        }

        // 3. Initialize StreetView
        SupportStreetViewPanoramaFragment streetViewFragment = (SupportStreetViewPanoramaFragment)
                getSupportFragmentManager().findFragmentById(R.id.game_streetview);
        if (streetViewFragment != null) {
            streetViewFragment.getStreetViewPanoramaAsync(this);
        }

        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnMap.setOnClickListener(v -> openGuessMap());
    }

    private void fetchOfficialMapData(String id) {
        FirebaseFirestore.getInstance().collection("official_maps").document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Update UI Labels
                        String name = documentSnapshot.getString("mapName");
                        String diff = documentSnapshot.getString("difficulty");
                        if (name != null) tvMapTitle.setText(name);
                        if (diff != null) tvDifficulty.setText(diff);

                        // Parse the Points Array
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

                        // Start first round if Panorama is already ready
                        if (mStreetView != null && !gamePoints.isEmpty()) {
                            loadRound(1);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show());
    }

    private void loadRound(int roundNumber) {
        if (gamePoints == null || gamePoints.isEmpty()) return;

        if (roundNumber <= TOTAL_ROUNDS) {
            tvRound.setText("ROUND: " + roundNumber + " / " + TOTAL_ROUNDS);
            LatLng target = gamePoints.get(roundNumber - 1);

            if (mStreetView != null) {
                // Set position with OUTDOOR source to avoid being stuck inside shops
                mStreetView.setPosition(target, 100, StreetViewSource.OUTDOOR);
                mStreetView.setUserNavigationEnabled(false); // NO MOVING
            }
        } else {
            Toast.makeText(this, "Game Over! Total Score: " + totalScore, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        mStreetView = panorama;
        mStreetView.setUserNavigationEnabled(false);

        // Initial UI setup
        tvScore.setText("SCORE: " + totalScore);

        // If Firebase data came in before the panorama was ready, load now
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

    private void openGuessMap() {
        // Next step: Implement the Fragment or Dialog where they place their marker
        Toast.makeText(this, "Opening Guess Map...", Toast.LENGTH_SHORT).show();
    }
}