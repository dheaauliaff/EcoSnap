package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.auth.LoginActivity;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.model.User;
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

    BottomNavigationView bottomNav;
    FirebaseAuth mAuth;

    TextView tvNamaAdmin, tvWilayahAdmin,
            tvTotalSampah, tvTotalOrganik, tvTotalAnorganik,
            tvTotalBukanSampah, tvTotalRecycle, tvTotalRtAktif;
    Chip btnMinggu, btnBulan, btnTahun;
    LinearLayout layoutRanking;
    BarChart barChart;
    AppCompatButton btnLihatPeta;

    String rwId = "";
    String periodAktif = "minggu";

    // RT tracking
    final Map<String, Integer> rtCount = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        mAuth = FirebaseAuth.getInstance();

        // Bind views
        tvNamaAdmin = findViewById(R.id.tvNamaAdmin);
        tvWilayahAdmin = findViewById(R.id.tvWilayahAdmin);
        tvTotalSampah = findViewById(R.id.tvTotalSampah);
        tvTotalOrganik = findViewById(R.id.tvTotalOrganik);
        tvTotalAnorganik = findViewById(R.id.tvTotalAnorganik);
        tvTotalBukanSampah = findViewById(R.id.tvTotalBukanSampah);
        tvTotalRecycle = findViewById(R.id.tvTotalRecycle);
        tvTotalRtAktif = findViewById(R.id.tvTotalRtAktif);
        btnMinggu = findViewById(R.id.btnMinggu);
        btnBulan = findViewById(R.id.btnBulan);
        btnTahun = findViewById(R.id.btnTahun);
        layoutRanking = findViewById(R.id.layoutRanking);
        barChart = findViewById(R.id.barChart);
        bottomNav = findViewById(R.id.bottomNav);
        btnLihatPeta = findViewById(R.id.btnLihatPeta);

        // CTA → Maps
        if (btnLihatPeta != null) {
            btnLihatPeta.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminMapsActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        loadDataAdmin();

        // Period filters
        if (btnMinggu != null) btnMinggu.setOnClickListener(v -> { periodAktif = "minggu"; loadStatistik(); });
        if (btnBulan  != null) btnBulan.setOnClickListener(v  -> { periodAktif = "bulan";  loadStatistik(); });
        if (btnTahun  != null) btnTahun.setOnClickListener(v  -> { periodAktif = "tahun";  loadStatistik(); });

        // Bottom nav
        bottomNav.setSelectedItemId(R.id.nav_admin_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                return true;
            } else if (id == R.id.nav_admin_rekap) {
                startActivity(new Intent(this, RekapAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_maps) {
                startActivity(new Intent(this, AdminMapsActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_profil) {
                startActivity(new Intent(this, ProfilAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void loadDataAdmin() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getUserByFirebaseUid("eq." + uid).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User admin = response.body().get(0);
                    if (tvNamaAdmin != null) tvNamaAdmin.setText(admin.getNama());
                    if (tvWilayahAdmin != null) tvWilayahAdmin.setText(admin.getWilayah());
                    rwId = admin.getRwId() != null ? admin.getRwId() : "";
                    loadStatistik();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(DashboardAdminActivity.this, "Gagal load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatistik() {
        if (rwId == null || rwId.isEmpty()) return;
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getScanByRw("eq." + rwId).enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
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
                Toast.makeText(DashboardAdminActivity.this, "Gagal load statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<ScanHistory> filterByPeriod(List<ScanHistory> data) {
        // TODO: filter by date based on periodAktif
        return data;
    }

    private void hitungStatistik(List<ScanHistory> data) {
        rtCount.clear();
        int total = data.size();
        int organik = 0, anorganik = 0, bukanSampah = 0, recycle = 0;

        for (ScanHistory s : data) {
            if (s.getKategori() != null) {
                String kat = s.getKategori().toLowerCase().replace(" ", "_");
                switch (kat) {
                    case "organik":       organik++;      break;
                    case "anorganik":     anorganik++;    break;
                    case "bukan_sampah":  bukanSampah++;  break;
                    case "recycle":       recycle++;      break;
                }
            }
            if (s.getRtId() != null && !s.getRtId().isEmpty()) {
                rtCount.put(s.getRtId(), rtCount.getOrDefault(s.getRtId(), 0) + 1);
            }
        }

        if (tvTotalSampah    != null) tvTotalSampah.setText(String.valueOf(total));
        if (tvTotalOrganik   != null) tvTotalOrganik.setText(String.valueOf(organik));
        if (tvTotalAnorganik != null) tvTotalAnorganik.setText(String.valueOf(anorganik));
        if (tvTotalBukanSampah != null) tvTotalBukanSampah.setText(String.valueOf(bukanSampah));
        if (tvTotalRecycle   != null) tvTotalRecycle.setText(String.valueOf(recycle));
        if (tvTotalRtAktif   != null) tvTotalRtAktif.setText(String.valueOf(rtCount.size()));
    }

    private void buatGrafik(List<ScanHistory> data) {
        if (barChart == null) return;
        Map<String, Integer> countMap = new HashMap<>();
        String[] labels = {"Organik", "Kardus", "Kaca", "Logam", "Kertas", "Plastik"};
        for (String label : labels) countMap.put(label.toLowerCase(), 0);

        for (ScanHistory s : data) {
            if (s.getJenisSampah() != null) {
                String jenis = s.getJenisSampah().toLowerCase();
                if (countMap.containsKey(jenis)) countMap.put(jenis, countMap.get(jenis) + 1);
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            entries.add(new BarEntry(i, countMap.get(labels[i].toLowerCase())));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Jenis Sampah");
        dataSet.setColors(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#FF9800")
        );
        dataSet.setValueTextColor(Color.parseColor("#333333"));
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
        xAxis.setTextColor(Color.parseColor("#333333"));

        barChart.getAxisLeft().setTextColor(Color.parseColor("#333333"));
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }

    private void buatRanking(List<ScanHistory> data) {
        if (layoutRanking == null) return;
        Map<String, Integer> rtScanCount = new HashMap<>();

        for (ScanHistory s : data) {
            if (s.getWilayah() != null) {
                rtScanCount.put(s.getWilayah(), rtScanCount.getOrDefault(s.getWilayah(), 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(rtScanCount.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        layoutRanking.removeAllViews();

        if (sorted.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada data");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            layoutRanking.addView(empty);
            return;
        }

        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvMedal = new TextView(this);
            tvMedal.setText(i < 3 ? medals[i] : (i + 1) + ".");
            tvMedal.setTextSize(16);
            tvMedal.setPadding(0, 0, 16, 0);

            TextView tvNama = new TextView(this);
            tvNama.setText(entry.getKey());
            tvNama.setTextColor(Color.parseColor("#212121"));
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
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
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