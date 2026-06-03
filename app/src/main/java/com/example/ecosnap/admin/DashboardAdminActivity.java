package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.app.DatePickerDialog;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    String selectedStartDate = "";
    String selectedEndDate = "";
    String selectedMonthKey = "";
    String selectedYearKey = "2026";
    final List<ScanHistory> cachedScans = new ArrayList<>();

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
        updateChipStyle(); // terapkan highlight chip default (minggu)

        // Period filters
        if (btnMinggu != null) btnMinggu.setOnClickListener(v -> {
            periodAktif = "minggu";
            updateChipStyle();
            showDateRangePicker();
        });
        if (btnBulan != null) btnBulan.setOnClickListener(v -> {
            periodAktif = "bulan";
            updateChipStyle();
            showMonthPicker();
        });
        if (btnTahun != null) btnTahun.setOnClickListener(v -> {
            periodAktif = "tahun";
            updateChipStyle();
            showYearPicker();
        });

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

    /** Highlight chip yang sedang aktif, reset yang lain */
    private void updateChipStyle() {
        Chip[] chips   = { btnMinggu, btnBulan, btnTahun };
        String[] keys  = { "minggu",  "bulan",  "tahun"  };

        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean aktif = keys[i].equals(periodAktif);
            if (aktif) {
                // Chip aktif: solid hijau + teks putih
                chips[i].setChipBackgroundColorResource(R.color.colorPrimary); // hijau tema
                chips[i].setTextColor(android.graphics.Color.WHITE);
                chips[i].setChipStrokeWidth(0f);
                chips[i].setElevation(4f);
            } else {
                // Chip non-aktif: putih + border abu tipis + teks abu
                chips[i].setChipBackgroundColor(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#F5F5F5")));
                chips[i].setTextColor(android.graphics.Color.parseColor("#757575"));
                chips[i].setChipStrokeColor(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#BDBDBD")));
                chips[i].setChipStrokeWidth(1.5f);
                chips[i].setElevation(0f);
            }
        }
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
                    cachedScans.clear();
                    cachedScans.addAll(response.body());
                    initializeDefaultPeriod();
                    applyCurrentPeriod();
                }
            }
            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(DashboardAdminActivity.this, "Gagal load statistik", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<ScanHistory> filterByPeriod(List<ScanHistory> data) {
        List<ScanHistory> filtered = new ArrayList<>();
        for (ScanHistory s : data) {
            String date = extractDate(s.getCreatedAt());
            if (date.isEmpty()) continue;

            if ("minggu".equals(periodAktif)) {
                if (!selectedStartDate.isEmpty() && !selectedEndDate.isEmpty()
                        && date.compareTo(selectedStartDate) >= 0
                        && date.compareTo(selectedEndDate) <= 0) {
                    filtered.add(s);
                }
            } else if ("bulan".equals(periodAktif)) {
                if (!selectedMonthKey.isEmpty() && date.startsWith(selectedMonthKey)) {
                    filtered.add(s);
                }
            } else if ("tahun".equals(periodAktif)) {
                if (!selectedYearKey.isEmpty() && date.startsWith(selectedYearKey)) {
                    filtered.add(s);
                }
            } else {
                filtered.add(s);
            }
        }
        return filtered;
    }

    private void applyCurrentPeriod() {
        List<ScanHistory> filtered = filterByPeriod(cachedScans);
        hitungStatistik(filtered);
        buatGrafik(filtered);
        buatRanking(filtered);
    }

    private void initializeDefaultPeriod() {
        if (selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            selectedEndDate = formatDate(cal);
            cal.add(Calendar.DAY_OF_YEAR, -6);
            selectedStartDate = formatDate(cal);
        }

        if (selectedMonthKey.isEmpty()) {
            List<String> months = getAvailableMonths();
            if (!months.isEmpty()) {
                selectedMonthKey = months.get(0);
            }
        }

        if (selectedYearKey.isEmpty()) {
            selectedYearKey = "2026";
        }
    }

    private void showDateRangePicker() {
        Calendar startCal = parseDateToCalendar(selectedStartDate);
        DatePickerDialog startDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedStart = Calendar.getInstance();
                    selectedStart.set(year, month, dayOfMonth);
                    selectedStartDate = formatDate(selectedStart);

                    Calendar endCal = parseDateToCalendar(selectedEndDate);
                    DatePickerDialog endDialog = new DatePickerDialog(this,
                            (endView, endYear, endMonth, endDayOfMonth) -> {
                                Calendar selectedEnd = Calendar.getInstance();
                                selectedEnd.set(endYear, endMonth, endDayOfMonth);
                                selectedEndDate = formatDate(selectedEnd);

                                if (selectedStartDate.compareTo(selectedEndDate) > 0) {
                                    String temp = selectedStartDate;
                                    selectedStartDate = selectedEndDate;
                                    selectedEndDate = temp;
                                }

                                Toast.makeText(this,
                                        "Periode: " + selectedStartDate + " s/d " + selectedEndDate,
                                        Toast.LENGTH_SHORT).show();
                                applyCurrentPeriod();
                            },
                            endCal.get(Calendar.YEAR),
                            endCal.get(Calendar.MONTH),
                            endCal.get(Calendar.DAY_OF_MONTH));
                    endDialog.setTitle("Pilih tanggal akhir");
                    endDialog.show();
                },
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH));
        startDialog.setTitle("Pilih tanggal awal");
        startDialog.show();
    }

    private void showMonthPicker() {
        List<String> months = getAvailableMonths();
        if (months.isEmpty()) {
            Toast.makeText(this, "Belum ada data bulan dari scan", Toast.LENGTH_SHORT).show();
            applyCurrentPeriod();
            return;
        }

        String[] labels = new String[months.size()];
        int checked = 0;
        for (int i = 0; i < months.size(); i++) {
            labels[i] = formatMonthLabel(months.get(i));
            if (months.get(i).equals(selectedMonthKey)) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih bulan")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    selectedMonthKey = months.get(which);
                    dialog.dismiss();
                    applyCurrentPeriod();
                })
                .show();
    }

    private void showYearPicker() {
        List<String> years = getAvailableYears();
        if (years.isEmpty()) {
            years.add("2026");
        }

        String[] labels = years.toArray(new String[0]);
        int checked = 0;
        for (int i = 0; i < years.size(); i++) {
            if (years.get(i).equals(selectedYearKey)) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih tahun")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    selectedYearKey = years.get(which);
                    dialog.dismiss();
                    applyCurrentPeriod();
                })
                .show();
    }

    private List<String> getAvailableMonths() {
        Set<String> months = new LinkedHashSet<>();
        List<String> sorted = new ArrayList<>();
        for (ScanHistory s : cachedScans) {
            String date = extractDate(s.getCreatedAt());
            if (date.length() >= 7) sorted.add(date.substring(0, 7));
        }
        Collections.sort(sorted);
        months.addAll(sorted);
        return new ArrayList<>(months);
    }

    private List<String> getAvailableYears() {
        Set<String> years = new LinkedHashSet<>();
        List<String> sorted = new ArrayList<>();
        for (ScanHistory s : cachedScans) {
            String date = extractDate(s.getCreatedAt());
            if (date.length() >= 4) sorted.add(date.substring(0, 4));
        }
        Collections.sort(sorted);
        years.addAll(sorted);
        return new ArrayList<>(years);
    }

    private String extractDate(String createdAt) {
        if (createdAt == null || createdAt.length() < 10) return "";
        return createdAt.substring(0, 10);
    }

    private String formatDate(Calendar cal) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private Calendar parseDateToCalendar(String date) {
        Calendar cal = Calendar.getInstance();
        if (date != null && date.length() >= 10) {
            try {
                int year = Integer.parseInt(date.substring(0, 4));
                int month = Integer.parseInt(date.substring(5, 7)) - 1;
                int day = Integer.parseInt(date.substring(8, 10));
                cal.set(year, month, day);
            } catch (NumberFormatException ignored) {}
        }
        return cal;
    }

    private String formatMonthLabel(String monthKey) {
        if (monthKey == null || monthKey.length() < 7) return monthKey;
        String[] monthNames = {
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };
        try {
            int month = Integer.parseInt(monthKey.substring(5, 7));
            String year = monthKey.substring(0, 4);
            if (month >= 1 && month <= 12) {
                return monthNames[month - 1] + " " + year;
            }
        } catch (NumberFormatException ignored) {}
        return monthKey;
    }

    private String formatRtId(String rtId) {
        if (rtId == null) return "";
        String clean = rtId.replace("RT", "").replace("rt", "").trim();
        if (clean.isEmpty()) return "";
        try {
            return "RT " + String.format(Locale.US, "%02d", Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toUpperCase().startsWith("RT") ? clean : "RT " + clean;
        }
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
            String rt = formatRtId(s.getRtId());
            if (!rt.isEmpty()) {
                rtCount.put(rt, rtCount.getOrDefault(rt, 0) + 1);
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
            String rt = formatRtId(s.getRtId());
            if (!rt.isEmpty()) {
                rtScanCount.put(rt, rtScanCount.getOrDefault(rt, 0) + 1);
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
