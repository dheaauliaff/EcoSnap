package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.app.DatePickerDialog;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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
import com.example.ecosnap.WilayahUtils;
import com.example.ecosnap.model.User;
import com.example.ecosnap.user.HistoryActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.YAxis;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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
    String selectedYearKey = "";
    boolean customWeekRangeSelected = false;
    boolean customMonthSelected = false;
    boolean customYearSelected = false;
    final List<ScanHistory> cachedScans = new ArrayList<>();
    final List<String> registeredRtLabels = new ArrayList<>();

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
        setupCategoryHistoryCard(findViewById(R.id.cardOrganik), "Organik");
        setupCategoryHistoryCard(findViewById(R.id.cardAnorganik), "Anorganik");
        setupCategoryHistoryCard(findViewById(R.id.cardRecycle), "Recycle");
        setupCategoryHistoryCard(findViewById(R.id.cardBukanSampah), "Bukan Sampah");
        View cardTotalScan = findViewById(R.id.cardTotalScan);
        if (cardTotalScan != null) {
            cardTotalScan.setOnClickListener(v -> openAdminHistory(""));
        }

        loadDataAdmin();
        updateChipStyle(); // terapkan highlight chip default (minggu)

        // Period filters
        if (btnMinggu != null) btnMinggu.setOnClickListener(v -> {
            periodAktif = "minggu";
            customWeekRangeSelected = false;
            resetCurrentWeekRange();
            updateChipStyle();
            applyCurrentPeriod();
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

    private void setupCategoryHistoryCard(View card, String kategori) {
        if (card == null) return;
        card.setOnClickListener(v -> openAdminHistory(kategori));
    }

    private void openAdminHistory(String kategori) {
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("admin_view", true);
        if (kategori != null && !kategori.isEmpty()) {
            intent.putExtra("category_filter", kategori);
            intent.putExtra("filter_mode", "kategori");
        }
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rwId != null && !rwId.isEmpty()) {
            loadStatistik();
        }
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
                    loadRegisteredRtUsers(api);
                    loadStatistik();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(DashboardAdminActivity.this, "Gagal load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRegisteredRtUsers(ApiService api) {
        registeredRtLabels.clear();
        api.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (User user : response.body()) {
                        if (!"user".equalsIgnoreCase(user.getRole())) continue;
                        if (!WilayahUtils.isMatchingRw(user.getRwId(), rwId)) continue;
                        String rt = WilayahUtils.formatRtId(user.getRtId());
                        if (!rt.isEmpty() && !registeredRtLabels.contains(rt)) {
                            registeredRtLabels.add(rt);
                        }
                    }
                    Collections.sort(registeredRtLabels);
                    if (tvTotalRtAktif != null) {
                        tvTotalRtAktif.setText(String.valueOf(registeredRtLabels.size()));
                    }
                    applyCurrentPeriod();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (tvTotalRtAktif != null) tvTotalRtAktif.setText("0");
            }
        });
    }

    private void loadStatistik() {
        if (rwId == null || rwId.isEmpty()) return;
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedScans.clear();
                    for (ScanHistory s : response.body()) {
                        if (WilayahUtils.isMatchingRw(s.getRwId(), rwId)) {
                            cachedScans.add(s);
                        }
                    }
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
        hitungStatistik(cachedScans);
        buatGrafik(filtered);
        buatRanking(cachedScans);
    }

    private void initializeDefaultPeriod() {
        if (selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
            resetCurrentWeekRange();
        } else if ("minggu".equals(periodAktif) && !customWeekRangeSelected) {
            resetCurrentWeekRange();
        }

        if (selectedMonthKey.isEmpty() || !customMonthSelected) {
            selectedMonthKey = getDefaultMonthKey();
        }

        if (selectedYearKey.isEmpty() || !customYearSelected) {
            selectedYearKey = getDefaultYearKey();
        }
    }

    private void resetCurrentWeekRange() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        selectedStartDate = formatDate(cal);
        cal.add(Calendar.DAY_OF_YEAR, 6);
        selectedEndDate = formatDate(cal);
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

                                customWeekRangeSelected = true;
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

        String[] months = {
                "Januari", "Februari", "Maret", "April",
                "Mei", "Juni", "Juli", "Agustus",
                "September", "Oktober", "November", "Desember"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_month_wheel, null);

        NumberPicker picker = view.findViewById(R.id.monthPicker);

        picker.setMinValue(0);
        picker.setMaxValue(11);
        picker.setDisplayedValues(months);
        picker.setWrapSelectorWheel(true);

        // set default selected
        if (selectedMonthKey != null && !selectedMonthKey.isEmpty()) {
            try {
                int monthIndex = Integer.parseInt(selectedMonthKey.substring(5, 7)) - 1;
                picker.setValue(monthIndex);
            } catch (Exception ignored) {}
        }

        builder.setView(view);

        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnOk).setOnClickListener(v -> {

            int selectedIndex = picker.getValue();
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);

            selectedMonthKey = String.format(Locale.US, "%04d-%02d",
                    year, selectedIndex + 1);

            customMonthSelected = true;
            applyCurrentPeriod();

            Toast.makeText(this,
                    "Bulan: " + months[selectedIndex],
                    Toast.LENGTH_SHORT).show();

            dialog.dismiss();
        });

        dialog.show();
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
                    customYearSelected = true;
                    dialog.dismiss();
                    applyCurrentPeriod();
                })
                .show();
    }

    private String getDefaultMonthKey() {
        List<String> months = getAvailableMonths();
        if (months.isEmpty()) return "";
        String currentMonth = formatMonthKey(Calendar.getInstance());
        return months.contains(currentMonth) ? currentMonth : months.get(months.size() - 1);
    }

    private String getDefaultYearKey() {
        List<String> years = getAvailableYears();
        String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        if (years.isEmpty()) return currentYear;
        return years.contains(currentYear) ? currentYear : years.get(years.size() - 1);
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
        String clean = createdAt.trim();
        Date parsed = parseCreatedAt(clean);
        if (parsed != null) {
            SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            output.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            return output.format(parsed);
        }
        return clean.substring(0, 10);
    }

    private Date parseCreatedAt(String createdAt) {
        String normalized = normalizeIsoDateTime(createdAt);
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat input = new SimpleDateFormat(pattern, Locale.US);
                if (!pattern.endsWith("XXX")) {
                    input.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                return input.parse(normalized);
            } catch (ParseException ignored) {}
        }
        return null;
    }

    private String normalizeIsoDateTime(String value) {
        String clean = value.trim();
        int dotIndex = clean.indexOf('.');
        if (dotIndex >= 0) {
            int zoneIndex = findTimeZoneStart(clean, dotIndex + 1);
            String zone = zoneIndex >= 0 ? clean.substring(zoneIndex) : "";
            clean = clean.substring(0, dotIndex) + zone;
        }
        if (clean.endsWith("Z")) {
            clean = clean.substring(0, clean.length() - 1) + "+00:00";
        }
        return clean;
    }

    private int findTimeZoneStart(String value, int startIndex) {
        for (int i = Math.max(startIndex, 19); i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '+' || c == '-' || c == 'Z') return i;
        }
        return -1;
    }

    private String formatDate(Calendar cal) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private String formatMonthKey(Calendar cal) {
        return String.format(Locale.US, "%04d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1);
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

    private boolean isMatchingRw(String scanRwId, String filterRwId) {
        if (scanRwId == null || filterRwId == null) return false;
        return normalizeRw(scanRwId).equals(normalizeRw(filterRwId));
    }

    private String normalizeRw(String rw) {
        if (rw == null) return "";
        String clean = rw.replace("RW", "").replace("rw", "").trim();
        try {
            return String.valueOf(Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toLowerCase(Locale.US);
        }
    }

    private void hitungStatistik(List<ScanHistory> data) {
        rtCount.clear();
        int total = data.size();
        int organik = 0, anorganik = 0, bukanSampah = 0, recycle = 0;

        for (ScanHistory s : data) {
            String kat = normalizeKategori(s);
            switch (kat) {
                case "organik":       organik++;      break;
                case "anorganik":     anorganik++;    break;
                case "bukan_sampah":  bukanSampah++;  break;
                case "recycle":       recycle++;      break;
            }
            String rt = WilayahUtils.formatRtId(s.getRtId());
            if (!rt.isEmpty()) {
                rtCount.put(rt, rtCount.getOrDefault(rt, 0) + 1);
            }
        }

        if (tvTotalSampah    != null) tvTotalSampah.setText(String.valueOf(total));
        if (tvTotalOrganik   != null) tvTotalOrganik.setText(String.valueOf(organik));
        if (tvTotalAnorganik != null) tvTotalAnorganik.setText(String.valueOf(anorganik));
        if (tvTotalBukanSampah != null) tvTotalBukanSampah.setText(String.valueOf(bukanSampah));
        if (tvTotalRecycle   != null) tvTotalRecycle.setText(String.valueOf(recycle));
        if (tvTotalRtAktif   != null) tvTotalRtAktif.setText(String.valueOf(registeredRtLabels.size()));
    }

    private String normalizeKategori(ScanHistory scan) {
        String kategori = scan != null ? scan.getKategori() : null;
        String clean = kategori == null ? "" : kategori.toLowerCase(Locale.US)
                .replace("_", " ")
                .replace("-", " ")
                .trim();

        if (clean.contains("bukan")) return "bukan_sampah";
        if (clean.contains("anorganik")) return "anorganik";
        if (clean.contains("organik")) return "organik";
        if (clean.contains("recycle") || clean.contains("daur ulang")) return "recycle";

        String jenis = scan != null ? WilayahUtils.normalizeJenis(scan.getJenisSampah()) : "";
        if ("Organik".equalsIgnoreCase(jenis)) return "organik";
        if ("Plastik".equalsIgnoreCase(jenis)) return "anorganik";
        if ("Kardus".equalsIgnoreCase(jenis)
                || "Kaca".equalsIgnoreCase(jenis)
                || "Logam".equalsIgnoreCase(jenis)
                || "Kertas".equalsIgnoreCase(jenis)) {
            return "recycle";
        }
        if ("Bukan Sampah".equalsIgnoreCase(jenis)) return "bukan_sampah";

        return "";
    }

    private void buatGrafik(List<ScanHistory> data) {
        if (barChart == null) return;
        Map<String, Integer> countMap = new HashMap<>();
        String[] labels = {"Organik", "Kardus", "Kaca", "Logam", "Kertas", "Plastik"};
        for (String label : labels) countMap.put(label.toLowerCase(), 0);

        for (ScanHistory s : data) {
            if (s.getJenisSampah() != null) {
                String jenis = WilayahUtils.normalizeJenis(s.getJenisSampah()).toLowerCase(Locale.US);
                if (countMap.containsKey(jenis)) countMap.put(jenis, countMap.get(jenis) + 1);
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        int maxValue = 0;
        for (int i = 0; i < labels.length; i++) {
            int value = countMap.get(labels[i].toLowerCase());
            maxValue = Math.max(maxValue, value);
            entries.add(new BarEntry(i, value));
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
        dataSet.setValueFormatter(compactScanValueFormatter());

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setExtraOffsets(maxValue >= 1000 ? 10f : 6f, 8f, 10f, 4f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#333333"));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#333333"));
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(calculateChartAxisMax(maxValue));
        leftAxis.setLabelCount(5, false);
        leftAxis.setGranularity(maxValue >= 1000 ? 1000f : 1f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setValueFormatter(compactScanValueFormatter());
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(800);
        barChart.invalidate();
    }

    private ValueFormatter compactScanValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatCompactScanCount(value);
            }
        };
    }

    private String formatCompactScanCount(float value) {
        int rounded = Math.round(value);
        if (rounded < 1000) return String.valueOf(rounded);

        float thousands = rounded / 1000f;
        if (rounded % 1000 == 0 || thousands >= 10f) {
            return String.format(Locale.US, "%.0f rb", thousands);
        }
        return String.format(Locale.US, "%.1f rb", thousands).replace(".", ",");
    }

    private float calculateChartAxisMax(int maxValue) {
        if (maxValue <= 5) return 10f;
        if (maxValue <= 10) return 20f;
        if (maxValue <= 20) return 30f;
        if (maxValue <= 50) return 75f;
        if (maxValue <= 100) return 150f;
        if (maxValue < 1000) {
            int step = maxValue <= 500 ? 100 : 250;
            return ((maxValue / step) + 1) * step;
        }

        int step;
        if (maxValue <= 5000) {
            step = 1000;
        } else if (maxValue <= 20000) {
            step = 5000;
        } else {
            step = 10000;
        }
        return ((maxValue / step) + 1) * step;
    }

    private String normalizeJenisKey(String jenisSampah) {
        if (jenisSampah == null) return "";
        String clean = jenisSampah.toLowerCase(Locale.US).replace("_", " ").trim();
        if (clean.contains("organik")) return "organik";
        if (clean.contains("kardus")) return "kardus";
        if (clean.contains("kaca")) return "kaca";
        if (clean.contains("logam") || clean.contains("kaleng")) return "logam";
        if (clean.contains("kertas")) return "kertas";
        if (clean.contains("plastik") || clean.contains("botol")) return "plastik";
        return clean;
    }

    private void buatRanking(List<ScanHistory> data) {
        if (layoutRanking == null) return;
        Map<String, Integer> rtScanCount = new HashMap<>();

        for (String rt : registeredRtLabels) {
            rtScanCount.put(rt, 0);
        }

        for (ScanHistory s : data) {
            String rt = WilayahUtils.formatRtId(s.getRtId());
            if (!rt.isEmpty() && (registeredRtLabels.isEmpty() || registeredRtLabels.contains(rt))) {
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
