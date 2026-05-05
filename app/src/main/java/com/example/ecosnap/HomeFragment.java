package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecosnap.adapter.RiwayatScanAdapter;
import com.example.ecosnap.model.User;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.user.HistoryActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    FirebaseAuth mAuth;
    DataRepository repository;
    TextView tvNamaUser, tvWilayahUser, tvScanHariIni,
            tvScanMingguIni, tvJenisTerakhir, tvKategoriTerakhir,
            tvWaktuTerakhir, tvQuote;
    TextView tvHomeOrganik, tvHomeKardus, tvHomeKaca, tvHomeLogam, tvHomeKertas, tvHomePlastik;
    AppCompatButton btnScanCepat;

    // Scan Terakhir (RecyclerView)
    RecyclerView rvScanTerakhir;
    TextView tvEmptyRiwayat, tvLihatSemua;
    RiwayatScanAdapter scanAdapter;
    List<ScanHistory> recentScans = new ArrayList<>();

    String[] quotes = {
            "\"Memilah sampah hari ini, menyelamatkan bumi 🌍\"",
            "\"Sampah organik bisa jadi kompos ♻️\"",
            "\"Lingkungan bersih dimulai dari kita 🌱\"",
            "\"Pilah sampahmu 💚\"",
            "\"Setiap sampah berarti 🌿\""
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_dashboard_user, container, false);

        mAuth = FirebaseAuth.getInstance();
        repository = DataRepository.getInstance();

        tvNamaUser = view.findViewById(R.id.tvNamaUser);
        tvWilayahUser = view.findViewById(R.id.tvWilayahUser);
        tvScanHariIni = view.findViewById(R.id.tvScanHariIni);
        tvScanMingguIni = view.findViewById(R.id.tvScanMingguIni);
        tvJenisTerakhir = view.findViewById(R.id.tvJenisTerakhir);
        tvKategoriTerakhir = view.findViewById(R.id.tvKategoriTerakhir);
        tvWaktuTerakhir = view.findViewById(R.id.tvWaktuTerakhir);
        tvQuote = view.findViewById(R.id.tvQuote);
        btnScanCepat = view.findViewById(R.id.btnScanCepat);
        
        tvHomeOrganik = view.findViewById(R.id.tvHomeOrganik);
        tvHomeKardus = view.findViewById(R.id.tvHomeKardus);
        tvHomeKaca = view.findViewById(R.id.tvHomeKaca);
        tvHomeLogam = view.findViewById(R.id.tvHomeLogam);
        tvHomeKertas = view.findViewById(R.id.tvHomeKertas);
        tvHomePlastik = view.findViewById(R.id.tvHomePlastik);

        // Scan Terakhir views
        rvScanTerakhir = view.findViewById(R.id.rvScanTerakhir);
        tvEmptyRiwayat = view.findViewById(R.id.tvEmptyRiwayat);
        tvLihatSemua = view.findViewById(R.id.tvLihatSemua);

        // Setup RecyclerView
        scanAdapter = new RiwayatScanAdapter(requireContext(), recentScans);
        rvScanTerakhir.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvScanTerakhir.setAdapter(scanAdapter);

        int randomIndex = (int) (Math.random() * quotes.length);
        if (tvQuote != null) tvQuote.setText(quotes[randomIndex]);

        if (btnScanCepat != null) {
            btnScanCepat.setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.bottomNav);
                    if (nav != null) nav.setSelectedItemId(R.id.nav_scan);
                }
            });
        }

        // Lihat Semua → buka HistoryActivity
        if (tvLihatSemua != null) {
            tvLihatSemua.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), HistoryActivity.class));
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataUser();
    }
    
    // Ambil data kategori nyata dari Supabase berdasarkan scan user yang login
    private void loadCategoryStats(String uid) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getScanByUser("eq." + uid).enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                java.util.Map<String, Integer> counts = new java.util.HashMap<>();
                for (ScanHistory s : response.body()) {
                    String nama = s.getJenisSampah(); // nama_sampah dari Supabase
                    if (nama != null) counts.put(nama, counts.getOrDefault(nama, 0) + 1);
                }
                if (tvHomeOrganik != null) tvHomeOrganik.setText(counts.getOrDefault("Organik", 0) > 0 ? String.valueOf(counts.get("Organik")) : "-");
                if (tvHomeKardus  != null) tvHomeKardus.setText(counts.getOrDefault("Kardus",  0) > 0 ? String.valueOf(counts.get("Kardus"))  : "-");
                if (tvHomeKaca   != null) tvHomeKaca.setText(counts.getOrDefault("Kaca",    0) > 0 ? String.valueOf(counts.get("Kaca"))    : "-");
                if (tvHomeLogam  != null) tvHomeLogam.setText(counts.getOrDefault("Logam",   0) > 0 ? String.valueOf(counts.get("Logam"))   : "-");
                if (tvHomeKertas != null) tvHomeKertas.setText(counts.getOrDefault("Kertas",  0) > 0 ? String.valueOf(counts.get("Kertas"))  : "-");
                if (tvHomePlastik!= null) tvHomePlastik.setText(counts.getOrDefault("Plastik", 0) > 0 ? String.valueOf(counts.get("Plastik")) : "-");
            }
            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {}
        });
    }

    private void loadDataUser() {
        if (mAuth.getCurrentUser() == null || repository == null) return;
        String uid = repository.getCurrentUserId();
        Call<List<User>> call = repository.getUser();
        if (call == null) return;
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    if (tvNamaUser    != null) tvNamaUser.setText(user.getNama());
                    if (tvWilayahUser != null) tvWilayahUser.setText(user.getRtId() + " - " + user.getRwId());
                    loadStatistikScan(uid);
                    loadCategoryStats(uid);
                    loadRecentScans(uid);
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (isAdded()) Toast.makeText(getContext(), "Gagal load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatistikScan(String uid) {
        Call<List<ScanHistory>> call = repository.getStats();
        if (call == null) return;
        call.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> data = response.body();
                    int hariIni = 0; int mingguIni = 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Calendar sekarang = Calendar.getInstance();
                    for (ScanHistory s : data) {
                        try {
                            if (s.getCreatedAt() == null) continue;
                            Date tanggal = sdf.parse(s.getCreatedAt());
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(tanggal);
                            if (cal.get(Calendar.DAY_OF_YEAR) == sekarang.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == sekarang.get(Calendar.YEAR)) hariIni++;
                            if (cal.get(Calendar.WEEK_OF_YEAR) == sekarang.get(Calendar.WEEK_OF_YEAR) && cal.get(Calendar.YEAR) == sekarang.get(Calendar.YEAR)) mingguIni++;
                        } catch (Exception ignored) {}
                    }
                    if (tvScanHariIni != null) tvScanHariIni.setText(String.valueOf(hariIni));
                    if (tvScanMingguIni != null) tvScanMingguIni.setText(String.valueOf(mingguIni));
                }
            }
            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {}
        });
    }

    // ─── Load 5 scan terbaru dari Supabase (sama seperti Riwayat Scan) ────────
    private void loadRecentScans(String uid) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getScanByUserOrdered("eq." + uid).enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> allScans = response.body();

                    recentScans.clear();
                    // Tampilkan max 5 scan terbaru
                    int limit = Math.min(allScans.size(), 5);
                    for (int i = 0; i < limit; i++) {
                        recentScans.add(allScans.get(i));
                    }
                    scanAdapter.notifyDataSetChanged();

                    // Empty state
                    if (recentScans.isEmpty()) {
                        rvScanTerakhir.setVisibility(View.GONE);
                        tvEmptyRiwayat.setVisibility(View.VISIBLE);
                    } else {
                        rvScanTerakhir.setVisibility(View.VISIBLE);
                        tvEmptyRiwayat.setVisibility(View.GONE);
                    }

                    // Update hidden last scan fields (backwards compat)
                    if (!allScans.isEmpty()) {
                        ScanHistory last = allScans.get(0);
                        if (tvJenisTerakhir != null) tvJenisTerakhir.setText(last.getJenisSampah());
                        if (tvKategoriTerakhir != null) tvKategoriTerakhir.setText(last.getKategori());
                        if (tvWaktuTerakhir != null) tvWaktuTerakhir.setText(last.getCreatedAt());
                    }
                } else {
                    rvScanTerakhir.setVisibility(View.GONE);
                    tvEmptyRiwayat.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (isAdded()) {
                    rvScanTerakhir.setVisibility(View.GONE);
                    tvEmptyRiwayat.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
