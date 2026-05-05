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

import com.example.ecosnap.R;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRankingActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TextView tvTotalRtAktif, tvTotalScanAll;
    private LinearLayout layoutRankingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ranking);

        bottomNav = findViewById(R.id.bottomNav);
        tvTotalRtAktif = findViewById(R.id.tvTotalRtAktif);
        tvTotalScanAll = findViewById(R.id.tvTotalScanAll);
        layoutRankingList = findViewById(R.id.layoutRankingList);

        setupBottomNavigation();
        setupBackPressed();
        loadDataFromSupabase();
    }

    private void loadDataFromSupabase() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    processAndDisplay(response.body());
                } else {
                    Toast.makeText(AdminRankingActivity.this, "Gagal memuat ranking", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (!isFinishing()) {
                    Toast.makeText(AdminRankingActivity.this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void processAndDisplay(List<ScanHistory> list) {
        int totalScans = list.size();
        tvTotalScanAll.setText(String.valueOf(totalScans));

        Map<String, Integer> rtMap = new HashMap<>();
        for (ScanHistory s : list) {
            String rt = s.getRtId();
            if (rt != null && !rt.isEmpty()) {
                rtMap.put(rt, rtMap.getOrDefault(rt, 0) + 1);
            }
        }

        tvTotalRtAktif.setText(String.valueOf(rtMap.size()));

        List<Map.Entry<String, Integer>> rtList = new ArrayList<>(rtMap.entrySet());
        rtList.sort((a, b) -> b.getValue() - a.getValue());

        layoutRankingList.removeAllViews();

        if (rtList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada data scan yang tercatat.");
            empty.setTextColor(Color.parseColor("#757575"));
            empty.setTextSize(14);
            layoutRankingList.addView(empty);
            return;
        }

        String[] medals = {"🥇", "🥈", "🥉"};

        for (int i = 0; i < rtList.size(); i++) {
            Map.Entry<String, Integer> entry = rtList.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvRank = new TextView(this);
            tvRank.setText(i < 3 ? medals[i] : (i + 1) + ".");
            tvRank.setTextSize(i < 3 ? 20 : 16);
            tvRank.setTextColor(Color.parseColor("#757575"));
            tvRank.setPadding(0, 0, 16, 0);

            TextView tvNama = new TextView(this);
            tvNama.setText(entry.getKey());
            tvNama.setTextColor(Color.parseColor("#1B5E20"));
            tvNama.setTextSize(16);
            tvNama.setTypeface(null, android.graphics.Typeface.BOLD);
            tvNama.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvJumlah = new TextView(this);
            tvJumlah.setText(entry.getValue() + " scan");
            tvJumlah.setTextColor(Color.parseColor("#2E7D32"));
            tvJumlah.setTextSize(14);
            tvJumlah.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(tvRank);
            row.addView(tvNama);
            row.addView(tvJumlah);
            layoutRankingList.addView(row);

            if (i < rtList.size() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
                layoutRankingList.addView(divider);
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.nav_admin_ranking);
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
                startActivity(new Intent(AdminRankingActivity.this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }
}