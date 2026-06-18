package com.example.ecosnap.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.WilayahUtils;
import com.example.ecosnap.model.User;
import com.example.ecosnap.admin.DashboardAdminActivity;
import com.example.ecosnap.admin.RekapAdminActivity;
import com.example.ecosnap.admin.AdminRankingActivity;
import com.example.ecosnap.admin.ProfilAdminActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.core.widget.NestedScrollView;

public class MapsActivity extends AppCompatActivity {

    TextView tvDetailContent;
    MapView mapView;
    LinearLayout detailMarkerCard;
    NestedScrollView mapsRoot;
    BottomNavigationView bottomNav;
    FirebaseAuth mAuth;
    String rwId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Gunakan layout maps admin karena kelihatannya user maps activity diarahkan ke sana
        //setContentView(R.layout.activity_maps_admin);
        setContentView(R.layout.activity_maps);

        mAuth = FirebaseAuth.getInstance();
        mapView = findViewById(R.id.osmMapView);
        tvDetailContent = findViewById(R.id.tvDetailContent);
        detailMarkerCard = findViewById(R.id.detailMarkerCard);
        bottomNav = findViewById(R.id.bottomNav);
        View btnCloseDetail = findViewById(R.id.btnCloseDetail);
        if (btnCloseDetail != null) {
            btnCloseDetail.setOnClickListener(v -> hideMarkerDetail());
        }

        if (mapView == null) {
            Toast.makeText(this, "Error: MapView not found in layout", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupMap();
        loadDataUser();

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_admin_maps);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_admin_dashboard) {
                    startActivity(new Intent(this, DashboardAdminActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_admin_rekap) {
                    startActivity(new Intent(this, RekapAdminActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_admin_ranking) {
                    startActivity(new Intent(this, AdminRankingActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_admin_maps) {
                    return true;
                } else if (id == R.id.nav_admin_profil) {
                    startActivity(new Intent(this, ProfilAdminActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        GeoPoint startPoint = new GeoPoint(-6.9175, 107.6191);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(startPoint);
    }

    private void loadDataUser() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getUserByFirebaseUid("eq." + uid).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    rwId = user.getRwId() != null ? user.getRwId() : "";
                    loadDataSebaran();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(MapsActivity.this, "Gagal load data user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDataSebaran() {
        if (rwId.isEmpty()) return;
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> sameRw = new ArrayList<>();
                    for (ScanHistory s : response.body()) {
                        if (WilayahUtils.isMatchingRw(s.getRwId(), rwId)) {
                            sameRw.add(s);
                        }
                    }
                    tampilkanMarkers(sameRw);
                }
            }
            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(MapsActivity.this, "Gagal load peta", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void tampilkanMarkers(List<ScanHistory> data) {
        mapView.getOverlays().clear();
        if (data.isEmpty()) {
            Toast.makeText(this, "Belum ada scan di wilayah ini", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalLat = 0, totalLng = 0;
        int count = 0;

        for (ScanHistory s : data) {
            if (s.getLatitude() != null && s.getLongitude() != null
                    && s.getLatitude() != 0 && s.getLongitude() != 0) {
                double lat = s.getLatitude();
                double lng = s.getLongitude();

                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(lat, lng));
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Judul: nama sampah yang terdeteksi
                String nama = s.getJenisSampah(); // ← nama_sampah dari Supabase
                String kat  = s.getKategori();
                marker.setTitle("");
                marker.setSnippet("");
                marker.setOnMarkerClickListener((m, map) -> {
                    showMarkerDetail(
                            nama,
                            kat,
                            WilayahUtils.formatRtId(s.getRtId()),
                            WilayahUtils.formatRwId(s.getRwId()),
                            lat,
                            lng
                    );
                    map.getController().animateTo(new GeoPoint(lat, lng));
                    return true;
                });

                mapView.getOverlays().add(marker);
                totalLat += lat;
                totalLng += lng;
                count++;
            }
        }

        if (count > 0) {
            // Pusatkan peta ke rata-rata lokasi semua scan
            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(new GeoPoint(totalLat / count, totalLng / count));
        }
        mapView.invalidate();
    }

    private void showMarkerDetail(
            String jenis,
            String kategori,
            String rt,
            String rw,
            double lat,
            double lng) {

        if (tvDetailContent == null) return;

        String detail =
                "Jenis Sampah : " + jenis +
                        "\nKategori : " + kategori +
                        "\nWilayah : " + rt + " / " + rw +
                        "\nLatitude : " + lat +
                        "\nLongitude : " + lng;

        tvDetailContent.setText(detail);
        if (detailMarkerCard != null) {
            detailMarkerCard.setVisibility(View.VISIBLE);
            detailMarkerCard.setAlpha(0f);
            detailMarkerCard.animate().alpha(1f).setDuration(160L).start();
        }
    }

    private void hideMarkerDetail() {
        if (detailMarkerCard != null) {
            detailMarkerCard.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
