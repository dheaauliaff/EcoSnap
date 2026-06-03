package com.example.ecosnap;

import com.google.gson.annotations.SerializedName;

public class ScanHistory {

    @SerializedName("id")
    private String id;

    @SerializedName("firebase_id")
    private String userId;

    @SerializedName("rw_id")
    private String rwId;

    @SerializedName("rt_id")
    private String rtId;

    @SerializedName("wilayah")
    private String wilayah;

    @SerializedName("nama_sampah")
    private String jenisSampah;

    @SerializedName("kategori")
    private String kategori;

    @SerializedName("image_url")
    private String fotoUrl;

    @SerializedName("confidence")
    private Float akurasi;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("alamat")
    private String alamat;

    // =====================
    // GETTER
    // =====================

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRwId() { return rwId; }
    public String getRtId() { return rtId; }
    public String getWilayah() { return wilayah; }
    public String getJenisSampah() { return jenisSampah; }
    public String getKategori() { return kategori; }
    public String getFotoUrl() { return fotoUrl; }
    public Float getAkurasi() { return akurasi; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCreatedAt() { return createdAt; }

    public String getAlamat() { return alamat; }
}
