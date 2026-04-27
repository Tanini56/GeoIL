package com.example.geoil_geoguessrisraellearner;

// No UI imports needed here! Just the pure data class.

public class OfficialMap {
    private String mapName;
    private String mapId;
    private boolean isMoveEnabled; // Add this

    public OfficialMap() {}

    public OfficialMap(String mapName, String mapId, boolean isMoveEnabled) {
        this.mapName = mapName;
        this.mapId = mapId;
        this.isMoveEnabled = isMoveEnabled;
    }

    public String getMapName() { return mapName; }
    public String getMapId() { return mapId; }
    public boolean isMoveEnabled() { return isMoveEnabled; } // Add getter
}