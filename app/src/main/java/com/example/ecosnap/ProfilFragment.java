package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.ecosnap.model.User;
import com.example.ecosnap.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilFragment extends Fragment {

    TextView tvAvatarInisial, tvNamaProfil, tvRoleProfil, tvInfoWilayahHeader;
    TextView tvInfoNama, tvInfoNomorHp, tvInfoWilayah, tvInfoRole;
    TextView tvTotalScan, tvJenisTerbanyak;

    MaterialButton btnLogout;
    View rowNama, rowNomorHp, btnRiwayatScanMenu, btnTentangAplikasiMenu, btnBantuanMenu, progressBar;

    FirebaseAuth mAuth;
    DataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profil_user, container, false);

        mAuth = FirebaseAuth.getInstance();
        repository = DataRepository.getInstance();

        initView(view);

        if (rowNama != null) {
            rowNama.setOnClickListener(v -> {
                String currentVal = tvInfoNama != null ? tvInfoNama.getText().toString() : "";
                showEditDialog("Nama", currentVal, tvInfoNama);
            });
        }

        if (rowNomorHp != null) {
            rowNomorHp.setOnClickListener(v -> {
                String currentVal = tvInfoNomorHp != null ? tvInfoNomorHp.getText().toString() : "";
                showEditDialog("Nomor HP", currentVal, tvInfoNomorHp);
            });
        }

        if (btnRiwayatScanMenu != null) {
            btnRiwayatScanMenu.setOnClickListener(v -> {
                if (isAdded() && getActivity() != null) {
                    Intent i = new Intent(getActivity(), com.example.ecosnap.user.HistoryActivity.class);
                    startActivity(i);
                }
            });
        }

        if (btnTentangAplikasiMenu != null) {
            btnTentangAplikasiMenu.setOnClickListener(v -> {
                if (isAdded() && getActivity() != null) {
                    Intent i = new Intent(getActivity(), com.example.ecosnap.user.AboutActivity.class);
                    startActivity(i);
                }
            });
        }

        if (btnBantuanMenu != null) {
            btnBantuanMenu.setOnClickListener(v -> {
                if (isAdded() && getActivity() != null) {
                    Intent i = new Intent(getActivity(), com.example.ecosnap.user.HelpActivity.class);
                    startActivity(i);
                }
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                if (isAdded() && getActivity() != null) {
                    Intent i = new Intent(getActivity(), LoginActivity.class);
                    startActivity(i);
                    getActivity().finishAffinity();
                }
            });
        }

        return view;
    }

    private void showEditDialog(String fieldName, String currentValue, TextView targetTextView) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Ubah " + fieldName);

        final EditText input = new EditText(getContext());
        input.setText(currentValue.equals("-") ? "" : currentValue);
        input.setSingleLine(true);
        if (fieldName.equals("Nomor HP")) {
            input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        } else {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = dp(20);
        lp.setMargins(margin, dp(8), margin, dp(8));
        input.setLayoutParams(lp);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String newValue = input.getText().toString().trim();

            if (newValue.isEmpty()) {
                Toast.makeText(getContext(), fieldName + " tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            if (fieldName.equals("Nomor HP")) {
                if (!newValue.matches("^[0-9]{10,15}$")) {
                    Toast.makeText(getContext(), "Nomor HP harus berupa 10-15 digit angka", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            updateFieldOnSupabase(fieldName, newValue, targetTextView);
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateFieldOnSupabase(String fieldName, String newValue, TextView targetTextView) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        com.example.ecosnap.network.ApiService api =
                com.example.ecosnap.network.RetrofitClient.getClient()
                        .create(com.example.ecosnap.network.ApiService.class);

        Map<String, String> updates = new HashMap<>();
        if (fieldName.equals("Nama")) {
            updates.put("nama", newValue);
        } else if (fieldName.equals("Nomor HP")) {
            updates.put("nomor_hp", newValue);
        }

        Call<Void> call = api.updateUserPatch("eq." + uid, updates);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() || response.code() == 204) {
                    Toast.makeText(getContext(), fieldName + " berhasil diperbarui ✓", Toast.LENGTH_SHORT).show();

                    // Realtime Auto-refresh local UI state
                    targetTextView.setText(newValue);
                    if (fieldName.equals("Nama")) {
                        if (tvNamaProfil != null) tvNamaProfil.setText(newValue);
                        String inisial = newValue.substring(0, 1).toUpperCase();
                        if (tvAvatarInisial != null) tvAvatarInisial.setText(inisial);
                    }
                } else {
                    Toast.makeText(getContext(), "Gagal memperbarui: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dp(int v) {
        if (getContext() == null) return v;
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfil();
        loadStatistik();
    }

    private void initView(View view) {
        tvAvatarInisial = view.findViewById(R.id.tvAvatarInisial);
        tvNamaProfil = view.findViewById(R.id.tvNamaProfil);
        tvRoleProfil = view.findViewById(R.id.tvRoleProfil);
        tvInfoWilayahHeader = view.findViewById(R.id.tvInfoWilayahHeader);

        tvInfoNama = view.findViewById(R.id.tvInfoNama);
        tvInfoNomorHp = view.findViewById(R.id.tvInfoNomorHp);
        tvInfoWilayah = view.findViewById(R.id.tvInfoWilayah);
        tvInfoRole = view.findViewById(R.id.tvInfoRole);

        tvTotalScan = view.findViewById(R.id.tvTotalScan);
        tvJenisTerbanyak = view.findViewById(R.id.tvJenisTerbanyak);

        rowNama = view.findViewById(R.id.rowNama);
        rowNomorHp = view.findViewById(R.id.rowNomorHp);
        btnRiwayatScanMenu = view.findViewById(R.id.btnRiwayatScanMenu);
        btnTentangAplikasiMenu = view.findViewById(R.id.btnTentangAplikasiMenu);
        btnBantuanMenu = view.findViewById(R.id.btnBantuanMenu);
        progressBar = view.findViewById(R.id.progressBar);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void loadProfil() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (isAdded() && getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
            return;
        }

        Call<List<User>> call = repository != null ? repository.getUser() : null;
        if (call == null) return;

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    String nama = safe(user.getNama());
                    String hp = safe(user.getNomorHp());
                    String role = safe(user.getRole());
                    String wilayah = WilayahUtils.formatScanAreaLabel(user.getRwId(), user.getRtId());
                    String headerRole = "user".equalsIgnoreCase(role) ? "Warga" : capitalize(role);

                    String inisial = nama.equals("-") ? "U" : nama.substring(0,1).toUpperCase();

                    if (tvAvatarInisial != null) tvAvatarInisial.setText(inisial);
                    if (tvNamaProfil != null) tvNamaProfil.setText(nama);
                    if (tvRoleProfil != null) tvRoleProfil.setText(headerRole);
                    if (tvInfoWilayahHeader != null) tvInfoWilayahHeader.setText(wilayah.isEmpty() ? "-" : wilayah);

                    if (tvInfoNama != null) tvInfoNama.setText(nama);
                    if (tvInfoNomorHp != null) tvInfoNomorHp.setText(hp);
                    if (tvInfoWilayah != null) tvInfoWilayah.setText(wilayah.isEmpty() ? "-" : wilayah);
                    if (tvInfoRole != null) tvInfoRole.setText(capitalize(role));
                } else {
                    if (isAdded()) Toast.makeText(getContext(), "Data user tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                if (isAdded()) Toast.makeText(getContext(), "Gagal load profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatistik() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        com.example.ecosnap.network.ApiService api =
                com.example.ecosnap.network.RetrofitClient.getClient()
                        .create(com.example.ecosnap.network.ApiService.class);

        api.getScanByUser("eq." + uid).enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanHistory> list = response.body();
                    int total = list.size();
                    if (tvTotalScan != null) tvTotalScan.setText(String.valueOf(total));

                    // Cari jenis sampah terbanyak
                    Map<String, Integer> countMap = new HashMap<>();
                    for (ScanHistory s : list) {
                        String jenis = s.getJenisSampah();
                        if (jenis != null && !jenis.isEmpty()) {
                            countMap.put(jenis, countMap.getOrDefault(jenis, 0) + 1);
                        }
                    }

                    String terbanyak = "-";
                    int maxCount = 0;
                    for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                        if (entry.getValue() > maxCount) {
                            maxCount = entry.getValue();
                            terbanyak = entry.getKey();
                        }
                    }
                    if (tvJenisTerbanyak != null) tvJenisTerbanyak.setText(terbanyak);
                } else {
                    if (tvTotalScan != null) tvTotalScan.setText("0");
                    if (tvJenisTerbanyak != null) tvJenisTerbanyak.setText("-");
                }
            }

            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (!isAdded()) return;
                if (tvTotalScan != null) tvTotalScan.setText("0");
                if (tvJenisTerbanyak != null) tvJenisTerbanyak.setText("-");
            }
        });
    }

    private String safe(String text) {
        return text == null || text.isEmpty() ? "-" : text;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "-";
        return text.substring(0,1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
