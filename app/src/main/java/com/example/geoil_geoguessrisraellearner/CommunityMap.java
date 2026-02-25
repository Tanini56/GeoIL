package com.example.geoil_geoguessrisraellearner;

public class CommunityMap {
    private String mapName;
    private String author;
    private String category;
    private long timestamp; // Add this

    public CommunityMap() {}

    public CommunityMap(String mapName, String author, String category, long timestamp) {
        this.mapName = mapName;
        this.author = author;
        this.category = category;
        this.timestamp = timestamp; // Initialize it
    }

    public String getMapName() { return mapName; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }

    // Add this getter so the sorting logic works!
    public long getTimestamp() { return timestamp; }
}