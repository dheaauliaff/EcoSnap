package com.example.ecosnap.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.DonutChartView;
import com.example.ecosnap.R;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import androidx.activity.OnBackPressedCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RekapAdminActivity extends AppCompatActivity {

    private TextView tvTotalSemua, tvTotalTengah;
    private DonutChartView donutChart;
    private TextView tvStatOrganik, tvStatKardus, tvStatKaca, tvStatLogam, tvStatKertas, tvStatPlastik;
    private TextView tvRank1RT, tvRank1Total, tvRank2RT, tvRank2Total, tvRank3RT, tvRank3Total;
    private TextView tvRank4RT, tvRank4Total, tvRank5RT, tvRank5Total;
    private TextView tvRank6RT, tvRank6Total, tvRank7RT, tvRank7Total, tvRank8RT, tvRank8Total;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekap_admin);

        bottomNav = findViewById(R.id.bottomNav);

        // Distribusi views
        tvTotalSemua  = findViewById(R.id.tvTotalSemua);
        tvTotalTengah = findViewById(R.id.tvTotalTengah);
        donutChart    = findViewById(R.id.donutChart);

        tvStatOrganik = findViewById(R.id.tvStatOrganik);
        tvStatKardus  = findViewById(R.id.tvStatKardus);
        tvStatKaca    = findViewById(R.id.tvStatKaca);
        tvStatLogam   = findViewById(R.id.tvStatLogam);
        tvStatKertas  = findViewById(R.id.tvStatKertas);
        tvStatPlastik = findViewById(R.id.tvStatPlastik);

        // Ranking podium (top 3)
        tvRank1RT    = findViewById(R.id.tvRank1RT);
        tvRank1Total = findViewById(R.id.tvRank1Total);
        tvRank2RT    = findViewById(R.id.tvRank2RT);
        tvRank2Total = findViewById(R.id.tvRank2Total);
        tvRank3RT    = findViewById(R.id.tvRank3RT);
        tvRank3Total = findViewById(R.id.tvRank3Total);

        // Ranking list (4-8)
        tvRank4RT    = findViewById(R.id.tvRank4RT);
        tvRank4Total = findViewById(R.id.tvRank4Total);
        tvRank5RT    = findViewById(R.id.tvRank5RT);
        tvRank5Total = findViewById(R.id.tvRank5Total);
        tvRank6RT    = findViewById(R.id.tvRank6RT);
        tvRank6Total = findViewById(R.id.tvRank6Total);
        tvRank7RT    = findViewById(R.id.tvRank7RT);
        tvRank7Total = findViewById(R.id.tvRank7Total);
        tvRank8RT    = findViewById(R.id.tvRank8RT);
        tvRank8Total = findViewById(R.id.tvRank8Total);

        setupBottomNavigation();
        setupBackPressed();
        loadDataFromSupabase();
    }

    // ─── Ambil semua scan dari Supabase ──────────────────────────────────────

    private void loadDataFromSupabase() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    processAndDisplay(response.body());
                } else {
                    showError("Gagal memuat statistik (server)");
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (!isFinishing()) showError("Gagal terhubung ke server");
            }
        });
    }

    // ─── Proses data Supabase dan tampilkan ke UI ─────────────────────────────

    private void processAndDisplay(List<ScanHistory> list) {
        int total = list.size();

        // Hitung per jenis sampah (nama_sampah) dan per RT
        Map<String, Integer> namaMap = new HashMap<>();
        Map<String, Integer> rtMap   = new HashMap<>();

        for (ScanHistory s : list) {
            String nama = s.getJenisSampah();
            if (nama != null && !nama.isEmpty()) {
                namaMap.put(nama, namaMap.getOrDefault(nama, 0) + 1);
            }
            String rt = s.getRtId();
            if (rt != null && !rt.isEmpty()) {
                rtMap.put(rt, rtMap.getOrDefault(rt, 0) + 1);
            }
        }

        // Total
        if (tvTotalSemua  != null) tvTotalSemua.setText("Total " + total);
        if (tvTotalTengah != null) tvTotalTengah.setText(total > 0 ? String.valueOf(total) : "-");

        // Donut chart
        if (donutChart != null) {
            float[] arr = new float[6];
            arr[0] = namaMap.getOrDefault("Organik", 0);
            arr[1] = namaMap.getOrDefault("Plastik", 0);
            arr[2] = namaMap.getOrDefault("Kertas",  0);
            arr[3] = namaMap.getOrDefault("Kaca",    0);
            arr[4] = namaMap.getOrDefault("Kardus",  0);
            arr[5] = namaMap.getOrDefault("Logam",   0);
            donutChart.setValues(arr);
        }

        // Teks per kategori
        updateStatText(tvStatOrganik, namaMap.getOrDefault("Organik", 0), total);
        updateStatText(tvStatKardus,  namaMap.getOrDefault("Kardus",  0), total);
        updateStatText(tvStatKaca,    namaMap.getOrDefault("Kaca",    0), total);
        updateStatText(tvStatLogam,   namaMap.getOrDefault("Logam",   0), total);
        updateStatText(tvStatKertas,  namaMap.getOrDefault("Kertas",  0), total);
        updateStatText(tvStatPlastik, namaMap.getOrDefault("Plastik", 0), total);

        // Ranking RT (urutkan descending)
        List<Map.Entry<String, Integer>> rtList = new ArrayList<>(rtMap.entrySet());
        rtList.sort((a, b) -> b.getValue() - a.getValue());

        updateRankRow(tvRank1RT, tvRank1Total, rtList, 0);
        updateRankRow(tvRank2RT, tvRank2Total, rtList, 1);
        updateRankRow(tvRank3RT, tvRank3Total, rtList, 2);
        updateRankRow(tvRank4RT, tvRank4Total, rtList, 3);
        updateRankRow(tvRank5RT, tvRank5Total, rtList, 4);
        updateRankRow(tvRank6RT, tvRank6Total, rtList, 5);
        updateRankRow(tvRank7RT, tvRank7Total, rtList, 6);
        updateRankRow(tvRank8RT, tvRank8Total, rtList, 7);
    }

    // ─── Helper UI ───────────────────────────────────────────────────────────

    private void updateStatText(TextView tv, int value, int total) {
        if (tv == null) return;
        if (value > 0) {
            int pct = total == 0 ? 0 : Math.round((value * 100f) / total);
            tv.setText(value + " (" + pct + "%)");
        } else {
            tv.setText("-");
        }
    }

    private void updateRankRow(TextView tvName, TextView tvTotal,
                               List<Map.Entry<String, Integer>> list, int index) {
        if (index < list.size()) {
            Map.Entry<String, Integer> entry = list.get(index);
            if (tvName  != null) tvName.setText(entry.getKey());
            if (tvTotal != null) tvTotal.setText(String.valueOf(entry.getValue()));
        } else {
            if (tvName  != null) tvName.setText("-");
            if (tvTotal != null) tvTotal.setText("-");
        }
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_admin_rekap);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_admin_rekap) {
                return true;
            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_admin_maps) {
                startActivity(new Intent(this, AdminMapsActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
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

    private void setupBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(RekapAdminActivity.this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }
}