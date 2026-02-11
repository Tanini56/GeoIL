package com.example.geoil_geoguessrisraellearner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewSource;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

public class HomeFragment extends Fragment implements OnMapReadyCallback, OnStreetViewPanoramaReadyCallback {

    private GoogleMap mGoogleMap;
    private StreetViewPanorama mStreetView;
    private FrameLayout streetViewContainer;
    private Button btnBackToMap, btnToggleBlueLines;
    private TileOverlay blueLinesOverlay;
    private boolean isBlueLinesVisible = true;

    private final LatLng ISRAEL_CENTER = new LatLng(31.0461, 34.8516);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        streetViewContainer = view.findViewById(R.id.streetview_container);
        btnBackToMap = view.findViewById(R.id.btn_back_to_map);
        btnToggleBlueLines = view.findViewById(R.id.btn_toggle_blue_lines);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        SupportStreetViewPanoramaFragment streetViewFragment = (SupportStreetViewPanoramaFragment) getChildFragmentManager().findFragmentById(R.id.streetview_fragment);
        if (streetViewFragment != null) streetViewFragment.getStreetViewPanoramaAsync(this);

        btnBackToMap.setOnClickListener(v -> {
            streetViewContainer.setVisibility(View.GONE);
            btnBackToMap.setVisibility(View.GONE);
            btnToggleBlueLines.setVisibility(View.VISIBLE);
        });

        // Toggle Blue Lines logic
        btnToggleBlueLines.setOnClickListener(v -> {
            isBlueLinesVisible = !isBlueLinesVisible;
            if (blueLinesOverlay != null) {
                blueLinesOverlay.setVisible(isBlueLinesVisible);
                btnToggleBlueLines.setText(isBlueLinesVisible ? "Hide Blue Lines" : "Show Blue Lines");
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ISRAEL_CENTER, 7.5f));

        // The Blue Lines "Magic"
        TileProvider provider = new UrlTileProvider(256, 256) {
            @Override
            public java.net.URL getTileUrl(int x, int y, int zoom) {
                // Updated URL to ensure high-quality tiles
                String s = String.format("https://mts1.google.com/vt?hl=en&lyrs=svv&x=%d&y=%d&z=%d", x, y, zoom);
                try {
                    return new java.net.URL(s);
                } catch (java.net.MalformedURLException e) {
                    return null;
                }
            }
        };

        // Add overlay with high Z-Index
        blueLinesOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(provider)
                .transparency(0.1f) // 0.1 is very visible, 0.9 is almost invisible
                .zIndex(100)); // Keeps lines above everything else

        // ... rest of your map click logic
    }

    @Override
    public void onStreetViewPanoramaReady(@NonNull StreetViewPanorama panorama) {
        mStreetView = panorama;
        mStreetView.setOnStreetViewPanoramaChangeListener(location -> {
            if (location == null) {
                Toast.makeText(getContext(), "No Street View here!", Toast.LENGTH_SHORT).show();
                streetViewContainer.setVisibility(View.GONE);
                btnBackToMap.setVisibility(View.GONE);
                btnToggleBlueLines.setVisibility(View.VISIBLE);
            }
        });
    }
}