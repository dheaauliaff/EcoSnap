package com.example.ecosnap;

import com.google.gson.annotations.SerializedName;

public class ScanHistory {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("rw_id")
    private String rwId;

    @SerializedName("rt_id")
    private String rtId;

    @SerializedName("wilayah")
    private String wilayah;

    @SerializedName("jenis_sampah")
    private String jenisSampah;

    @SerializedName("kategori")
    private String kategori;

    @SerializedName("foto_url")
    private String fotoUrl;

    @SerializedName("akurasi")
    private Float akurasi;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("created_at")
    private String createdAt;

    // 🔥 TAMBAHAN GPS
    @SerializedName("latitud")
    private Double latitud;

    @SerializedName("longitud")
    private Double longitud;

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
<<<<<<< Updated upstream

    // 🔥 GETTER GPS
    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
=======
}
>>>>>>> Stashed changes
