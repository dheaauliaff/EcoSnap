package com.example.ecosnap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.LinkedHashMap;
import java.util.List;

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
        
        tvTotalSemua = view.findViewById(R.id.tvTotalSemua);
        tvTotalTengah = view.findViewById(R.id.tvTotalTengah);
        donutChart = view.findViewById(R.id.donutChart);
        
        tvStatOrganik = view.findViewById(R.id.tvStatOrganik);
        tvStatKardus = view.findViewById(R.id.tvStatKardus);
        tvStatKaca = view.findViewById(R.id.tvStatKaca);
        tvStatLogam = view.findViewById(R.id.tvStatLogam);
        tvStatKertas = view.findViewById(R.id.tvStatKertas);
        tvStatPlastik = view.findViewById(R.id.tvStatPlastik);
        
        tvRank1RT = view.findViewById(R.id.tvRank1RT);
        tvRank1Total = view.findViewById(R.id.tvRank1Total);
        tvRank2RT = view.findViewById(R.id.tvRank2RT);
        tvRank2Total = view.findViewById(R.id.tvRank2Total);
        tvRank3RT = view.findViewById(R.id.tvRank3RT);
        tvRank3Total = view.findViewById(R.id.tvRank3Total);
        tvRank4RT = view.findViewById(R.id.tvRank4RT);
        tvRank4Total = view.findViewById(R.id.tvRank4Total);
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        SharedPrototypeData data = SharedPrototypeData.getInstance();
        int total = data.getGlobalTotalReports();
        
        if (tvTotalSemua != null) tvTotalSemua.setText("Total " + total);
        if (tvTotalTengah != null) {
            tvTotalTengah.setText(total > 0 ? String.valueOf(total) : "-");
        }
        
        LinkedHashMap<String, Integer> totals = data.getGlobalCategoryTotals();
        
        // Update Chart
        if (donutChart != null) {
            float[] arr = new float[6];
            arr[0] = totals.getOrDefault("Organik", 0);
            arr[1] = totals.getOrDefault("Plastik", 0);
            arr[2] = totals.getOrDefault("Kertas", 0);
            arr[3] = totals.getOrDefault("Kaca", 0);
            arr[4] = totals.getOrDefault("Kardus", 0);
            arr[5] = totals.getOrDefault("Logam", 0);
            donutChart.setValues(arr);
        }
        
        // Update Texts
        updateStatText(tvStatOrganik, "Organik", totals.getOrDefault("Organik", 0), total);
        updateStatText(tvStatKardus, "Kardus", totals.getOrDefault("Kardus", 0), total);
        updateStatText(tvStatKaca, "Kaca", totals.getOrDefault("Kaca", 0), total);
        updateStatText(tvStatLogam, "Logam", totals.getOrDefault("Logam", 0), total);
        updateStatText(tvStatKertas, "Kertas", totals.getOrDefault("Kertas", 0), total);
        updateStatText(tvStatPlastik, "Plastik", totals.getOrDefault("Plastik", 0), total);
        
        // Ranking
        List<SharedPrototypeData.RTStat> ranking = data.getRTRanking();
        if (ranking.size() > 0) {
            updateRankText(tvRank1RT, tvRank1Total, ranking.get(0));
        }
        if (ranking.size() > 1) {
            updateRankText(tvRank2RT, tvRank2Total, ranking.get(1));
        }
        if (ranking.size() > 2) {
            updateRankText(tvRank3RT, tvRank3Total, ranking.get(2));
        }
        if (ranking.size() > 3) {
            updateRankText(tvRank4RT, tvRank4Total, ranking.get(3));
        }
    }
    
    private void updateStatText(TextView tv, String category, int value, int total) {
        if (tv == null) return;
        if (value > 0) {
            int pct = total == 0 ? 0 : Math.round((value * 100f) / total);
            tv.setText(value + " (" + pct + "%)");
        } else {
            tv.setText("-");
        }
    }
    
    private void updateRankText(TextView tvName, TextView tvTotal, SharedPrototypeData.RTStat stat) {
        if (tvName != null) tvName.setText(stat.rtName);
        if (tvTotal != null) tvTotal.setText(stat.total > 0 ? String.valueOf(stat.total) : "-");
    }
}
