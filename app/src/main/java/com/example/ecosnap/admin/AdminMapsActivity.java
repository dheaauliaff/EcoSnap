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

import java.util.List;

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

        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_maps_admin);

        mAuth = FirebaseAuth.getInstance();
        mapView = findViewById(R.id.mapView);
        bottomNav = findViewById(R.id.bottomNav);

        setupMap();
        loadDataAdmin();

        // 🔥 Bottom Navigation (sudah smooth)
        bottomNav.setSelectedItemId(R.id.nav_admin_maps);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;

            } else if (id == R.id.nav_admin_rekap) {
                startActivity(new Intent(this, RekapAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;

            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;

            } else if (id == R.id.nav_admin_maps) {
                return true;

            } else if (id == R.id.nav_admin_profil) {
                startActivity(new Intent(this, ProfilAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            }

            return false;
        });
    }

    // Setup awal map
    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Default posisi (akan diganti nanti otomatis)
        GeoPoint startPoint = new GeoPoint(-6.9175, 107.6191);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(startPoint);
    }

    // Ambil data admin
    private void loadDataAdmin() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.getUserByFirebaseUid("eq." + uid)
                .enqueue(new Callback<List<User>>() {

                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> response) {

                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                            User admin = response.body().get(0);
                            rwId = admin.getRwId() != null ? admin.getRwId() : "";

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

    // Ambil data scan dari Supabase
    private void loadDataSebaran() {
        if (rwId.isEmpty()) return;

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.getScanByRw("eq." + rwId)
                .enqueue(new Callback<List<ScanHistory>>() {

                    @Override
                    public void onResponse(Call<List<ScanHistory>> call,
                                           Response<List<ScanHistory>> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            tampilkanSebaranGPS(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                        Toast.makeText(AdminMapsActivity.this,
                                "Gagal load data peta", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 🔥 INI BAGIAN UTAMA — TAMPILKAN MARKER REAL GPS
    private void tampilkanSebaranGPS(List<ScanHistory> data) {

        mapView.getOverlays().clear();

        if (data.isEmpty()) return;

        double totalLat = 0;
        double totalLng = 0;
        int count = 0;

        for (ScanHistory s : data) {

            if (s.getLatitude() != null && s.getLongitude() != null) {

                double lat = s.getLatitude();
                double lng = s.getLongitude();

                GeoPoint point = new GeoPoint(lat, lng);

                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Info marker
                marker.setTitle(s.getWilayah() != null ? s.getWilayah() : "Lokasi");

                marker.setSnippet(
                        (s.getJenisSampah() != null ? s.getJenisSampah() : "Sampah")
                                + " | "
                                + (s.getKategori() != null ? s.getKategori() : "-")
                );

                mapView.getOverlays().add(marker);

                totalLat += lat;
                totalLng += lng;
                count++;
            }
        }

        // Auto center map
        if (count > 0) {
            double avgLat = totalLat / count;
            double avgLng = totalLng / count;

            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(new GeoPoint(avgLat, avgLng));
        }

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