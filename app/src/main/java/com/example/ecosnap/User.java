package com.example.ecosnap;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("firebase_uid")
    private String firebaseUid;

    @SerializedName("nama")
    private String nama;

    // Ganti email jadi nomor_hp sesuai kolom di Supabase
    @SerializedName("nomor_hp")
    private String nomorHp;

    @SerializedName("role")
    private String role;

    @SerializedName("wilayah")
    private String wilayah;

    @SerializedName("rw_id")
    private String rwId;

    @SerializedName("rt_id")
    private String rtId;

    public String getFirebaseUid() { return firebaseUid; }
    public String getNama() { return nama; }
    public String getNomorHp() { return nomorHp; }
    public String getRole() { return role; }
    public String getWilayah() { return wilayah; }
    public String getRwId() { return rwId; }
    public String getRtId() { return rtId; }
}