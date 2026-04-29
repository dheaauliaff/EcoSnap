package com.example.ecosnap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardAdminActivity extends AppCompatActivity {

    // Hapus semua drawer — ganti bottomNav
    BottomNavigationView bottomNav;
    FirebaseAuth mAuth;

    TextView tvNamaAdmin, tvWilayahAdmin, tvTotalSampah, tvTotalOrganik,
            tvTotalAnorganik, tvTotalBukanSampah, tvTotalRecycle;
    Chip btnMinggu, btnBulan, btnTahun;
    LinearLayout layoutRanking;
    BarChart barChart;

    String rwId = "";
    String periodAktif = "minggu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        mAuth = FirebaseAuth.getInstance();

        // Init semua views
        tvNamaAdmin = findViewById(R.id.tvNamaAdmin);
        tvWilayahAdmin = findViewById(R.id.tvWilayahAdmin);
        tvTotalSampah = findViewById(R.id.tvTotalSampah);
        tvTotalOrganik = findViewById(R.id.tvTotalOrganik);
        tvTotalAnorganik = findViewById(R.id.tvTotalAnorganik);
        tvTotalBukanSampah = findViewById(R.id.tvTotalBukanSampah);
        tvTotalRecycle = findViewById(R.id.tvTotalRecycle);
        btnMinggu = findViewById(R.id.btnMinggu);
        btnBulan = findViewById(R.id.btnBulan);
        btnTahun = findViewById(R.id.btnTahun);
        layoutRanking = findViewById(R.id.layoutRanking);
        barChart = findViewById(R.id.barChart);
        bottomNav = findViewById(R.id.bottomNav);

        // Load data admin dari Supabase
        loadDataAdmin();

        // Filter periode statistik
        btnMinggu.setOnClickListener(v -> {
            periodAktif = "minggu";
            loadStatistik();
        });

        btnBulan.setOnClickListener(v -> {
            periodAktif = "bulan";
            loadStatistik();
        });

        btnTahun.setOnClickListener(v -> {
            periodAktif = "tahun";
            loadStatistik();
        });

        // Setup bottom navigation admin
        bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                // Sudah di dashboard
                return true;
            } else if (id == R.id.nav_admin_rekap) {
                startActivity(new Intent(this, RekapanActivity.class));
                return true;
            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                return true;
            } else if (id == R.id.nav_admin_maps) {
                startActivity(new Intent(this, AdminMapsActivity.class));
                return true;
            } else if (id == R.id.nav_admin_profil) {
                startActivity(new Intent(this, ProfilAdminActivity.class));
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

    private void loadDataAdmin() {
        String uid = mAuth.getCurrentUser().getUid();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + uid);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    User admin = response.body().get(0);

                    // Tampilkan nama dan wilayah admin
                    tvNamaAdmin.setText(admin.getNama());
                    tvWilayahAdmin.setText(admin.getWilayah());
                    rwId = admin.getRwId();

                    // Load statistik setelah dapat rwId
                    loadStatistik();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(DashboardAdminActivity.this,
                        "Gagal load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatistik() {
        if (rwId.isEmpty()) return;

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<ScanHistory>> call = apiService.getScanByRw("eq." + rwId);

        call.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call,
                                   Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> allData = response.body();
                    List<ScanHistory> filtered = filterByPeriod(allData);
                    hitungStatistik(filtered);
                    buatGrafik(filtered);
                    buatRanking(filtered);
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(DashboardAdminActivity.this,
                        "Gagal load statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Filter data berdasarkan periode yang dipilih
    private List<ScanHistory> filterByPeriod(List<ScanHistory> data) {
        // TODO: implementasi filter berdasarkan tanggal
        // Untuk sekarang return semua data
        return data;
    }

    private void hitungStatistik(List<ScanHistory> data) {
        int total = data.size();
        int organik = 0, anorganik = 0, bukanSampah = 0, recycle = 0;

        for (ScanHistory s : data) {
            if (s.getKategori() != null) {
                switch (s.getKategori()) {
                    case "organik": organik++; break;
                    case "anorganik": anorganik++; break;
                    case "bukan_sampah": bukanSampah++; break;
                    case "recycle": recycle++; break;
                }
            }
        }

        tvTotalSampah.setText(String.valueOf(total));
        tvTotalOrganik.setText(String.valueOf(organik));
        tvTotalAnorganik.setText(String.valueOf(anorganik));
        tvTotalBukanSampah.setText(String.valueOf(bukanSampah));
        tvTotalRecycle.setText(String.valueOf(recycle));
    }

    private void buatGrafik(List<ScanHistory> data) {
        Map<String, Integer> countMap = new HashMap<>();
        String[] labels = {"Organik", "Kardus", "Kaca", "Logam", "Kertas", "Plastik"};

        for (String label : labels) countMap.put(label.toLowerCase(), 0);

        for (ScanHistory s : data) {
            if (s.getJenisSampah() != null) {
                String jenis = s.getJenisSampah().toLowerCase();
                if (countMap.containsKey(jenis)) {
                    countMap.put(jenis, countMap.get(jenis) + 1);
                }
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            entries.add(new BarEntry(i, countMap.get(labels[i].toLowerCase())));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Jenis Sampah");
        dataSet.setColors(
                Color.parseColor("#2E7D32"),
                Color.parseColor("#388E3C"),
                Color.parseColor("#43A047"),
                Color.parseColor("#4CAF50"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#81C784")
        );
        dataSet.setValueTextColor(Color.parseColor("#1B5E20"));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#1B5E20"));

        barChart.getAxisLeft().setTextColor(Color.parseColor("#1B5E20"));
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }

    private void buatRanking(List<ScanHistory> data) {
        Map<String, Integer> rtCount = new HashMap<>();

        for (ScanHistory s : data) {
            if (s.getWilayah() != null) {
                rtCount.put(s.getWilayah(),
                        rtCount.getOrDefault(s.getWilayah(), 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(rtCount.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        layoutRanking.removeAllViews();

        if (sorted.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada data");
            empty.setTextColor(Color.parseColor("#81C784"));
            layoutRanking.addView(empty);
            return;
        }

        String[] medals = {"🥇", "🥈", "🥉"};

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);

            TextView tvMedal = new TextView(this);
            tvMedal.setText(i < 3 ? medals[i] : (i + 1) + ".");
            tvMedal.setTextSize(16);
            tvMedal.setPadding(0, 0, 16, 0);

            TextView tvNama = new TextView(this);
            tvNama.setText(entry.getKey());
            tvNama.setTextColor(Color.parseColor("#1B5E20"));
            tvNama.setTextSize(14);
            tvNama.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvJumlah = new TextView(this);
            tvJumlah.setText(entry.getValue() + " item");
            tvJumlah.setTextColor(Color.parseColor("#2E7D32"));
            tvJumlah.setTextSize(14);
            tvJumlah.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(tvMedal);
            row.addView(tvNama);
            row.addView(tvJumlah);
            layoutRanking.addView(row);

            if (i < sorted.size() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(Color.parseColor("#E8F5E9"));
                layoutRanking.addView(divider);
            }
        }
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}