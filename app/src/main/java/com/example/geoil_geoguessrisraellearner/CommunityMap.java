package com.example.geoil_geoguessrisraellearner;

import java.util.List;
import java.util.Map;

public class CommunityMap {
    private String mapName;
    private String category;
    private String iconName;
    private String author;
    private long timestamp; // Added this back
    private List<String> imageUrls;
    private List<Map<String, Double>> locations;

    // Required empty constructor
    public CommunityMap() {}

    // Getters
    public String getMapName() { return mapName; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getIconName() { return iconName; }
    public long getTimestamp() { return timestamp; } // Added this getter
    public List<String> getImageUrls() { return imageUrls; }
    public List<Map<String, Double>> getLocations() { return locations; } // Added this getter
}