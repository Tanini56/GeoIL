package com.example.geoil_geoguessrisraellearner;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewCommunityGameActivity extends AppCompatActivity implements OnMapReadyCallback {

    private List<String> communityImageUrls = new ArrayList<>();
    private List<Map<String, Double>> communityLocations = new ArrayList<>();
    private List<LatLng> userGuesses = new ArrayList<>();

    private int totalRounds = 5;
    private int currentRound = 1;
    private int totalScore = 0;
    private LatLng currentTargetLatLng;
    private boolean isMapVisible = false;

    private GoogleMap mGuessMap;
    private Marker userGuessMarker;

    private TextView tvScore, tvRound, tvMapTitle;
    private ImageView imgCommunityPhoto;
    private CardView guessMapContainer;
    private FloatingActionButton btnOpenMap;

    private final LatLng ISRAEL_CENTER = new LatLng(31.0461, 34.8516);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_game_clean);

        // Link Views
        tvScore = findViewById(R.id.tv_game_score);
        tvRound = findViewById(R.id.tv_game_round);
        tvMapTitle = findViewById(R.id.tv_map_title);
        imgCommunityPhoto = findViewById(R.id.img_community_photo);
        guessMapContainer = findViewById(R.id.guess_map_container);
        btnOpenMap = findViewById(R.id.btn_open_guess_map);
        Button btnConfirm = findViewById(R.id.btn_confirm_guess);
        ImageButton btnSettings = findViewById(R.id.btn_game_settings);
        ImageButton btnCloseMap = findViewById(R.id.btn_close_guess_map);

        // Setup Guessing Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Fetch Data
        String mapId = getIntent().getStringExtra("SELECTED_MAP_ID");
        if (mapId != null) {
            fetchCommunityMapData(mapId);
        }

        // Click Logic
        btnOpenMap.setOnClickListener(v -> toggleGuessMap());

        btnConfirm.setOnClickListener(v -> {
            if (userGuessMarker == null) {
                Toast.makeText(this, "Please place a pin first!", Toast.LENGTH_SHORT).show();
                return;
            }
            processUserGuess();
        });

        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnCloseMap.setOnClickListener(v -> toggleGuessMap());
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

    private void fetchCommunityMapData(String id) {
        FirebaseFirestore.getInstance().collection("community_maps").document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("mapName");
                        if (name != null) tvMapTitle.setText(name);

                        communityImageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        communityLocations = (List<Map<String, Double>>) documentSnapshot.get("locations");

                        if (communityLocations != null && !communityLocations.isEmpty()) {
                            totalRounds = communityLocations.size();
                            loadRound(1);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching records", Toast.LENGTH_SHORT).show());
    }

    private void loadRound(int roundNumber) {
        // Parse Locations Safely
        Map<String, Double> loc = communityLocations.get(roundNumber - 1);
        currentTargetLatLng = new LatLng(loc.get("lat"), loc.get("lng"));

        // Render Image using Glide
        String imageUrl = communityImageUrls.get(roundNumber - 1);
        Glide.with(this)
                .load(imageUrl)
                .into(imgCommunityPhoto);

        tvRound.setText("ROUND: " + roundNumber + " / " + totalRounds);
    }

    private void toggleGuessMap() {
        if (isMapVisible) {
            guessMapContainer.setVisibility(View.GONE);
            btnOpenMap.setImageResource(R.drawable.ic_map);
            isMapVisible = false;
        } else {
            guessMapContainer.setVisibility(View.VISIBLE);
            btnOpenMap.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            isMapVisible = true;
        }
    }

    private void processUserGuess() {
        LatLng userPos = userGuessMarker.getPosition();
        userGuesses.add(userPos);

        float[] results = new float[1];
        android.location.Location.distanceBetween(
                userPos.latitude, userPos.longitude,
                currentTargetLatLng.latitude, currentTargetLatLng.longitude,
                results
        );

        float distanceInMeters = results[0];
        int roundScore = calculateScore(distanceInMeters);
        totalScore += roundScore;

        tvScore.setText("SCORE: " + totalScore);
        guessMapContainer.setVisibility(View.GONE);
        isMapVisible = false;
        btnOpenMap.setImageResource(R.drawable.ic_map);

        showRoundResultDialog(roundScore, (int) distanceInMeters);
    }

    private int calculateScore(double distanceInMeters) {
        if (distanceInMeters <= 50) return 5000;
        if (distanceInMeters >= 400000) return 0;
        return (int) Math.round(5000 * (1 - (distanceInMeters - 50.0) / (400000.0 - 50.0)));
    }

    private void showRoundResultDialog(int score, int distanceMeters) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_round_result, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TransparentDialog).setView(dialogView).setCancelable(false).create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        TextView tvResScore = dialogView.findViewById(R.id.result_score_text);
        TextView tvResDist = dialogView.findViewById(R.id.result_distance_text);
        Button btnNext = dialogView.findViewById(R.id.btn_next_round);

        tvResScore.setText("+" + score);
        tvResDist.setText(distanceMeters < 1000 ? "Distance: " + distanceMeters + "m" : String.format("Distance: %.2f km", distanceMeters / 1000.0));

        SupportMapFragment resultMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.result_mini_map);
        if (resultMapFragment != null) {
            resultMapFragment.getMapAsync(googleMap -> {
                LatLng userPos = userGuesses.get(userGuesses.size() - 1);
                googleMap.addMarker(new MarkerOptions().position(userPos).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                googleMap.addMarker(new MarkerOptions().position(currentTargetLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                googleMap.addPolyline(new PolylineOptions().add(userPos, currentTargetLatLng).width(8).color(Color.WHITE));

                LatLngBounds bounds = new LatLngBounds.Builder().include(userPos).include(currentTargetLatLng).build();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            });
        }

        btnNext.setOnClickListener(v -> {
            if (resultMapFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(resultMapFragment).commit();
            }
            dialog.dismiss();
            prepareNextRound();
        });
    }

    private void prepareNextRound() {
        if (currentRound < totalRounds) {
            currentRound++;
            if (userGuessMarker != null) userGuessMarker.remove();
            userGuessMarker = null;
            loadRound(currentRound);
        } else {
            showFinalScore();
        }
    }

    private void showFinalScore() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_summary, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TransparentDialog).setView(dialogView).setCancelable(false).create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        TextView tvFinalScore = dialogView.findViewById(R.id.final_score_text);
        Button btnFinish = dialogView.findViewById(R.id.btn_finish_game);
        tvFinalScore.setText(String.valueOf(totalScore));

        SupportMapFragment summaryMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.final_summary_map);
        if (summaryMapFragment != null) {
            summaryMapFragment.getMapAsync(googleMap -> {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (int i = 0; i < totalRounds; i++) {
                    Map<String, Double> loc = communityLocations.get(i);
                    LatLng actual = new LatLng(loc.get("lat"), loc.get("lng"));
                    LatLng guess = userGuesses.get(i);

                    googleMap.addMarker(new MarkerOptions().position(actual).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                    googleMap.addMarker(new MarkerOptions().position(guess).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    googleMap.addPolyline(new PolylineOptions().add(actual, guess).width(4).color(Color.parseColor("#80FFFFFF")));

                    builder.include(actual).include(guess);
                }
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            });
        }

        btnFinish.setOnClickListener(v -> {
            String mapId = getIntent().getStringExtra("SELECTED_MAP_ID");
            if (mapId != null) saveHighScore(mapId, totalScore);
            if (summaryMapFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(summaryMapFragment).commit();
            }
            dialog.dismiss();
            finish();
        });
    }

    private void saveHighScore(String mapId, int finalScore) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .collection("scores").document(mapId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentBest = documentSnapshot.getLong("highScore");
                        if (currentBest != null && finalScore > currentBest) {
                            updateScoreInFirestore(userId, mapId, finalScore);
                        }
                    } else {
                        updateScoreInFirestore(userId, mapId, finalScore);
                    }
                });
    }

    private void updateScoreInFirestore(String userId, String mapId, int score) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("highScore", score);
        mapData.put("lastPlayed", System.currentTimeMillis());

        db.collection("users").document(userId).collection("scores").document(mapId).set(mapData);
        db.collection("users").document(userId).update("score", FieldValue.increment(score));
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create();
        dialogView.findViewById(R.id.btn_resume).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_exit).setOnClickListener(v -> finish());
        dialog.show();
    }
}