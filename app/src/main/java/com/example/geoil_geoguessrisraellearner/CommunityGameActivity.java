package com.example.geoil_geoguessrisraellearner;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewSource;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Added OnMapReadyCallback here
public class CommunityGameActivity extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback, OnMapReadyCallback {

    private boolean isCommunityMode = false;
    private List<String> communityImageUrls = new ArrayList<>();
    private List<Map<String, Double>> communityLocations = new ArrayList<>();
    private int totalRounds = 5; // Default

    private StreetViewPanorama mStreetView;
    private TextView tvScore, tvRound, tvMapTitle, tvDifficulty;
    private boolean isMusicOn = true;
    private List<LatLng> userGuesses = new ArrayList<>();
    private LatLng currentTargetLatLng; // Added for distance calculation
    private View streetViewLayout; // The FrameLayout/Fragment containing StreetView
    private Button btnSubmitGuess; // Reference for the submit button
    private TextView tvRoundIndicator; // Added to match the fetch logic
    private int currentRound = 1;
    private final int TOTAL_ROUNDS = 5;
    private int totalScore = 0;
    private boolean canMove = false;

    private CardView guessMapContainer;
    private boolean isMapVisible = false;
    private GoogleMap mGuessMap;
    private Marker userGuessMarker;
    private ImageView imgCommunityPhoto;
    private final LatLng ISRAEL_CENTER = new LatLng(31.0461, 34.8516);

