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
import com.example.ecosnap.WilayahUtils;
import com.example.ecosnap.adapter.RiwayatScanAdapter;
import com.example.ecosnap.model.User;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final List<ScanHistory> scanList = new ArrayList<>();
    private RiwayatScanAdapter adapter;

    private boolean isAdminView = false;
    private String categoryFilter = "";
    private String filterMode = "";

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

        isAdminView = getIntent().getBooleanExtra("admin_view", false);
        categoryFilter = safe(getIntent().getStringExtra("category_filter"));
        filterMode = safe(getIntent().getStringExtra("filter_mode"));

        if (tvSubtitle != null) {
            boolean showSubtitle = isAdminView || !categoryFilter.isEmpty();
            tvSubtitle.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);
            tvSubtitle.setText(buildSubtitle());
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        adapter = new RiwayatScanAdapter(this, scanList, isAdminView);
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));
        rvRiwayat.setAdapter(adapter);

        loadRiwayat();
    }

    private void loadRiwayat() {
        showLoading();
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        if (isAdminView) {
            loadAdminRwThenScans(api);
        } else {
            loadCurrentUserScans(api);
        }
    }

    private void loadAdminRwThenScans(ApiService api) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            hideLoading();
            showEmpty();
            return;
        }

        api.getUserByFirebaseUid("eq." + currentUser.getUid()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User admin = response.body().get(0);
                    loadAdminScansForRw(api, admin.getRwId());
                } else {
                    hideLoading();
                    showEmpty();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                hideLoading();
                showEmpty();
                Toast.makeText(HistoryActivity.this,
                        "Gagal memuat data admin: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAdminScansForRw(ApiService api, String adminRwId) {
        if (safe(adminRwId).isEmpty()) {
            hideLoading();
            showEmpty();
            return;
        }

        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    applyScanList(response.body(), adminRwId);
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

    private void loadCurrentUserScans(ApiService api) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            hideLoading();
            showEmpty();
            return;
        }

        api.getScanByUserOrdered("eq." + currentUser.getUid()).enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    applyScanList(response.body(), null);
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

    private void applyScanList(List<ScanHistory> source, String rwFilter) {
        scanList.clear();
        Set<String> seenIds = new HashSet<>();

        for (ScanHistory s : source) {
            String id = s.getId();
            if (id != null && !seenIds.add(id)) continue;
            if (rwFilter != null && !WilayahUtils.isMatchingRw(s.getRwId(), rwFilter)) continue;
            if (!matchesCategoryFilter(s)) continue;
            scanList.add(s);
        }

        if (scanList.isEmpty()) {
            showEmpty();
            return;
        }

        sortNewestFirst(scanList);
        adapter.notifyDataSetChanged();
        tvJumlahRiwayat.setText(scanList.size() + " scan");
        showList();
    }

    private boolean matchesCategoryFilter(ScanHistory scan) {
        if (categoryFilter.isEmpty()) return true;
        if ("kategori".equalsIgnoreCase(filterMode)) {
            return categoryFilter.equalsIgnoreCase(safe(scan.getKategori()));
        }
        return categoryFilter.equalsIgnoreCase(WilayahUtils.normalizeJenis(scan.getJenisSampah()));
    }

    private String buildSubtitle() {
        String base = isAdminView ? "Semua warga dalam RT ini" : "Riwayat kamu";
        if (categoryFilter.isEmpty()) return base;
        return base + " - " + categoryFilter;
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

    private void sortNewestFirst(List<ScanHistory> list) {
        list.sort((a, b) -> safe(b.getCreatedAt()).compareTo(safe(a.getCreatedAt())));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
