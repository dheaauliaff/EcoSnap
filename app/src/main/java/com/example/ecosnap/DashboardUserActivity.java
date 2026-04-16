package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardUserActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    FirebaseAuth mAuth;

    TextView tvNamaUser, tvWilayahUser, tvScanHariIni,
            tvScanMingguIni, tvJenisTerakhir, tvKategoriTerakhir,
            tvWaktuTerakhir, tvQuote;
    AppCompatButton btnScanCepat;

    String[] quotes = {
            "\"Memilah sampah hari ini, menyelamatkan bumi untuk generasi berikutnya 🌍\"",
            "\"Sampah organik bisa jadi kompos, sampah anorganik bisa didaur ulang ♻️\"",
            "\"Lingkungan bersih dimulai dari kebiasaan kecil setiap hari 🌱\"",
            "\"Pilah sampahmu, jaga bumi kita bersama 💚\"",
            "\"Setiap sampah yang dipilah adalah kontribusi nyata untuk lingkungan 🌿\""
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_user);

        mAuth = FirebaseAuth.getInstance();

        // Init views
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        tvNamaUser = findViewById(R.id.tvNamaUser);
        tvWilayahUser = findViewById(R.id.tvWilayahUser);
        tvScanHariIni = findViewById(R.id.tvScanHariIni);
        tvScanMingguIni = findViewById(R.id.tvScanMingguIni);
        tvJenisTerakhir = findViewById(R.id.tvJenisTerakhir);
        tvKategoriTerakhir = findViewById(R.id.tvKategoriTerakhir);
        tvWaktuTerakhir = findViewById(R.id.tvWaktuTerakhir);
        tvQuote = findViewById(R.id.tvQuote);
        btnScanCepat = findViewById(R.id.btnScanCepat);

        // Setup toolbar
        setSupportActionBar(toolbar);

        // Setup drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Tampilkan quote random
        int randomIndex = (int) (Math.random() * quotes.length);
        tvQuote.setText(quotes[randomIndex]);

        // Load data user
        loadDataUser();

        // Tombol scan cepat
        btnScanCepat.setOnClickListener(v ->
                startActivity(new Intent(this, ScanActivity.class)));

        // Setup sidebar
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_scan) {
                startActivity(new Intent(this, ScanActivity.class));
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
            } else if (id == R.id.nav_maps) {
                startActivity(new Intent(this, MapsActivity.class));
            } else if (id == R.id.nav_ranking) {
                startActivity(new Intent(this, RankingActivity.class));
            } else if (id == R.id.nav_profil) {
                startActivity(new Intent(this, ProfilActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadDataUser() {
        String uid = mAuth.getCurrentUser().getUid();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + uid);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    User user = response.body().get(0);

                    // Update welcome banner
                    tvNamaUser.setText(user.getNama());
                    tvWilayahUser.setText(user.getWilayah() + " - " + user.getRwId());

                    // Update header sidebar
                    TextView navNama = navigationView.getHeaderView(0)
                            .findViewById(R.id.tvNamaUser);
                    TextView navWilayah = navigationView.getHeaderView(0)
                            .findViewById(R.id.tvWilayahUser);
                    if (navNama != null) navNama.setText(user.getNama());
                    if (navWilayah != null) navWilayah.setText(
                            user.getWilayah() + " - " + user.getRwId());

                    // Load statistik scan
                    loadStatistikScan(uid);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(DashboardUserActivity.this,
                        "Gagal load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatistikScan(String uid) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Load scan terakhir
        Call<List<ScanHistory>> callTerakhir = apiService.getScanTerakhir("eq." + uid);
        callTerakhir.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call,
                                   Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    ScanHistory last = response.body().get(0);
                    tvJenisTerakhir.setText(last.getJenisSampah());
                    tvKategoriTerakhir.setText(last.getKategori());
                    tvWaktuTerakhir.setText(last.getCreatedAt());
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {}
        });
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}