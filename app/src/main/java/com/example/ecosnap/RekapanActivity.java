package com.example.ecosnap;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RekapanActivity extends AppCompatActivity {

    TextView btnMingguan, btnBulanan, btnTahunan;
    TextView tvTotal, tvOrganik, tvAnorganik;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekap_admin);

        btnMingguan = findViewById(R.id.btnMingguan);
        btnBulanan = findViewById(R.id.btnBulanan);
        btnTahunan = findViewById(R.id.btnTahunan);

        tvTotal = findViewById(R.id.tvTotal);
        tvOrganik = findViewById(R.id.tvOrganik);
        tvAnorganik = findViewById(R.id.tvAnorganik);

        // DEFAULT BULANAN
        setData("bulanan");

        btnMingguan.setOnClickListener(v -> {
            setData("mingguan");
            setActive(btnMingguan);
        });

        btnBulanan.setOnClickListener(v -> {
            setData("bulanan");
            setActive(btnBulanan);
        });

        btnTahunan.setOnClickListener(v -> {
            setData("tahunan");
            setActive(btnTahunan);
        });
    }

    private void setData(String mode) {

        if (mode.equals("mingguan")) {
            tvTotal.setText("40\nTotal");
            tvOrganik.setText("25\nOrganik");
            tvAnorganik.setText("15\nAnorganik");
        }

        else if (mode.equals("bulanan")) {
            tvTotal.setText("120\nTotal");
            tvOrganik.setText("70\nOrganik");
            tvAnorganik.setText("50\nAnorganik");
        }

        else {
            tvTotal.setText("1000\nTotal");
            tvOrganik.setText("600\nOrganik");
            tvAnorganik.setText("400\nAnorganik");
        }
    }

    private void setActive(TextView selected) {

        btnMingguan.setBackgroundResource(R.drawable.bg_filter_off);
        btnBulanan.setBackgroundResource(R.drawable.bg_filter_off);
        btnTahunan.setBackgroundResource(R.drawable.bg_filter_off);

        btnMingguan.setTextColor(Color.parseColor("#2E7D32"));
        btnBulanan.setTextColor(Color.parseColor("#2E7D32"));
        btnTahunan.setTextColor(Color.parseColor("#2E7D32"));

        selected.setBackgroundResource(R.drawable.bg_filter_on);
        selected.setTextColor(Color.WHITE);
    }
}