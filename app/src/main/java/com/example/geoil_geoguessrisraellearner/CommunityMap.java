package com.example.geoil_geoguessrisraellearner;

import com.google.firebase.firestore.DocumentId; // ADD THIS IMPORT
import java.util.List;
import java.util.Map;

public class CommunityMap {
    @DocumentId // This tells Firestore to automatically populate this string with the Document ID!
    private String mapId;

    private String mapName;
    private String category;
    private String iconName;
    private String author;
    private List<String> imageUrls;
    private List<Map<String, Double>> locations;

    public CommunityMap() {}

    // ADD THIS GETTER
    public String getMapId() { return mapId; }

    public String getMapName() { return mapName; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getIconName() { return iconName; }
    public List<String> getImageUrls() { return imageUrls; }
    public List<Map<String, Double>> getLocations() { return locations; }
}