    private List<LatLng> gamePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_game);

        // 1. Initialize UI Views
        tvScore = findViewById(R.id.tv_game_score);
        tvRound = findViewById(R.id.tv_game_round);
        tvMapTitle = findViewById(R.id.tv_map_title);
        tvDifficulty = findViewById(R.id.tv_difficulty);
        imgCommunityPhoto = findViewById(R.id.img_community_photo);

        // The container for your StreetView fragment (used for toggling visibility)
        streetViewLayout = findViewById(R.id.game_streetview);

        ImageButton btnSettings = findViewById(R.id.btn_game_settings);
        ImageButton btnReturn = findViewById(R.id.btn_return_to_start);
        guessMapContainer = findViewById(R.id.guess_map_container);
        FloatingActionButton btnOpenMap = findViewById(R.id.btn_open_guess_map);
        Button btnConfirm = findViewById(R.id.btn_confirm_guess);

        // 2. Initialize Map Fragments
        // Mini-map for making guesses
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mini_map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // StreetView fragment for official rounds
        SupportStreetViewPanoramaFragment streetViewFragment = (SupportStreetViewPanoramaFragment)
                getSupportFragmentManager().findFragmentById(R.id.game_streetview);
        if (streetViewFragment != null) {
            streetViewFragment.getStreetViewPanoramaAsync(this);
        }

        // 3. Handle Map Selection Logic
        String mapId = getIntent().getStringExtra("SELECTED_MAP_ID");
        boolean isOfficial = getIntent().getBooleanExtra("IS_OFFICIAL", false);

        if (mapId != null) {
            if (isOfficial) {
                fetchOfficialMapData(mapId); // Fetch logic from "official_maps"
            } else {
                fetchCommunityMapData(mapId); // Fetch logic from "community_maps"
            }
        }

        // 4. Click Listeners
        btnReturn.setOnClickListener(v -> {
            // Returns the view to the current round's starting coordinates
            if (currentTargetLatLng != null) {
                if (isCommunityMode) {
                    // For community mode, we just reset the ImageView zoom/position if needed
                    Toast.makeText(this, "Resetting view...", Toast.LENGTH_SHORT).show();
                } else if (mStreetView != null) {
                    mStreetView.setPosition(currentTargetLatLng, 100, StreetViewSource.OUTDOOR);
                }
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (userGuessMarker == null) {
                Toast.makeText(this, "Please place a pin on the map first!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save guess and calculate results using the universal currentTargetLatLng
            userGuesses.add(userGuessMarker.getPosition());
            LatLng userPos = userGuessMarker.getPosition();
            LatLng actualPos = currentTargetLatLng;

            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    userPos.latitude, userPos.longitude,
                    actualPos.latitude, actualPos.longitude,
                    results
            );

            float distanceInMeters = results[0];
            int roundScore = calculateScore(distanceInMeters);
            totalScore += roundScore;

            // Update UI and show results
            tvScore.setText("SCORE: " + totalScore);
            guessMapContainer.setVisibility(View.GONE);
            isMapVisible = false;
            btnOpenMap.setImageResource(R.drawable.ic_map);

            showRoundResultDialog(roundScore, (int) distanceInMeters);
        });

        btnOpenMap.setOnClickListener(v -> {
            if (isMapVisible) {
                guessMapContainer.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    guessMapContainer.setVisibility(View.GONE);
                });
                btnOpenMap.setImageResource(R.drawable.ic_map);
                isMapVisible = false;
            } else {
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
        isCommunityMode = false;
        FirebaseFirestore.getInstance().collection("official_maps").document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean moveFlag = documentSnapshot.getBoolean("isMoveEnabled");
                        canMove = (moveFlag != null) ? moveFlag : false;

                        String name = documentSnapshot.getString("mapName");
                        if (name != null) tvMapTitle.setText(name);

                        List<Object> pointsRaw = (List<Object>) documentSnapshot.get("points");
                        if (pointsRaw != null) {
                            gamePoints.clear();
                            for (Object p : pointsRaw) {
                                Map<String, Object> pointData = (Map<String, Object>) p;
                                double lat = ((Number) pointData.get("lat")).doubleValue();
                                double lng = ((Number) pointData.get("lng")).doubleValue();
                                gamePoints.add(new LatLng(lat, lng));
                            }
                            totalRounds = gamePoints.size();
                            loadRound(1);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show());
    }

    private void fetchCommunityMapData(String id) {
        isCommunityMode = true;
        canMove = false; // Community images are static

        FirebaseFirestore.getInstance().collection("community_maps").document(id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("mapName");
                        if (name != null) tvMapTitle.setText(name);
                        tvDifficulty.setText("Community");

                        // Parse data as seen in image_39d1f5.png
                        communityImageUrls = (List<String>) documentSnapshot.get("imageUrls");
                        communityLocations = (List<Map<String, Double>>) documentSnapshot.get("locations");

                        if (communityLocations != null && !communityLocations.isEmpty()) {
                            totalRounds = communityLocations.size();
                            loadRound(1);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading community map", Toast.LENGTH_SHORT).show());
    }

    private void loadRound(int roundNumber) {
        // 1. Get the target coordinates for this round
        LatLng roundLatLng;
        if (isCommunityMode) {
            // Parsing the structure from image_39d1f5.png
            Map<String, Double> loc = communityLocations.get(roundNumber - 1);
            roundLatLng = new LatLng(loc.get("lat"), loc.get("lng"));
        } else {
            roundLatLng = gamePoints.get(roundNumber - 1);
        }

        currentTargetLatLng = roundLatLng;

        // 2. Display logic (Hybrid Toggle)
        if (isCommunityMode) {
            // Hide StreetView, Show ImageView
            if (streetViewLayout != null) streetViewLayout.setVisibility(View.GONE);
            imgCommunityPhoto.setVisibility(View.VISIBLE);

            String imageUrl = communityImageUrls.get(roundNumber - 1);
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(imgCommunityPhoto);
        } else {
            // Show StreetView, Hide ImageView
            imgCommunityPhoto.setVisibility(View.GONE);
            if (streetViewLayout != null) streetViewLayout.setVisibility(View.VISIBLE);

            if (mStreetView != null) {
                mStreetView.setPosition(roundLatLng, 100);
                mStreetView.setUserNavigationEnabled(canMove);
            }
        }

        tvRound.setText("ROUND: " + roundNumber + " / " + totalRounds);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        mStreetView = panorama;
        ImageButton btnCompass = findViewById(R.id.btn_compass);
        mStreetView.setUserNavigationEnabled(canMove);
        tvScore.setText("SCORE: " + totalScore);

        if (!gamePoints.isEmpty()) {
            loadRound(currentRound);
        }

        // 1. Make the compass rotate as the player looks around
        mStreetView.setOnStreetViewPanoramaChangeListener(panoramaLocation -> {
            mStreetView.setOnStreetViewPanoramaCameraChangeListener(camera -> {
                // Rotate the icon. We use negative because if the camera moves right,
                // the "North" icon should rotate left to stay pointing North.
                btnCompass.setRotation(-camera.bearing);
            });
        });

// 2. Click to reset view to North
        btnCompass.setOnClickListener(v -> {
            if (mStreetView != null) {
                StreetViewPanoramaCamera north = new StreetViewPanoramaCamera.Builder()
                        .bearing(0) // 0 is North
                        .tilt(mStreetView.getPanoramaCamera().tilt)
                        .zoom(mStreetView.getPanoramaCamera().zoom)
                        .build();
                mStreetView.animateTo(north, 500); // Smooth 500ms rotation
            }
        });
    }

    private void showRoundResultDialog(int score, int distanceMeters) {
        // 1. Inflate our custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_round_result, null);

        // 2. Create the dialog with the transparent style
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(false) // Force them to click "Next"
                .create();

        // 3. SHOW the dialog first
        dialog.show();

        // 4. FORCE BIG SIZE (Match Parent)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        // 5. Link UI Elements
        TextView tvResScore = dialogView.findViewById(R.id.result_score_text);
        TextView tvResDist = dialogView.findViewById(R.id.result_distance_text);
        Button btnNext = dialogView.findViewById(R.id.btn_next_round);

        tvResScore.setText("+" + score);

        // Nicer distance formatting (show meters if close, km if far)
        if (distanceMeters < 1000) {
            tvResDist.setText("Distance: " + distanceMeters + "m");
        } else {
            tvResDist.setText(String.format("Distance: %.2f km", distanceMeters / 1000.0));
        }

        // 6. Initialize the Result Map
        SupportMapFragment resultMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.result_mini_map);

        if (resultMapFragment != null) {
            resultMapFragment.getMapAsync(googleMap -> {
                // Get positions
                LatLng userPos = userGuessMarker.getPosition();
                LatLng actualPos = gamePoints.get(currentRound - 1);

                // Red Pin (User)
                googleMap.addMarker(new MarkerOptions()
                        .position(userPos)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                // Yellow Pin (Real)
                googleMap.addMarker(new MarkerOptions()
                        .position(actualPos)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                // Draw the line (Classic White Line)
                googleMap.addPolyline(new PolylineOptions()
                        .add(userPos, actualPos)
                        .width(8)
                        .color(android.graphics.Color.WHITE));

                // Zoom camera to fit both pins
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(userPos);
                builder.include(actualPos);
                LatLngBounds bounds = builder.build();

                // Use 150dp padding so pins aren't touching the edge of the map
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
            });
        }

        // 7. Button Logic
        btnNext.setOnClickListener(v -> {
            // CLEANUP: Very important for SupportMapFragment in Dialogs
            if (resultMapFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(resultMapFragment).commit();
            }
            dialog.dismiss();
            prepareNextRound();
        });
    }

    private int calculateScore(double distanceInMeters) {
        if (distanceInMeters <= 50) {
            return 5000;
        }
        if (distanceInMeters >= 400000) {
            return 0;
        }

        // Linear calculation:
        double maxDist = 400000.0;
        double minDist = 50.0;

        double score = 5000 * (1 - (distanceInMeters - minDist) / (maxDist - minDist));
        return (int) Math.round(score);
    }

    private void showFinalScore() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_summary, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TransparentDialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        TextView tvFinalScore = dialogView.findViewById(R.id.final_score_text);
        Button btnFinish = dialogView.findViewById(R.id.btn_finish_game);

        tvFinalScore.setText(String.valueOf(totalScore));

        SupportMapFragment summaryMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.final_summary_map);

        if (summaryMapFragment != null) {
            summaryMapFragment.getMapAsync(googleMap -> {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                for (int i = 0; i < gamePoints.size(); i++) {
                    LatLng actual = gamePoints.get(i);
                    LatLng guess = (i < userGuesses.size()) ? userGuesses.get(i) : null;

                    if (guess != null) {
                        // Actual Location (Yellow)
                        googleMap.addMarker(new MarkerOptions()
                                .position(actual)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

                        // User Guess (Red)
                        googleMap.addMarker(new MarkerOptions()
                                .position(guess)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                        // Connect them with a thin gray line
                        googleMap.addPolyline(new PolylineOptions()
                                .add(actual, guess)
                                .width(4)
                                .color(Color.parseColor("#80FFFFFF"))); // Semi-transparent white

                        builder.include(actual);
                        builder.include(guess);
                    }
                }

                // Zoom to fit all 10 pins
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            });
        }

        btnFinish.setOnClickListener(v -> {
            // 1. Save the score before leaving
            String mapId = getIntent().getStringExtra("SELECTED_MAP_ID");
            if (mapId != null) {
                saveHighScore(mapId, totalScore);
            }

            // 2. Cleanup fragments
            if (summaryMapFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(summaryMapFragment).commit();
            }
            dialog.dismiss();
            finish();
        });
    }

    private void prepareNextRound() {
        if (currentRound < TOTAL_ROUNDS) {
            currentRound++;
            tvRound.setText("ROUND: " + currentRound + " / " + TOTAL_ROUNDS);

            // --- RESET UI STATE FOR NEW ROUND ---
            isMapVisible = false;
            guessMapContainer.setVisibility(View.GONE);

            // Find your FAB and reset the icon to the map icon
            FloatingActionButton btnOpenMap = findViewById(R.id.btn_open_guess_map);
            btnOpenMap.setImageResource(R.drawable.ic_map);

            loadRound(currentRound);

            // Clear the previous guess marker
            if (userGuessMarker != null) userGuessMarker.remove();
            userGuessMarker = null;
        } else {
            showFinalScore();
        }
    }

    private void resetGuessMap() {
        if (mGuessMap != null) {
            // 1. Remove any markers left over
            mGuessMap.clear();
            userGuessMarker = null;

            // 2. Teleport the camera back to the center of Israel (zoom 7.2 or 7.5)
            mGuessMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ISRAEL_CENTER, 7.2f));
        }
    }

    private void saveHighScore(String mapId, int finalScore) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        // Reference to the specific map score for this user
        // Path: users/{userId}/scores/{mapId}
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .collection("scores").document(mapId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentBest = documentSnapshot.getLong("highScore");

                        // Only update if the new score is better
                        if (currentBest != null && finalScore > currentBest) {
                            updateScoreInFirestore(userId, mapId, finalScore, true);
                        }
                    } else {
                        // No score yet, create the first record
                        updateScoreInFirestore(userId, mapId, finalScore, false);
                    }
                });
    }

    private void updateScoreInFirestore(String userId, String mapId, int score, boolean isNewBest) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Update the Map-Specific High Score
        Map<String, Object> mapData = new HashMap<>();
        mapData.put("highScore", score);
        mapData.put("lastPlayed", System.currentTimeMillis());

        db.collection("users").document(userId)
                .collection("scores").document(mapId)
                .set(mapData);

        // 2. Add the current game score to the GLOBAL Total
        // This looks for the "score" field in the main user document and adds to it
        db.collection("users").document(userId)
                .update("score", FieldValue.increment(score))
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE_UPDATE", "Global score updated by +" + score);
                });

        if (isNewBest) {
            Toast.makeText(this, "New Personal Best!", Toast.LENGTH_SHORT).show();
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