package com.example.geoil_geoguessrisraellearner;

// No UI imports needed here! Just the pure data class.

public class OfficialMap {
    private String mapName;
    private String mapId;

    // Required for Firebase to work
    public OfficialMap() {}

    public OfficialMap(String mapName, String mapId) {
        this.mapName = mapName;
        this.mapId = mapId;
    }

    public String getMapName() { return mapName; }
    public String getMapId() { return mapId; }
}