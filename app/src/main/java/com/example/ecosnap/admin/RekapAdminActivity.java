package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.ecosnap.DonutChartView;
import com.example.ecosnap.R;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RekapAdminActivity extends AppCompatActivity {

    private TextView tvTotalSemua, tvTotalTengah;
    private DonutChartView donutChart;
    private TextView tvStatOrganik, tvStatKardus, tvStatKaca, tvStatLogam, tvStatKertas, tvStatPlastik;
    private TextView tvRankTotalRtAktif, tvRankTotalScan, tvRankDominan;
    private LinearLayout layoutRankingList;
    private BottomNavigationView bottomNav;

    // Section riwayat scan (semua user)
    private LinearLayout layoutRiwayatAll;
    private ProgressBar pbRiwayat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekap_admin);

        bottomNav = findViewById(R.id.bottomNav);

        tvTotalSemua  = findViewById(R.id.tvTotalSemua);
        tvTotalTengah = findViewById(R.id.tvTotalTengah);
        donutChart    = findViewById(R.id.donutChart);

        tvStatOrganik = findViewById(R.id.tvStatOrganik);
        tvStatKardus  = findViewById(R.id.tvStatKardus);
        tvStatKaca    = findViewById(R.id.tvStatKaca);
        tvStatLogam   = findViewById(R.id.tvStatLogam);
        tvStatKertas  = findViewById(R.id.tvStatKertas);
        tvStatPlastik = findViewById(R.id.tvStatPlastik);

        tvRankTotalRtAktif = findViewById(R.id.tvRankTotalRtAktif);
        tvRankTotalScan    = findViewById(R.id.tvRankTotalScan);
        tvRankDominan      = findViewById(R.id.tvRankDominan);
        layoutRankingList  = findViewById(R.id.layoutRankingList);

        layoutRiwayatAll = findViewById(R.id.layoutRiwayatAll);
        pbRiwayat        = findViewById(R.id.pbRiwayat);

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
                    showError("Gagal memuat statistik (server)");
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (!isFinishing()) showError("Gagal terhubung ke server");
            }
        });
    }

    private void processAndDisplay(List<ScanHistory> list) {
        int total = list.size();

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

        if (tvTotalSemua  != null) tvTotalSemua.setText("Total " + total);
        if (tvTotalTengah != null) tvTotalTengah.setText(total > 0 ? String.valueOf(total) : "-");

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

        updateStatText(tvStatOrganik, namaMap.getOrDefault("Organik", 0), total);
        updateStatText(tvStatKardus,  namaMap.getOrDefault("Kardus",  0), total);
        updateStatText(tvStatKaca,    namaMap.getOrDefault("Kaca",    0), total);
        updateStatText(tvStatLogam,   namaMap.getOrDefault("Logam",   0), total);
        updateStatText(tvStatKertas,  namaMap.getOrDefault("Kertas",  0), total);
        updateStatText(tvStatPlastik, namaMap.getOrDefault("Plastik", 0), total);

        // ── Summary card ranking
        if (tvRankTotalRtAktif != null) tvRankTotalRtAktif.setText(String.valueOf(rtMap.size()));
        if (tvRankTotalScan    != null) tvRankTotalScan.setText(String.valueOf(total));
        String dominan = getDominant(namaMap);
        if (tvRankDominan != null) tvRankDominan.setText("Sampah paling banyak: " + dominan);

        buildRankingList(list, rtMap);

        // Tampilkan riwayat scan semua user dengan foto
        buildRiwayatAllUsers(list);
    }

    /**
     * Membangun daftar riwayat scan semua user (admin bisa lihat gambar)
     */
    private void buildRiwayatAllUsers(List<ScanHistory> list) {
        if (layoutRiwayatAll == null) return;
        if (pbRiwayat != null) pbRiwayat.setVisibility(View.GONE);
        layoutRiwayatAll.removeAllViews();

        // Ambil 20 scan terbaru
        List<ScanHistory> recent = list.size() > 20 ? list.subList(0, 20) : list;

        for (ScanHistory scan : recent) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackground(getDrawable(R.drawable.bg_premium_card));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(cardParams);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // === Thumbnail gambar ===
            ImageView img = new ImageView(this);
            int imgSize = dp(72);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(imgSize, imgSize);
            imgParams.setMarginEnd(dp(12));
            img.setLayoutParams(imgParams);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);

            String imageUrl = scan.getFotoUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .transform(new CenterCrop(), new RoundedCorners(dp(10)))
                        .placeholder(R.drawable.ic_scan)
                        .error(R.drawable.ic_scan)
                        .into(img);
            } else {
                img.setImageResource(R.drawable.ic_scan);
            }
            card.addView(img);

            // === Info kanan ===
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            // Nama sampah
            TextView tvNama = new TextView(this);
            tvNama.setText(safe(scan.getJenisSampah()));
            tvNama.setTextColor(Color.parseColor("#212121"));
            tvNama.setTextSize(15f);
            tvNama.setTypeface(null, Typeface.BOLD);
            tvNama.setMaxLines(1);
            info.addView(tvNama);

            // Kategori badge
            TextView tvKat = new TextView(this);
            tvKat.setText(safe(scan.getKategori()));
            tvKat.setTextSize(11f);
            tvKat.setTypeface(null, Typeface.BOLD);
            tvKat.setTextColor(getKategoriTextColor(scan.getKategori()));
            // Set background dan tint secara aman
            android.graphics.drawable.GradientDrawable katBg = new android.graphics.drawable.GradientDrawable();
            katBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            katBg.setCornerRadius(dp(20));
            katBg.setColor(getKategoriColor(scan.getKategori()));
            tvKat.setBackground(katBg);
            tvKat.setPadding(dp(8), dp(2), dp(8), dp(2));
            LinearLayout.LayoutParams katParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            katParams.setMargins(0, dp(4), 0, dp(4));
            tvKat.setLayoutParams(katParams);
            info.addView(tvKat);

            // Wilayah RT/RW
            String wilayah = "";
            if (scan.getRwId() != null) wilayah = scan.getRwId();
            if (scan.getRtId() != null && !scan.getRtId().isEmpty())
                wilayah += (wilayah.isEmpty() ? "" : " / ") + scan.getRtId();
            TextView tvWilayah = new TextView(this);
            tvWilayah.setText("📍 " + (wilayah.isEmpty() ? "-" : wilayah));
            tvWilayah.setTextColor(Color.parseColor("#9E9E9E"));
            tvWilayah.setTextSize(11f);
            info.addView(tvWilayah);

            // Tanggal
            TextView tvTanggal = new TextView(this);
            tvTanggal.setText("🕐 " + formatDate(scan.getCreatedAt()));
            tvTanggal.setTextColor(Color.parseColor("#BDBDBD"));
            tvTanggal.setTextSize(11f);
            info.addView(tvTanggal);

            card.addView(info);

            // === Confidence ===
            LinearLayout confLayout = new LinearLayout(this);
            confLayout.setOrientation(LinearLayout.VERTICAL);
            confLayout.setGravity(android.view.Gravity.CENTER);

            TextView tvConf = new TextView(this);
            Float conf = scan.getAkurasi();
            tvConf.setText(conf != null && conf > 0 ?
                    String.format(Locale.US, "%.0f%%", conf) : "-");
            tvConf.setTextColor(Color.parseColor("#4CAF50"));
            tvConf.setTextSize(15f);
            tvConf.setTypeface(null, Typeface.BOLD);
            tvConf.setGravity(android.view.Gravity.CENTER);

            TextView tvAkurasi = new TextView(this);
            tvAkurasi.setText("akurasi");
            tvAkurasi.setTextColor(Color.parseColor("#BDBDBD"));
            tvAkurasi.setTextSize(10f);
            tvAkurasi.setGravity(android.view.Gravity.CENTER);

            confLayout.addView(tvConf);
            confLayout.addView(tvAkurasi);
            card.addView(confLayout);

            layoutRiwayatAll.addView(card);
        }

        if (recent.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada riwayat scan");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, dp(24), 0, dp(24));
            layoutRiwayatAll.addView(empty);
        }
    }

    private void buildRankingList(List<ScanHistory> allList, Map<String, Integer> rtMap) {
        if (layoutRankingList == null) return;
        layoutRankingList.removeAllViews();
        if (rtMap.isEmpty()) return;

        List<Map.Entry<String, Integer>> rtSorted = new ArrayList<>(rtMap.entrySet());
        rtSorted.sort((a, b) -> b.getValue() - a.getValue());

        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};

        for (int i = 0; i < rtSorted.size(); i++) {
            Map.Entry<String, Integer> entry = rtSorted.get(i);
            String rtId = entry.getKey();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackground(getDrawable(R.drawable.bg_premium_card));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rp);

            // Rank badge
            android.widget.TextView tvRank = new android.widget.TextView(this);
            tvRank.setText(i < 3 ? medals[i] : (i + 1) + ".");
            tvRank.setTextSize(i < 3 ? 22 : 16);
            LinearLayout.LayoutParams rankP = new LinearLayout.LayoutParams(dp(44), dp(44));
            rankP.setMarginEnd(dp(12));
            tvRank.setLayoutParams(rankP);
            tvRank.setGravity(android.view.Gravity.CENTER);

            // Info
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            android.widget.TextView tvNama = new android.widget.TextView(this);
            tvNama.setText(rtId);
            tvNama.setTextColor(Color.parseColor("#1B5E20"));
            tvNama.setTextSize(16);
            tvNama.setTypeface(null, Typeface.BOLD);

            Map<String, Integer> rtJenis = new HashMap<>();
            for (ScanHistory s : allList) {
                if (rtId.equals(s.getRtId()) && s.getJenisSampah() != null)
                    rtJenis.put(s.getJenisSampah(), rtJenis.getOrDefault(s.getJenisSampah(), 0) + 1);
            }

            android.widget.TextView tvDom = new android.widget.TextView(this);
            tvDom.setText("Terbanyak: " + getDominant(rtJenis));
            tvDom.setTextColor(Color.parseColor("#757575"));
            tvDom.setTextSize(12);

            info.addView(tvNama);
            info.addView(tvDom);

            android.widget.TextView tvJumlah = new android.widget.TextView(this);
            tvJumlah.setText(entry.getValue() + " scan");
            tvJumlah.setTextColor(Color.parseColor("#2E7D32"));
            tvJumlah.setTextSize(14);
            tvJumlah.setTypeface(null, Typeface.BOLD);

            android.widget.ImageView arrow = new android.widget.ImageView(this);
            arrow.setImageResource(R.drawable.ic_back);
            arrow.setRotation(180f);
            LinearLayout.LayoutParams arrowP = new LinearLayout.LayoutParams(dp(16), dp(16));
            arrowP.setMarginStart(dp(8));
            arrow.setLayoutParams(arrowP);
            arrow.setColorFilter(Color.parseColor("#BDBDBD"));

            row.addView(tvRank);
            row.addView(info);
            row.addView(tvJumlah);
            row.addView(arrow);
            layoutRankingList.addView(row);
        }
    }

    private String getDominant(Map<String, Integer> map) {
        String dom = "-"; int max = 0;
        for (Map.Entry<String, Integer> e : map.entrySet())
            if (e.getValue() > max) { max = e.getValue(); dom = e.getKey(); }
        return dom;
    }

    private void updateStatText(TextView tv, int value, int total) {
        if (tv == null) return;
        if (value > 0) {
            int pct = total == 0 ? 0 : Math.round((value * 100f) / total);
            tv.setText(value + " (" + pct + "%)");
        } else {
            tv.setText("-");
        }
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "-";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            input.setTimeZone(TimeZone.getTimeZone("UTC"));
            String clean = isoDate;
            if (clean.contains(".")) clean = clean.substring(0, clean.indexOf('.'));
            if (clean.contains("+")) clean = clean.substring(0, clean.indexOf('+'));
            Date date = input.parse(clean);
            if (date == null) return isoDate;
            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy, HH:mm",
                    new Locale("id", "ID"));
            output.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            return output.format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    private int getKategoriColor(String kategori) {
        if (kategori == null) return 0xFFE8F5E9;
        switch (kategori.toLowerCase()) {
            case "organik":      return 0xFFE8F5E9;
            case "anorganik":    return 0xFFFFF3E0;
            case "recycle":      return 0xFFE3F2FD;
            case "bukan sampah": return 0xFFFFEBEE;
            default:             return 0xFFF5F5F5;
        }
    }

    private int getKategoriTextColor(String kategori) {
        if (kategori == null) return 0xFF2E7D32;
        switch (kategori.toLowerCase()) {
            case "organik":      return 0xFF2E7D32;
            case "anorganik":    return 0xFFE65100;
            case "recycle":      return 0xFF1565C0;
            case "bukan sampah": return 0xFFC62828;
            default:             return 0xFF616161;
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
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
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(RekapAdminActivity.this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }
}