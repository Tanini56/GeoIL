package com.example.geoil_geoguessrisraellearner;

import android.content.Intent;
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

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapCreatorActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView imgPreview;
    private Button btnSelectImage, btnNextStep;
    private TextView tvSelectionCount;
    private View mapContainer, layoutPinning, layoutDetails;
    private EditText etMapName;
    private Spinner spinnerCategory;

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

        // Initialize Views
        imgPreview = findViewById(R.id.img_preview);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnNextStep = findViewById(R.id.btn_next_step);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        mapContainer = findViewById(R.id.map_container);
        layoutPinning = findViewById(R.id.layout_step_pinning);
        layoutDetails = findViewById(R.id.layout_step_details);
        etMapName = findViewById(R.id.et_map_name);
        spinnerCategory = findViewById(R.id.spinner_category);

        // Setup Category Spinner
        String[] categories = {"City", "Desert", "Mountains", "Beach"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_picker);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        showModeSelectionDialog();

        btnSelectImage.setOnClickListener(v -> startSelection());
        btnNextStep.setOnClickListener(v -> handleNextStep());
    }

    private void showModeSelectionDialog() {
        String[] options = {"1 Round (Camera)", "1 Round (Gallery)", "5 Round (Gallery)"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Map Mode")
                .setCancelable(false)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { roundMode = 1; startSelection(); }
                    else if (which == 1) { roundMode = 1; singleImageLauncher.launch("image/*"); }
                    else { roundMode = 5; multiImageLauncher.launch("image/*"); }
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
        imgPreview.setImageURI(selectedImageUris.get(0));
        tvSelectionCount.setText("Pinning Image 1 of " + selectedImageUris.size());
        btnSelectImage.setVisibility(View.GONE);
        btnNextStep.setVisibility(View.VISIBLE);
        mapContainer.setVisibility(View.VISIBLE);
    }

    private void handleNextStep() {
        // PHASE 1: PINNING LOGIC
        if (layoutPinning.getVisibility() == View.VISIBLE) {
            if (tempLatLng != null) {
                // Save the location the user just pinned
                selectedLocations.add(tempLatLng);
                currentRoundIndex++;

                if (currentRoundIndex < roundMode) {
                    // Prepare for the next round
                    imgPreview.setImageURI(selectedImageUris.get(currentRoundIndex));
                    tvSelectionCount.setText("Pinning Image " + (currentRoundIndex + 1) + " of " + roundMode);

                    // Reset marker for the next image
                    if (currentMarker != null) currentMarker.remove();
                    tempLatLng = null;
                    btnNextStep.setEnabled(false);
                } else {
                    // All rounds pinned! Switch to the Details screen
                    layoutPinning.setVisibility(View.GONE);
                    layoutDetails.setVisibility(View.VISIBLE);
                    btnNextStep.setText("Publish Map");
                }
            }
        }
        // PHASE 2: DETAILS PAGE LOGIC
        else {
            String userEnteredName = etMapName.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString().toLowerCase();

            if (!userEnteredName.isEmpty()) {
                // 1. Automatically format the map name with round count
                String finalMapName = userEnteredName + " (" + roundMode + " Rounds)";

                // 2. The icon name based on your files (e.g., "desert_icon")
                String iconResourceName = category + "_icon";

                // 3. Trigger the Firebase Upload
                Toast.makeText(this, "Publishing " + finalMapName, Toast.LENGTH_SHORT).show();
                // uploadToFirebase(finalMapName, category, iconResourceName);
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
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
}