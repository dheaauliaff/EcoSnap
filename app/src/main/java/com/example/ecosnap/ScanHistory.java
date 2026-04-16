package com.example.ecosnap;

import com.google.gson.annotations.SerializedName;

public class ScanHistory {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("rw_id")
    private String rwId;

    @SerializedName("wilayah")
    private String wilayah;

    @SerializedName("jenis_sampah")
    private String jenisSampah;

    @SerializedName("kategori")
    private String kategori;

    @SerializedName("foto_url")
    private String fotoUrl;

    @SerializedName("akurasi")
    private float akurasi;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRwId() { return rwId; }
    public String getWilayah() { return wilayah; }
    public String getJenisSampah() { return jenisSampah; }
    public String getKategori() { return kategori; }
    public String getFotoUrl() { return fotoUrl; }
    public float getAkurasi() { return akurasi; }
    public String getCreatedAt() { return createdAt; }
}