package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMapsActivity extends AppCompatActivity {

    MapView mapView;
    BottomNavigationView bottomNav;
    FirebaseAuth mAuth;
    String rwId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Wajib sebelum setContentView
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_maps_admin);

        mAuth = FirebaseAuth.getInstance();
        mapView = findViewById(R.id.mapView);
        bottomNav = findViewById(R.id.bottomNav);

        // Setup peta
        setupMap();

        // Load data admin untuk dapat rwId
        loadDataAdmin();

        // Setup bottom nav admin
        bottomNav.setSelectedItemId(R.id.nav_admin_maps);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, DashboardAdminActivity.class));
                return true;
            } else if (id == R.id.nav_admin_rekap) {
                startActivity(new Intent(this, RekapAdminActivity.class));
                return true;
            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                return true;
            } else if (id == R.id.nav_admin_maps) {
                return true;
            } else if (id == R.id.nav_admin_profil) {
                startActivity(new Intent(this, ProfilAdminActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Ganti koordinat sesuai lokasi desa kamu
        GeoPoint startPoint = new GeoPoint(-6.9175, 107.6191);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(startPoint);
    }

    private void loadDataAdmin() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + uid);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    User admin = response.body().get(0);
                    rwId = admin.getRwId() != null ? admin.getRwId() : "";

                    // Langsung load sebaran setelah dapat rwId
                    loadDataSebaran();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(AdminMapsActivity.this,
                        "Gagal load data admin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDataSebaran() {
        if (rwId.isEmpty()) return;

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<ScanHistory>> call = apiService.getScanByRw("eq." + rwId);

        call.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call,
                                   Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tampilkanSebaranPerRT(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(AdminMapsActivity.this,
                        "Gagal load data peta", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void tampilkanSebaranPerRT(List<ScanHistory> data) {
        // Hitung jumlah scan per RT
        Map<String, Integer> countPerRT = new HashMap<>();
        for (ScanHistory s : data) {
            if (s.getWilayah() != null) {
                countPerRT.put(s.getWilayah(),
                        countPerRT.getOrDefault(s.getWilayah(), 0) + 1);
            }
        }

        // Koordinat tiap RT — sesuaikan dengan lokasi desa asli!
        Map<String, GeoPoint> koordinatRT = new HashMap<>();
        koordinatRT.put("RT 01", new GeoPoint(-6.9150, 107.6180));
        koordinatRT.put("RT 02", new GeoPoint(-6.9175, 107.6200));
        koordinatRT.put("RT 03", new GeoPoint(-6.9200, 107.6175));
        koordinatRT.put("RT 04", new GeoPoint(-6.9160, 107.6220));
        koordinatRT.put("RT 05", new GeoPoint(-6.9190, 107.6150));

        // Cari nilai max untuk skala warna
        int maxScan = 1;
        for (int val : countPerRT.values()) {
            if (val > maxScan) maxScan = val;
        }

        // Tampilkan zona per RT
        for (Map.Entry<String, GeoPoint> entry : koordinatRT.entrySet()) {
            String rtNama = entry.getKey();
            GeoPoint titik = entry.getValue();
            int jumlah = countPerRT.getOrDefault(rtNama, 0);
            int warna = getWarnaZona(jumlah, maxScan);
            buatZona(titik, rtNama, jumlah, warna);
        }
    }

    private int getWarnaZona(int jumlah, int max) {
        if (jumlah == 0) return Color.argb(80, 158, 158, 158);
        float ratio = (float) jumlah / max;
        if (ratio < 0.33f) return Color.argb(120, 76, 175, 80);
        else if (ratio < 0.66f) return Color.argb(120, 255, 193, 7);
        else return Color.argb(120, 244, 67, 54);
    }

    private void buatZona(GeoPoint center, String rtNama, int jumlah, int warna) {
        double radius = 0.0009;
        List<GeoPoint> points = new ArrayList<>();
        int segments = 32;

        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double lat = center.getLatitude() + radius * Math.cos(angle);
            double lng = center.getLongitude() + radius * Math.sin(angle);
            points.add(new GeoPoint(lat, lng));
        }

        Polygon polygon = new Polygon();
        polygon.setPoints(points);
        polygon.getFillPaint().setColor(warna);
        polygon.getOutlinePaint().setColor(Color.argb(180, 46, 125, 50));
        polygon.getOutlinePaint().setStrokeWidth(3f);
        mapView.getOverlays().add(polygon);

        Marker marker = new Marker(mapView);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setTitle(rtNama);
        marker.setSnippet(jumlah + " scan sampah");
        marker.setIcon(null);
        mapView.getOverlays().add(marker);

        mapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}