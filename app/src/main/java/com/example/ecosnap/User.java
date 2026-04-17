package com.example.ecosnap;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private String id;

    @SerializedName("firebase_uid")
    private String firebaseUid;

    @SerializedName("nama")
    private String nama;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("wilayah")
    private String wilayah;

    @SerializedName("rw_id")
    private String rwId;

    // Getters
    public String getId() { return id; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getNama() { return nama; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getWilayah() { return wilayah; }
    public String getRwId() { return rwId; }
}