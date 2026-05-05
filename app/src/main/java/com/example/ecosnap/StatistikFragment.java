package com.example.ecosnap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;

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
    private TextView tvRank1RT, tvRank1Total;
    private TextView tvRank2RT, tvRank2Total;
    private TextView tvRank3RT, tvRank3Total;
    private TextView tvRank4RT, tvRank4Total;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_rekap_admin, container, false);

        tvTotalSemua  = view.findViewById(R.id.tvTotalSemua);
        tvTotalTengah = view.findViewById(R.id.tvTotalTengah);
        donutChart    = view.findViewById(R.id.donutChart);

        tvStatOrganik = view.findViewById(R.id.tvStatOrganik);
        tvStatKardus  = view.findViewById(R.id.tvStatKardus);
        tvStatKaca    = view.findViewById(R.id.tvStatKaca);
        tvStatLogam   = view.findViewById(R.id.tvStatLogam);
        tvStatKertas  = view.findViewById(R.id.tvStatKertas);
        tvStatPlastik = view.findViewById(R.id.tvStatPlastik);

        tvRank1RT    = view.findViewById(R.id.tvRank1RT);
        tvRank1Total = view.findViewById(R.id.tvRank1Total);
        tvRank2RT    = view.findViewById(R.id.tvRank2RT);
        tvRank2Total = view.findViewById(R.id.tvRank2Total);
        tvRank3RT    = view.findViewById(R.id.tvRank3RT);
        tvRank3Total = view.findViewById(R.id.tvRank3Total);
        tvRank4RT    = view.findViewById(R.id.tvRank4RT);
        tvRank4Total = view.findViewById(R.id.tvRank4Total);

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
                    processAndDisplay(response.body());
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

    // ─── Proses data Supabase dan tampilkan ke UI ─────────────────────────────

    private void processAndDisplay(List<ScanHistory> list) {
        int total = list.size();

        // Hitung per jenis sampah (nama_sampah) dan per RT
        Map<String, Integer> namaMap = new HashMap<>();
        Map<String, Integer> rtMap   = new HashMap<>();

        for (ScanHistory s : list) {
            // Jenis sampah (nama_sampah di Supabase)
            String nama = s.getJenisSampah();
            if (nama != null && !nama.isEmpty()) {
                namaMap.put(nama, namaMap.getOrDefault(nama, 0) + 1);
            }
            // Ranking per RT
            String rt = s.getRtId();
            if (rt != null && !rt.isEmpty()) {
                rtMap.put(rt, rtMap.getOrDefault(rt, 0) + 1);
            }
        }

        // Total
        if (tvTotalSemua  != null) tvTotalSemua.setText("Total " + total);
        if (tvTotalTengah != null) tvTotalTengah.setText(total > 0 ? String.valueOf(total) : "-");

        // Donut chart (urutan: Organik, Plastik, Kertas, Kaca, Kardus, Logam)
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
        updateStatText(tvStatOrganik, "Organik", namaMap.getOrDefault("Organik", 0), total);
        updateStatText(tvStatKardus,  "Kardus",  namaMap.getOrDefault("Kardus",  0), total);
        updateStatText(tvStatKaca,    "Kaca",    namaMap.getOrDefault("Kaca",    0), total);
        updateStatText(tvStatLogam,   "Logam",   namaMap.getOrDefault("Logam",   0), total);
        updateStatText(tvStatKertas,  "Kertas",  namaMap.getOrDefault("Kertas",  0), total);
        updateStatText(tvStatPlastik, "Plastik", namaMap.getOrDefault("Plastik", 0), total);

        // Ranking RT (urutkan descending)
        List<Map.Entry<String, Integer>> rtList = new ArrayList<>(rtMap.entrySet());
        rtList.sort((a, b) -> b.getValue() - a.getValue());

        updateRankRow(tvRank1RT, tvRank1Total, rtList, 0);
        updateRankRow(tvRank2RT, tvRank2Total, rtList, 1);
        updateRankRow(tvRank3RT, tvRank3Total, rtList, 2);
        updateRankRow(tvRank4RT, tvRank4Total, rtList, 3);
    }

    // ─── Helper UI ───────────────────────────────────────────────────────────

    private void updateStatText(TextView tv, String category, int value, int total) {
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
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
