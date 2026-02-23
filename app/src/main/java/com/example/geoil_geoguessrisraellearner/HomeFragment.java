package com.example.geoil_geoguessrisraellearner;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.viewpager2.widget.ViewPager2;

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
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

public class HomeFragment extends Fragment implements OnMapReadyCallback, OnStreetViewPanoramaReadyCallback {

    private GoogleMap mGoogleMap;
    private StreetViewPanorama mStreetView;
    private FrameLayout mapWrapper, streetViewContainer;
    private Button btnBackToMap, btnToggleBlueLines;
    private TileOverlay blueLinesOverlay;
    private boolean isBlueLinesVisible = true;

    private final LatLng ISRAEL_CENTER = new LatLng(31.47, 34.9516);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mapWrapper = view.findViewById(R.id.map_wrapper);
        streetViewContainer = view.findViewById(R.id.streetview_container);
        btnBackToMap = view.findViewById(R.id.btn_back_to_map);
        btnToggleBlueLines = view.findViewById(R.id.btn_toggle_blue_lines);

        // Initialize fragments
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        SupportStreetViewPanoramaFragment streetViewFragment = (SupportStreetViewPanoramaFragment) getChildFragmentManager().findFragmentById(R.id.streetview_fragment);
        if (streetViewFragment != null) streetViewFragment.getStreetViewPanoramaAsync(this);

        // UI Listeners
        btnBackToMap.setOnClickListener(v -> {
            streetViewContainer.setVisibility(View.GONE);
            mapWrapper.setVisibility(View.VISIBLE);
        });

        btnToggleBlueLines.setOnClickListener(v -> {
            if (blueLinesOverlay != null) {
                isBlueLinesVisible = !isBlueLinesVisible;
                blueLinesOverlay.setVisible(isBlueLinesVisible);
                updateToggleButtonUI();
            }
        });

        updateToggleButtonUI();
        return view;
    }

    private void updateToggleButtonUI() {
        if (isBlueLinesVisible) {
            btnToggleBlueLines.setText("Hide StreetView");
            btnToggleBlueLines.setAlpha(1.0f);
        } else {
            btnToggleBlueLines.setText("Show StreetView");
            btnToggleBlueLines.setAlpha(0.7f);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ISRAEL_CENTER, 7.5f));

        // Create the StreetView Coverage Layer (Blue Lines)
        UrlTileProvider provider = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                // Use mt0 or mt1, and ensure the parameters are exactly as Google expects
                String s = "https://mt0.google.com/vt/lyrs=svv&x=" + x + "&y=" + y + "&z=" + zoom;
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        };

        blueLinesOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(provider)
                .zIndex(100)
                .visible(isBlueLinesVisible));

        mGoogleMap.setOnMapClickListener(latLng -> {
            if (mStreetView != null) {
                // Search for panorama within 250m radius for better accuracy
                mStreetView.setPosition(latLng, 250, StreetViewSource.DEFAULT);
                mapWrapper.setVisibility(View.GONE);
                streetViewContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onStreetViewPanoramaReady(@NonNull StreetViewPanorama panorama) {
        mStreetView = panorama;

        mStreetView.setOnStreetViewPanoramaChangeListener(location -> {
            if (location == null && streetViewContainer.getVisibility() == View.VISIBLE) {
                Toast.makeText(getContext(), "No Street View coverage here!", Toast.LENGTH_SHORT).show();
                streetViewContainer.setVisibility(View.GONE);
                mapWrapper.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setSwipeEnabled(false); // Prevents ViewPager swiping while interacting with map
    }

    @Override
    public void onPause() {
        super.onPause();
        setSwipeEnabled(true);
    }

    private void setSwipeEnabled(boolean enabled) {
        if (getActivity() != null) {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setUserInputEnabled(enabled);
            }
        }
    }
}
