package com.example.ecosnap.user;

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

import com.example.ecosnap.R;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.adapter.RiwayatScanAdapter;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvRiwayat;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private TextView tvJumlahRiwayat;
    private TextView tvSubtitle;
    private ImageView btnBack;

    private List<ScanHistory> scanList = new ArrayList<>();
    private RiwayatScanAdapter adapter;

    // Mode: true = lihat semua user (admin view), false = hanya user sendiri
    private boolean isAdminView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_scan);

        rvRiwayat       = findViewById(R.id.rvRiwayat);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        progressBar     = findViewById(R.id.progressBar);
        tvJumlahRiwayat = findViewById(R.id.tvJumlahRiwayat);
        tvSubtitle      = findViewById(R.id.tvSubtitle);
        btnBack         = findViewById(R.id.btnBack);

        // Cek apakah ada flag admin view dari Intent
        isAdminView = getIntent().getBooleanExtra("admin_view", false);

        if (tvSubtitle != null) {
            tvSubtitle.setVisibility(isAdminView ? View.VISIBLE : View.GONE);
            tvSubtitle.setText("Semua pengguna");
        }

        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup RecyclerView — pass showUser=true jika admin
        adapter = new RiwayatScanAdapter(this, scanList, isAdminView);
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));
        rvRiwayat.setAdapter(adapter);

        loadRiwayat();
    }

    private void loadRiwayat() {
        showLoading();
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        if (isAdminView) {
            // Admin: tampilkan semua scan
            api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
                @Override
                public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                    hideLoading();
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        scanList.clear();
                        scanList.addAll(response.body());
                        adapter.notifyDataSetChanged();
                        tvJumlahRiwayat.setText(scanList.size() + " scan");
                        showList();
                    } else {
                        showEmpty();
                    }
                }

                @Override
                public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                    hideLoading();
                    showEmpty();
                    Toast.makeText(HistoryActivity.this,
                            "Gagal memuat riwayat: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // User: hanya scan milik sendiri
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                showEmpty();
                return;
            }

            String uid = currentUser.getUid();
            api.getScanByUserOrdered("eq." + uid).enqueue(new Callback<List<ScanHistory>>() {
                @Override
                public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                    hideLoading();
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        scanList.clear();
                        scanList.addAll(response.body());
                        adapter.notifyDataSetChanged();
                        tvJumlahRiwayat.setText(scanList.size() + " scan");
                        showList();
                    } else {
                        showEmpty();
                    }
                }

                @Override
                public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                    hideLoading();
                    showEmpty();
                    Toast.makeText(HistoryActivity.this,
                            "Gagal memuat riwayat: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (rvRiwayat != null) rvRiwayat.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
    }

    private void hideLoading() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void showList() {
        if (rvRiwayat != null) rvRiwayat.setVisibility(View.VISIBLE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmpty() {
        if (rvRiwayat != null) rvRiwayat.setVisibility(View.GONE);
        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
        if (tvJumlahRiwayat != null) tvJumlahRiwayat.setText("0 scan");
    }
}