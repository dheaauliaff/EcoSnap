package com.example.ecosnap.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.R;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RekapActivity extends AppCompatActivity {

    TextView tvTotalScan, tvJenisTerbanyak;
    BottomNavigationView bottomNav;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekap_user);

        mAuth = FirebaseAuth.getInstance();
        tvTotalScan = findViewById(R.id.tvTotalScan);
        tvJenisTerbanyak = findViewById(R.id.tvJenisTerbanyak);
        bottomNav = findViewById(R.id.bottomNav);

        setupBottomNavigation();
        loadStatistik();
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_statistik);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardUserActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_statistik) {
                return true;
            } else if (id == R.id.nav_scan) {
                startActivity(new Intent(this, ScanActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_maps) {
                startActivity(new Intent(this, MapsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profil) {
                startActivity(new Intent(this, ProfilUserActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadStatistik() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        Call<List<ScanHistory>> call = api.getScanByUser("eq." + currentUser.getUid());

        call.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> list = response.body();
                    int total = list.size();
                    tvTotalScan.setText("📸 Total Scan : " + total);

                    HashMap<String, Integer> map = new HashMap<>();
                    for (ScanHistory item : list) {
                        String jenis = item.getJenisSampah();
                        if (jenis == null) continue;
                        map.put(jenis, map.getOrDefault(jenis, 0) + 1);
                    }

                    String terbanyak = "-";
                    int max = 0;
                    for (Map.Entry<String, Integer> e : map.entrySet()) {
                        if (e.getValue() > max) {
                            max = e.getValue();
                            terbanyak = e.getKey();
                        }
                    }
                    tvJenisTerbanyak.setText("♻️ Jenis Terbanyak : " + terbanyak);
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(RekapActivity.this, "Gagal memuat statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
