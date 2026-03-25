package com.example.geoil_geoguessrisraellearner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.app.ProgressDialog;
import java.util.ArrayList; // You'll likely need this for the 'urls' list too
import java.util.List;
import java.util.HashMap;
import java.util.Map;



import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
public class MapCreatorActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView imgPreview;
    private boolean isCameraSource = false; // NEW: tracks if we are using the real-time camera
    private com.google.firebase.storage.FirebaseStorage storage;
    private com.google.firebase.firestore.FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private Button btnSelectImage, btnNextStep;
    private TextView tvSelectionCount;
    private View mapContainer, layoutPinning, layoutDetails;
    private EditText etMapName;
    private Spinner spinnerCategory;
    private String selectedCategory = "city"; // Default
    private ImageView lastSelectedIcon;

    private GoogleMap mMap;
    private Marker currentMarker;
    private Uri cameraImageUri;

    private int roundMode = 1;
    private int currentRoundIndex = 0;
    private LatLng tempLatLng = null;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private List<LatLng> selectedLocations = new ArrayList<>();

    // LAUNCHERS
    private final ActivityResultLauncher<String> singleImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if(uri != null){ selectedImageUris.add(uri); onImagesPicked(); } });

    private final ActivityResultLauncher<String> multiImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(), uris -> { if(uris != null && uris.size()==5){ selectedImageUris.addAll(uris); onImagesPicked(); } });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> { if(result) onImagesPicked(); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_creator);

        // 1. Initialize Firebase & Location
        storage = com.google.firebase.storage.FirebaseStorage.getInstance("gs://zivbase-bbcb4.firebasestorage.app");
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Initialize Views
        imgPreview = findViewById(R.id.img_preview);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnNextStep = findViewById(R.id.btn_next_step);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        mapContainer = findViewById(R.id.map_container);
        layoutPinning = findViewById(R.id.layout_step_pinning);
        layoutDetails = findViewById(R.id.layout_step_details);
        etMapName = findViewById(R.id.et_map_name);

        // 3. Setup the New Icon Selection UI
        lastSelectedIcon = findViewById(R.id.icon_city);
        if (lastSelectedIcon != null) {
            lastSelectedIcon.setBackgroundResource(R.drawable.selected_border_blue);
        }

        setupIconClick(R.id.icon_beach, "beach");
        setupIconClick(R.id.icon_city, "city");
        setupIconClick(R.id.icon_desert, "desert");
        setupIconClick(R.id.icon_farm, "farm");
        setupIconClick(R.id.icon_grassland, "grassland");
        setupIconClick(R.id.icon_lake, "lake");
        setupIconClick(R.id.icon_mountain, "mountain");

        // 4. Map Setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_picker);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // 5. Listeners
        btnSelectImage.setOnClickListener(v -> startSelection());
        btnNextStep.setOnClickListener(v -> handleNextStep());

        // Show the dialog last
        showModeSelectionDialog();
    }

    private void showModeSelectionDialog() {
        String[] options = {"1 Round (Camera)", "1 Round (Gallery)", "5 Round (Gallery)"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Map Mode")
                .setCancelable(false)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        roundMode = 1;
                        isCameraSource = true; // Mark as camera
                        startSelection();
                    }
                    else if (which == 1) {
                        roundMode = 1;
                        isCameraSource = false; // Mark as gallery
                        singleImageLauncher.launch("image/*");
                    }
                    else {
                        roundMode = 5;
                        isCameraSource = false; // Mark as gallery
                        multiImageLauncher.launch("image/*");
                    }
                }).show();
    }

    private void startSelection() {
        if (roundMode == 1 && cameraImageUri == null) {
            File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_map_img.jpg");
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            selectedImageUris.clear();
            selectedImageUris.add(cameraImageUri);
            cameraLauncher.launch(cameraImageUri);
        }
    }

    private void onImagesPicked() {
        if (selectedImageUris.isEmpty()) return;

        imgPreview.setImageURI(selectedImageUris.get(0));
        tvSelectionCount.setText("Pinning Image 1 of " + selectedImageUris.size());
        btnSelectImage.setVisibility(View.GONE);
        btnNextStep.setVisibility(View.VISIBLE);
        mapContainer.setVisibility(View.VISIBLE);

        // UPDATED LOGIC: Only auto-pin if it's 1-round AND it's a fresh Camera photo
        if (roundMode == 1 && isCameraSource) {
            autoPinCurrentLocation();
        } else {
            // If from Gallery, just show a hint to the user
            Toast.makeText(this, "Long-press the map to set location manually", Toast.LENGTH_LONG).show();

            // Ensure button is disabled until they actually drop a pin
            btnNextStep.setEnabled(false);
        }
    }

    private void autoPinCurrentLocation() {
        // 1. Check if the app has permission to use GPS
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        // 2. Try to get the last known location
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && mMap != null) {
                // SUCCESS: Location found
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (currentMarker != null) currentMarker.remove();
                currentMarker = mMap.addMarker(new MarkerOptions()
                        .position(currentLatLng)
                        .title("Captured Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));

                tempLatLng = currentLatLng;
                btnNextStep.setEnabled(true);
                Toast.makeText(this, "Location pinned automatically!", Toast.LENGTH_SHORT).show();
            } else {
                // FAIL: Location is null (GPS might be off or no signal)
                Toast.makeText(this, "Please enable location for automatic pinning.", Toast.LENGTH_LONG).show();
                // The backup (manual pinning) is still active because setOnMapLongClickListener is always running
            }
        });
    }

    private void handleNextStep() {
        // PHASE 1: PINNING LOGIC (The Map Part)
        if (layoutPinning.getVisibility() == View.VISIBLE) {
            if (tempLatLng != null) {
                // Save the location the user just pinned
                selectedLocations.add(tempLatLng);
                currentRoundIndex++;

                if (currentRoundIndex < roundMode) {
                    // Prepare for the next round (5-round mode)
                    imgPreview.setImageURI(selectedImageUris.get(currentRoundIndex));
                    tvSelectionCount.setText("Pinning Image " + (currentRoundIndex + 1) + " of " + roundMode);

                    // Reset marker and local variable for the next image
                    if (currentMarker != null) currentMarker.remove();
                    tempLatLng = null;
                    btnNextStep.setEnabled(false);
                } else {
                    // All rounds pinned! Transition to Phase 2 (Details)
                    layoutPinning.setVisibility(View.GONE);
                    layoutDetails.setVisibility(View.VISIBLE);
                    btnNextStep.setText("Publish Map");

                    // Final check: if 5 rounds were selected, ensure we have 5 locations
                    if (selectedLocations.size() != roundMode) {
                        Toast.makeText(this, "Error: Some pins are missing!", Toast.LENGTH_SHORT).show();
                        // Optional: reset currentRoundIndex to fix it, but usually the logic prevents this
                    }
                }
            } else {
                Toast.makeText(this, "Please pin the location on the map first!", Toast.LENGTH_SHORT).show();
            }
        }

        // PHASE 2: DETAILS PAGE LOGIC (The Publishing Part)
        else {
            String userEnteredName = etMapName.getText().toString().trim();

            // We no longer use spinnerCategory.getSelectedItem()
            // Instead, we use the 'selectedCategory' variable set by the Icon Click listeners
            String category = selectedCategory;

            if (!userEnteredName.isEmpty()) {
                // 1. Format the map name
                String finalMapName = userEnteredName + " (" + roundMode + " Round/s)";

                // 2. The icon name based on your drawable files (e.g., "desert_icon")
                String iconResourceName = category + "_icon";

                // 3. Trigger the actual Firebase Upload
                Toast.makeText(this, "Publishing " + finalMapName + "...", Toast.LENGTH_SHORT).show();
                uploadToFirebase(finalMapName, category, iconResourceName);

            } else {
                Toast.makeText(this, "Please enter a name for your map", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(31.0461, 34.8516), 7));
        mMap.setOnMapLongClickListener(latLng -> {
            if (currentMarker != null) currentMarker.remove();
            currentMarker = mMap.addMarker(new MarkerOptions().position(latLng));
            tempLatLng = latLng;
            btnNextStep.setEnabled(true);
        });
    }
    private void uploadToFirebase(String mapName, String category, String iconName) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Map...");
        progressDialog.show();

        List<String> uploadedUrls = new ArrayList<>();
        uploadImagesRecursively(0, uploadedUrls, progressDialog, mapName, category, iconName);
    }

    private void uploadImagesRecursively(int index, List<String> urls, ProgressDialog pd, String name, String cat, String icon) {
        if (index >= selectedImageUris.size()) {
            // ALL IMAGES UPLOADED -> Save to Firestore
            saveMapDataToFirestore(name, cat, icon, urls, pd);
            return;
        }

        pd.setMessage("Uploading image " + (index + 1) + " of " + selectedImageUris.size());

        String fileName = "maps/" + System.currentTimeMillis() + "_" + index + ".jpg";
        com.google.firebase.storage.StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(selectedImageUris.get(index))
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    urls.add(uri.toString());
                    uploadImagesRecursively(index + 1, urls, pd, name, cat, icon);
                }))
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMapDataToFirestore(String name, String cat, String icon, List<String> urls, ProgressDialog pd) {
        // 1. IMPROVED AUTHOR LOGIC
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String authorName = "Anonymous";

        if (user != null) {
            // Check if the user has a Display Name set in their profile
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                authorName = user.getDisplayName();
            } else {
                // Fallback: Use the part of the email before the @
                String email = user.getEmail();
                if (email != null && email.contains("@")) {
                    authorName = email.split("@")[0];
                } else if (email != null) {
                    authorName = email;
                }
            }
        }

        Map<String, Object> mapData = new HashMap<>();
        mapData.put("mapName", name);
        mapData.put("category", cat);
        mapData.put("iconName", icon);
        mapData.put("author", authorName); // Now saves the Username/Prefix
        mapData.put("imageUrls", urls);
        mapData.put("timestamp", System.currentTimeMillis());

        // Convert LatLng list to a format Firestore understands
        List<Map<String, Double>> locationData = new ArrayList<>();
        for (LatLng latLng : selectedLocations) {
            Map<String, Double> point = new HashMap<>();
            point.put("lat", latLng.latitude);
            point.put("lng", latLng.longitude);
            locationData.add(point);
        }
        mapData.put("locations", locationData);

        db.collection("community_maps")
                .add(mapData)
                .addOnSuccessListener(documentReference -> {
                    pd.dismiss();
                    Toast.makeText(this, "Map Published Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupIconClick(int viewId, String categoryName) {
        ImageView iconView = findViewById(viewId);
        if (iconView == null) return; // Safety check

        iconView.setOnClickListener(v -> {
            // Step A: Remove border from the previous selection
            if (lastSelectedIcon != null) {
                lastSelectedIcon.setBackgroundResource(0);
            }

            // Step B: Add border to the new selection
            iconView.setBackgroundResource(R.drawable.selected_border_blue);

            // Step C: Update our global variables
            lastSelectedIcon = iconView;
            selectedCategory = categoryName;

            Toast.makeText(this, "Theme: " + categoryName, Toast.LENGTH_SHORT).show();
        });
    }

}
