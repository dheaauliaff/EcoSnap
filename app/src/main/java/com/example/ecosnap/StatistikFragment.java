package com.example.ecosnap;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatistikFragment extends Fragment {

    private TextView tvTotalSemua, tvTotalTengah;
    private DonutChartView donutChart;
    private TextView tvStatOrganik, tvStatKardus, tvStatKaca, tvStatLogam, tvStatKertas, tvStatPlastik;

    // New ranking views
    private TextView tvRankTotalRtAktif, tvRankTotalScan, tvRankDominan;
    private LinearLayout layoutRankingList;

    private List<ScanHistory> allScanList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_rekap_admin, container, false);

        // Sembunyikan bottom navigation admin (di dalam fragment user sudah ada bottom nav sendiri)
        View bottomNav = view.findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        tvTotalSemua  = view.findViewById(R.id.tvTotalSemua);
        tvTotalTengah = view.findViewById(R.id.tvTotalTengah);
        donutChart    = view.findViewById(R.id.donutChart);

        tvStatOrganik = view.findViewById(R.id.tvStatOrganik);
        tvStatKardus  = view.findViewById(R.id.tvStatKardus);
        tvStatKaca    = view.findViewById(R.id.tvStatKaca);
        tvStatLogam   = view.findViewById(R.id.tvStatLogam);
        tvStatKertas  = view.findViewById(R.id.tvStatKertas);
        tvStatPlastik = view.findViewById(R.id.tvStatPlastik);

        tvRankTotalRtAktif = view.findViewById(R.id.tvRankTotalRtAktif);
        tvRankTotalScan    = view.findViewById(R.id.tvRankTotalScan);
        tvRankDominan      = view.findViewById(R.id.tvRankDominan);
        layoutRankingList  = view.findViewById(R.id.layoutRankingList);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataFromSupabase();
    }

    // ─── Ambil semua scan dari Supabase ──────────────────────────────────────

    private void loadDataFromSupabase() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    allScanList = response.body();
                    processAndDisplay(allScanList);
                } else {
                    showError("Gagal memuat statistik (server)");
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (isAdded()) showError("Gagal terhubung ke server");
            }
        });
    }

    // ─── Proses dan tampilkan ke UI ───────────────────────────────────────────

    private void processAndDisplay(List<ScanHistory> list) {
        int total = list.size();

        Map<String, Integer> namaMap = new HashMap<>();
        Map<String, Integer> rtMap   = new HashMap<>();

        for (ScanHistory s : list) {
            String nama = s.getJenisSampah();
            if (nama != null && !nama.isEmpty())
                namaMap.put(nama, namaMap.getOrDefault(nama, 0) + 1);
            String rt = s.getRtId();
            if (rt != null && !rt.isEmpty())
                rtMap.put(rt, rtMap.getOrDefault(rt, 0) + 1);
        }

        // ── Donut + stat header
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

        updateStatText(tvStatOrganik, "Organik", namaMap.getOrDefault("Organik", 0), total);
        updateStatText(tvStatKardus,  "Kardus",  namaMap.getOrDefault("Kardus",  0), total);
        updateStatText(tvStatKaca,    "Kaca",    namaMap.getOrDefault("Kaca",    0), total);
        updateStatText(tvStatLogam,   "Logam",   namaMap.getOrDefault("Logam",   0), total);
        updateStatText(tvStatKertas,  "Kertas",  namaMap.getOrDefault("Kertas",  0), total);
        updateStatText(tvStatPlastik, "Plastik", namaMap.getOrDefault("Plastik", 0), total);

        // ── Summary card ranking
        if (tvRankTotalRtAktif != null) tvRankTotalRtAktif.setText(String.valueOf(rtMap.size()));
        if (tvRankTotalScan    != null) tvRankTotalScan.setText(String.valueOf(total));
        String dominan = getDominant(namaMap);
        if (tvRankDominan != null) tvRankDominan.setText("Sampah paling banyak: " + dominan);

        // ── Dynamic ranking list (sama persis gaya admin)
        buildRankingList(rtMap);
    }

    // ─── Build dynamic ranking list (gaya admin) ─────────────────────────────

    private void buildRankingList(Map<String, Integer> rtMap) {
        if (layoutRankingList == null || getContext() == null) return;
        layoutRankingList.removeAllViews();

        if (rtMap.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Belum ada data scan yang tercatat.");
            empty.setTextColor(Color.parseColor("#757575"));
            empty.setTextSize(14);
            layoutRankingList.addView(empty);
            return;
        }

        List<Map.Entry<String, Integer>> rtList = new ArrayList<>(rtMap.entrySet());
        rtList.sort((a, b) -> b.getValue() - a.getValue());

        String[] medals = {"🥇", "🥈", "🥉"};

        for (int i = 0; i < rtList.size(); i++) {
            final Map.Entry<String, Integer> entry = rtList.get(i);
            final String rtId = entry.getKey();

            // Row card
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(requireContext().getDrawable(R.drawable.bg_premium_card));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rowParams);
            row.setClickable(true);
            row.setFocusable(true);
            android.util.TypedValue ripple = new android.util.TypedValue();
            requireActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            row.setForeground(requireContext().getDrawable(ripple.resourceId));

            // Rank badge (medal atau angka)
            TextView tvRank = new TextView(getContext());
            tvRank.setText(i < 3 ? medals[i] : (i + 1) + ".");
            tvRank.setTextSize(i < 3 ? 22 : 16);
            LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            rankParams.setMarginEnd(dp(12));
            tvRank.setLayoutParams(rankParams);
            tvRank.setGravity(Gravity.CENTER);

            // Info (nama RT + dominan jenis)
            LinearLayout info = new LinearLayout(getContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNama = new TextView(getContext());
            tvNama.setText(rtId);
            tvNama.setTextColor(Color.parseColor("#1B5E20"));
            tvNama.setTextSize(16);
            tvNama.setTypeface(null, Typeface.BOLD);

            // Hitung jenis dominan per RT
            Map<String, Integer> rtJenisMap = new HashMap<>();
            for (ScanHistory s : allScanList) {
                if (rtId.equals(s.getRtId()) && s.getJenisSampah() != null) {
                    rtJenisMap.put(s.getJenisSampah(),
                            rtJenisMap.getOrDefault(s.getJenisSampah(), 0) + 1);
                }
            }
            String dominanRT = getDominant(rtJenisMap);

            TextView tvDominan = new TextView(getContext());
            tvDominan.setText("Terbanyak: " + dominanRT);
            tvDominan.setTextColor(Color.parseColor("#757575"));
            tvDominan.setTextSize(12);

            info.addView(tvNama);
            info.addView(tvDominan);

            // Total scan
            TextView tvJumlah = new TextView(getContext());
            tvJumlah.setText(entry.getValue() + " scan");
            tvJumlah.setTextColor(Color.parseColor("#2E7D32"));
            tvJumlah.setTextSize(14);
            tvJumlah.setTypeface(null, Typeface.BOLD);

            // Arrow icon
            ImageView arrow = new ImageView(getContext());
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

            // Klik → bottom sheet detail per RT
            final Map<String, Integer> jenisMapFinal = rtJenisMap;
            final int totalRt = entry.getValue();
            row.setOnClickListener(v -> showRtDetailBottomSheet(rtId, jenisMapFinal, totalRt));
        }
    }

    // ─── Bottom sheet detail per RT (sama seperti admin) ─────────────────────

    private void showRtDetailBottomSheet(String rtId, Map<String, Integer> jenisMap, int totalRt) {
        if (!isAdded() || getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(24), dp(24), dp(40));

        // Header
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("Detail Sampah — " + rtId);
        tvTitle.setTextColor(Color.parseColor("#1B5E20"));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        container.addView(tvTitle);

        // Total
        TextView tvTotal = new TextView(getContext());
        tvTotal.setText("Total scan: " + totalRt + " item");
        tvTotal.setTextColor(Color.parseColor("#757575"));
        tvTotal.setTextSize(13);
        LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        totalParams.setMargins(0, dp(4), 0, dp(20));
        tvTotal.setLayoutParams(totalParams);
        container.addView(tvTotal);

        // Sort jenis
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(jenisMap.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        for (Map.Entry<String, Integer> e : sorted) {
            int cnt   = e.getValue();
            int pct   = totalRt == 0 ? 0 : Math.round((cnt * 100f) / totalRt);
            int color = colorForNama(e.getKey());

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(12));
            row.setLayoutParams(rowParams);

            // Label row
            LinearLayout labelRow = new LinearLayout(getContext());
            labelRow.setOrientation(LinearLayout.HORIZONTAL);
            labelRow.setGravity(Gravity.CENTER_VERTICAL);

            // Dot
            FrameLayout dot = new FrameLayout(getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(12), dp(12));
            dotParams.setMarginEnd(dp(10));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(color);
            dot.setBackground(dotBg);

            TextView tvNama = new TextView(getContext());
            tvNama.setText(e.getKey());
            tvNama.setTextColor(Color.parseColor("#212121"));
            tvNama.setTextSize(14);
            tvNama.setTypeface(null, Typeface.BOLD);
            tvNama.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvCnt = new TextView(getContext());
            tvCnt.setText(cnt + " (" + pct + "%)");
            tvCnt.setTextColor(color);
            tvCnt.setTextSize(13);
            tvCnt.setTypeface(null, Typeface.BOLD);

            labelRow.addView(dot);
            labelRow.addView(tvNama);
            labelRow.addView(tvCnt);

            // Progress bar
            LinearLayout progressBg = new LinearLayout(getContext());
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setCornerRadius(dp(4));
            bgShape.setColor(Color.parseColor("#F0F0F0"));
            LinearLayout.LayoutParams progressBgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            progressBgParams.setMargins(0, dp(6), 0, 0);
            progressBg.setLayoutParams(progressBgParams);
            progressBg.setBackground(bgShape);
            progressBg.setWeightSum(100);

            View progressFill = new View(getContext());
            GradientDrawable fillShape = new GradientDrawable();
            fillShape.setCornerRadius(dp(4));
            fillShape.setColor(color);
            progressFill.setLayoutParams(new LinearLayout.LayoutParams(0, dp(8), Math.max(pct, 1)));
            progressFill.setBackground(fillShape);

            View progressEmpty = new View(getContext());
            progressEmpty.setLayoutParams(new LinearLayout.LayoutParams(0, dp(8), Math.max(100 - pct, 0)));

            progressBg.addView(progressFill);
            progressBg.addView(progressEmpty);

            row.addView(labelRow);
            row.addView(progressBg);
            container.addView(row);
        }

        if (sorted.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Belum ada data untuk RT ini");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            container.addView(empty);
        }

        dialog.setContentView(container);
        dialog.show();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void updateStatText(TextView tv, String category, int value, int total) {
        if (tv == null) return;
        if (value > 0) {
            int pct = total == 0 ? 0 : Math.round((value * 100f) / total);
            tv.setText(value + " (" + pct + "%)");
        } else {
            tv.setText("-");
        }
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

    private void showError(String msg) {
        if (isAdded() && getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
