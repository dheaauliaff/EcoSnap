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

    MaterialButton btnEditProfil, btnLogout;

    FirebaseAuth mAuth;
    DataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profil_user, container, false);

        mAuth = FirebaseAuth.getInstance();
        repository = DataRepository.getInstance();

        initView(view);

        if (btnEditProfil != null) btnEditProfil.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                Intent i = new Intent(getActivity(), com.example.ecosnap.user.EditProfilActivity.class);
                // Kirim data existing sebagai pre-fill
                i.putExtra("nama",     tvInfoNama    != null ? tvInfoNama.getText().toString()    : "");
                i.putExtra("nomor_hp", tvInfoNomorHp != null ? tvInfoNomorHp.getText().toString() : "");
                i.putExtra("rt_id",    tvInfoWilayah != null ? tvInfoWilayah.getText().toString() : "");
                startActivityForResult(i, 1001);
            }
        });

        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            if (isAdded() && getActivity() != null) {
                Intent i = new Intent(getActivity(), LoginActivity.class);
                startActivity(i);
                getActivity().finishAffinity();
            }
        });

        return view;
    }

    // Refresh profil setelah edit berhasil
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            loadProfil();   // reload data terbaru dari Supabase
            loadStatistik();
        }
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

        btnEditProfil = view.findViewById(R.id.btnEditProfil);
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
                    String wilayah = safe(user.getWilayah());
                    String role = safe(user.getRole());
                    String rw = safe(user.getRwId());
                    String rt = safe(user.getRtId());

                    String inisial = nama.equals("-") ? "U" : nama.substring(0,1).toUpperCase();

                    if (tvAvatarInisial != null) tvAvatarInisial.setText(inisial);
                    if (tvNamaProfil != null) tvNamaProfil.setText(nama);
                    if (tvRoleProfil != null) tvRoleProfil.setText(capitalize(role));
                    if (tvInfoWilayahHeader != null) tvInfoWilayahHeader.setText(rw + " / " + rt);

                    if (tvInfoNama != null) tvInfoNama.setText(nama);
                    if (tvInfoNomorHp != null) tvInfoNomorHp.setText(hp);
                    if (tvInfoWilayah != null) tvInfoWilayah.setText(wilayah);
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
