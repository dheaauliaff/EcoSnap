package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.ecosnap.model.User;
import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRankingActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private TextView tvTotalRtAktif, tvTotalScanAll, tvDominanJenis;
    private LinearLayout layoutRankingList;

    // Simpan data semua scan supaya bisa difilter per RT
    private List<ScanHistory> allScanList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ranking);

        bottomNav         = findViewById(R.id.bottomNav);
        tvTotalRtAktif    = findViewById(R.id.tvTotalRtAktif);
        tvTotalScanAll    = findViewById(R.id.tvTotalScanAll);
        tvDominanJenis    = findViewById(R.id.tvDominanJenis);
        layoutRankingList = findViewById(R.id.layoutRankingList);

        setupBottomNavigation();
        setupBackPressed();
        loadDataFromSupabase();
    }

    private void loadDataFromSupabase() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getUserByFirebaseUid("eq." + uid).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User admin = response.body().get(0);
                    String rwId = admin.getRwId() != null ? admin.getRwId() : "";
                    if (!rwId.isEmpty()) {
                        fetchScansForRw(api, rwId);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void fetchScansForRw(ApiService api, String rwId) {
        api.getScanByRw("eq." + rwId).enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    allScanList = response.body();
                    processAndDisplay(allScanList);
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

        // Hitung per RT
        Map<String, Integer> rtMap = new HashMap<>();
        Map<String, Integer> namaGlobalMap = new HashMap<>();

        for (ScanHistory s : list) {
            String rt = s.getRtId();
            if (rt != null && !rt.isEmpty()) {
                rtMap.put(rt, rtMap.getOrDefault(rt, 0) + 1);
            }
            String nama = s.getJenisSampah();
            if (nama != null && !nama.isEmpty()) {
                namaGlobalMap.put(nama, namaGlobalMap.getOrDefault(nama, 0) + 1);
            }
        }

        tvTotalRtAktif.setText(String.valueOf(rtMap.size()));

        // Tampilkan jenis dominan global
        String dominan = getDominant(namaGlobalMap);
        if (tvDominanJenis != null) {
            tvDominanJenis.setText("Sampah paling banyak: " + dominan);
        }

        // Sort RT by count
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
            final Map.Entry<String, Integer> entry = rtList.get(i);
            final String rtId = entry.getKey();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(getDrawable(R.drawable.bg_premium_card));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rowParams);
            row.setClickable(true);
            row.setFocusable(true);
            // Ripple effect yang benar
            android.util.TypedValue rippleValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleValue, true);
            row.setForeground(getDrawable(rippleValue.resourceId));

            // Rank badge
            TextView tvRank = new TextView(this);
            tvRank.setText(i < 3 ? medals[i] : (i + 1) + ".");
            tvRank.setTextSize(i < 3 ? 22 : 16);
            LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            rankParams.setMarginEnd(dp(12));
            tvRank.setLayoutParams(rankParams);
            tvRank.setGravity(Gravity.CENTER);

            // Info: nama RT + dominan jenis
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNama = new TextView(this);
            tvNama.setText(rtId);
            tvNama.setTextColor(Color.parseColor("#1B5E20"));
            tvNama.setTextSize(16);
            tvNama.setTypeface(null, Typeface.BOLD);

            // Hitung jenis dominan per RT ini
            Map<String, Integer> rtJenisMap = new HashMap<>();
            for (ScanHistory s : allScanList) {
                if (rtId.equals(s.getRtId()) && s.getJenisSampah() != null) {
                    rtJenisMap.put(s.getJenisSampah(),
                            rtJenisMap.getOrDefault(s.getJenisSampah(), 0) + 1);
                }
            }
            String dominanRT = getDominant(rtJenisMap);

            TextView tvDominan = new TextView(this);
            tvDominan.setText("Terbanyak: " + dominanRT);
            tvDominan.setTextColor(Color.parseColor("#757575"));
            tvDominan.setTextSize(12);
            tvDominan.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            info.addView(tvNama);
            info.addView(tvDominan);

            // Total scan
            TextView tvJumlah = new TextView(this);
            tvJumlah.setText(entry.getValue() + " scan");
            tvJumlah.setTextColor(Color.parseColor("#2E7D32"));
            tvJumlah.setTextSize(14);
            tvJumlah.setTypeface(null, Typeface.BOLD);

            // Arrow icon
            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.ic_back);
            arrow.setRotation(180f);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(16), dp(16));
            arrowParams.setMarginStart(dp(8));
            arrow.setLayoutParams(arrowParams);
            arrow.setColorFilter(Color.parseColor("#BDBDBD"));

            row.addView(tvRank);
            row.addView(info);
            row.addView(tvJumlah);
            row.addView(arrow);
            layoutRankingList.addView(row);

            // Klik → tampilkan detail sampah RT ini
            row.setOnClickListener(v -> showRtDetailBottomSheet(rtId, rtJenisMap, entry.getValue()));
        }
    }

    /** Bottom sheet: detail jenis sampah per RT */
    private void showRtDetailBottomSheet(String rtId, Map<String, Integer> jenisMap, int totalRt) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(24), dp(24), dp(40));

        // Header
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Detail Sampah — " + rtId);
        tvTitle.setTextColor(Color.parseColor("#1B5E20"));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tvTitle);

        // Total
        TextView tvTotal = new TextView(this);
        tvTotal.setText("Total scan: " + totalRt + " item");
        tvTotal.setTextColor(Color.parseColor("#757575"));
        tvTotal.setTextSize(13);
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        totalParams.setMargins(0, dp(4), 0, dp(20));
        tvTotal.setLayoutParams(totalParams);
        container.addView(tvTotal);

        // Sort jenis by count
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(jenisMap.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int[] colors = {
                Color.parseColor("#4CAF50"), // Organik
                Color.parseColor("#FF9800"), // Plastik
                Color.parseColor("#FFC107"), // Kertas
                Color.parseColor("#00BCD4"), // Kaca
                Color.parseColor("#2196F3"), // Kardus
                Color.parseColor("#9C27B0"), // Logam
                Color.parseColor("#F44336")  // default
        };

        int colorIdx = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            int cnt  = e.getValue();
            int pct  = totalRt == 0 ? 0 : Math.round((cnt * 100f) / totalRt);
            int color = colorForNama(e.getKey());

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(12));
            row.setLayoutParams(rowParams);

            // Row label
            LinearLayout labelRow = new LinearLayout(this);
            labelRow.setOrientation(LinearLayout.HORIZONTAL);
            labelRow.setGravity(Gravity.CENTER_VERTICAL);

            // Dot
            FrameLayout dot = new FrameLayout(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(12), dp(12));
            dotParams.setMarginEnd(dp(10));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(color);
            dot.setBackground(dotBg);

            TextView tvNama = new TextView(this);
            tvNama.setText(e.getKey());
            tvNama.setTextColor(Color.parseColor("#212121"));
            tvNama.setTextSize(14);
            tvNama.setTypeface(null, Typeface.BOLD);
            tvNama.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvCnt = new TextView(this);
            tvCnt.setText(cnt + " (" + pct + "%)");
            tvCnt.setTextColor(color);
            tvCnt.setTextSize(13);
            tvCnt.setTypeface(null, Typeface.BOLD);

            labelRow.addView(dot);
            labelRow.addView(tvNama);
            labelRow.addView(tvCnt);

            // Progress bar
            LinearLayout progressBg = new LinearLayout(this);
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setCornerRadius(dp(4));
            bgShape.setColor(Color.parseColor("#F0F0F0"));
            LinearLayout.LayoutParams progressBgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            progressBgParams.setMargins(0, dp(6), 0, 0);
            progressBg.setLayoutParams(progressBgParams);
            progressBg.setBackground(bgShape);

            // Progress bar berbasis weight
            progressBg.setWeightSum(100);

            View progressFill = new View(this);
            GradientDrawable fillShape = new GradientDrawable();
            fillShape.setCornerRadius(dp(4));
            fillShape.setColor(color);
            progressFill.setLayoutParams(new LinearLayout.LayoutParams(0, dp(8), Math.max(pct, 1)));
            progressFill.setBackground(fillShape);

            View progressEmpty = new View(this);
            progressEmpty.setLayoutParams(new LinearLayout.LayoutParams(0, dp(8), Math.max(100 - pct, 0)));

            progressBg.addView(progressFill);
            progressBg.addView(progressEmpty);

            row.addView(labelRow);
            row.addView(progressBg);
            container.addView(row);
            colorIdx++;
        }

        if (sorted.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada data untuk RT ini");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            container.addView(empty);
        }

        dialog.setContentView(container);
        dialog.show();
    }

    private String getDominant(Map<String, Integer> map) {
        String dom = "-"; int max = 0;
        for (Map.Entry<String, Integer> e : map.entrySet())
            if (e.getValue() > max) { max = e.getValue(); dom = e.getKey(); }
        return dom;
    }

    private int colorForNama(String nama) {
        if (nama == null) return Color.parseColor("#9E9E9E");
        switch (nama.toLowerCase()) {
            case "organik":  return Color.parseColor("#4CAF50");
            case "plastik":  return Color.parseColor("#FF9800");
            case "kertas":   return Color.parseColor("#FFC107");
            case "kaca":     return Color.parseColor("#00BCD4");
            case "kardus":   return Color.parseColor("#2196F3");
            case "logam":    return Color.parseColor("#9C27B0");
            default:         return Color.parseColor("#F44336");
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
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