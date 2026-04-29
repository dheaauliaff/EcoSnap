package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardUserActivity extends AppCompatActivity {

    // Hapus semua drawer — ganti bottomNav
    BottomNavigationView bottomNav;
    FirebaseAuth mAuth;

    TextView tvNamaUser, tvWilayahUser, tvScanHariIni,
            tvScanMingguIni, tvJenisTerakhir, tvKategoriTerakhir,
            tvWaktuTerakhir, tvQuote;
    AppCompatButton btnScanCepat;

    String[] quotes = {
            "\"Memilah sampah hari ini, menyelamatkan bumi 🌍\"",
            "\"Sampah organik bisa jadi kompos ♻️\"",
            "\"Lingkungan bersih dimulai dari kita 🌱\"",
            "\"Pilah sampahmu 💚\"",
            "\"Setiap sampah berarti 🌿\""
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_user);

        mAuth = FirebaseAuth.getInstance();

        // Init semua views
        tvNamaUser = findViewById(R.id.tvNamaUser);
        tvWilayahUser = findViewById(R.id.tvWilayahUser);
        tvScanHariIni = findViewById(R.id.tvScanHariIni);
        tvScanMingguIni = findViewById(R.id.tvScanMingguIni);
        tvJenisTerakhir = findViewById(R.id.tvJenisTerakhir);
        tvKategoriTerakhir = findViewById(R.id.tvKategoriTerakhir);
        tvWaktuTerakhir = findViewById(R.id.tvWaktuTerakhir);
        tvQuote = findViewById(R.id.tvQuote);
        btnScanCepat = findViewById(R.id.btnScanCepat);
        bottomNav = findViewById(R.id.bottomNav);

        // Quote random
        int randomIndex = (int) (Math.random() * quotes.length);
        tvQuote.setText(quotes[randomIndex]);

        // Load data user dari Supabase
        loadDataUser();

        // Tombol scan cepat di banner
        btnScanCepat.setOnClickListener(v ->
                startActivity(new Intent(this, ScanActivity.class)));

        // Setup bottom navigation
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Sudah di home
                return true;
            } else if (id == R.id.nav_statistik) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_scan) {
                startActivity(new Intent(this, ScanActivity.class));
                return true;
            } else if (id == R.id.nav_maps) {
                startActivity(new Intent(this, MapsActivity.class));
                return true;
            } else if (id == R.id.nav_profil) {
                startActivity(new Intent(this, ProfilUserActivity.class));
                return true;
            }
            return false;
        });

        // Handle tombol back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void loadDataUser() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + uid);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    User user = response.body().get(0);

                    // Tampilkan nama dan wilayah RT - RW
                    tvNamaUser.setText(user.getNama());
                    tvWilayahUser.setText(user.getRtId() + " - " + user.getRwId());

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

        // Ambil semua scan user untuk hitung hari ini & minggu ini
        Call<List<ScanHistory>> call = apiService.getScanByUser("eq." + uid);
        call.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call,
                                   Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> data = response.body();

                    int hariIni = 0;
                    int mingguIni = 0;

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Calendar sekarang = Calendar.getInstance();

                    for (ScanHistory s : data) {
                        try {
                            if (s.getCreatedAt() == null) continue;
                            Date tanggal = sdf.parse(s.getCreatedAt());
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(tanggal);

                            // Cek hari ini
                            if (cal.get(Calendar.DAY_OF_YEAR) ==
                                    sekarang.get(Calendar.DAY_OF_YEAR)
                                    && cal.get(Calendar.YEAR) ==
                                    sekarang.get(Calendar.YEAR)) {
                                hariIni++;
                            }

                            // Cek minggu ini
                            if (cal.get(Calendar.WEEK_OF_YEAR) ==
                                    sekarang.get(Calendar.WEEK_OF_YEAR)
                                    && cal.get(Calendar.YEAR) ==
                                    sekarang.get(Calendar.YEAR)) {
                                mingguIni++;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    tvScanHariIni.setText(String.valueOf(hariIni));
                    tvScanMingguIni.setText(String.valueOf(mingguIni));
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(DashboardUserActivity.this,
                        "Gagal load statistik", Toast.LENGTH_SHORT).show();
            }
        });

        // Ambil scan terakhir
        Call<List<ScanHistory>> callLast = apiService.getScanTerakhir("eq." + uid);
        callLast.enqueue(new Callback<List<ScanHistory>>() {
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
}