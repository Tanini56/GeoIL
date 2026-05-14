package com.example.geoil_geoguessrisraellearner;

public class UserScore {
    public String username;
    public long score;

    public UserScore() {} // Required for Firestore

    public UserScore(String username, long score) {
        this.username = username;
        this.score = score;
    }
}