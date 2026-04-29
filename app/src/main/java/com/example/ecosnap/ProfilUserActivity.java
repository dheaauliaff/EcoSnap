package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilUserActivity extends AppCompatActivity {

    TextView tvAvatarInisial, tvNamaProfil, tvRoleProfil, tvInfoWilayahHeader;
    TextView tvInfoNama, tvInfoNomorHp, tvInfoWilayah, tvInfoRole;
    TextView tvTotalScan, tvJenisTerbanyak;

    MaterialButton btnEditProfil, btnLogout;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil_user);

        mAuth = FirebaseAuth.getInstance();

        initView();
        loadProfil();
        loadStatistik();

        btnEditProfil.setOnClickListener(v ->
                Toast.makeText(this,
                        "Fitur edit profil coming soon!",
                        Toast.LENGTH_SHORT).show());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            Intent i = new Intent(
                    ProfilUserActivity.this,
                    LoginActivity.class
            );

            startActivity(i);
            finishAffinity();
        });
    }

    private void initView() {

        tvAvatarInisial = findViewById(R.id.tvAvatarInisial);
        tvNamaProfil = findViewById(R.id.tvNamaProfil);
        tvRoleProfil = findViewById(R.id.tvRoleProfil);
        tvInfoWilayahHeader = findViewById(R.id.tvInfoWilayahHeader);

        tvInfoNama = findViewById(R.id.tvInfoNama);
        tvInfoNomorHp = findViewById(R.id.tvInfoNomorHp);
        tvInfoWilayah = findViewById(R.id.tvInfoWilayah);
        tvInfoRole = findViewById(R.id.tvInfoRole);

        tvTotalScan = findViewById(R.id.tvTotalScan);
        tvJenisTerbanyak = findViewById(R.id.tvJenisTerbanyak);

        btnEditProfil = findViewById(R.id.btnEditProfil);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadProfil() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        ApiService api =
                RetrofitClient.getClient().create(ApiService.class);

        Call<List<User>> call =
                api.getUserByFirebaseUid(
                        "eq." + currentUser.getUid()
                );

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call,
                                   Response<List<User>> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    User user = response.body().get(0);

                    String nama = safe(user.getNama());
                    String hp = safe(user.getNomorHp());
                    String wilayah = safe(user.getWilayah());
                    String role = safe(user.getRole());

                    String rw = safe(user.getRwId());
                    String rt = safe(user.getRtId());

                    String inisial = nama.equals("-")
                            ? "U"
                            : nama.substring(0,1).toUpperCase();

                    tvAvatarInisial.setText(inisial);
                    tvNamaProfil.setText(nama);
                    tvRoleProfil.setText(capitalize(role));
                    tvInfoWilayahHeader.setText(rw + " / " + rt);

                    tvInfoNama.setText(nama);
                    tvInfoNomorHp.setText(hp);
                    tvInfoWilayah.setText(wilayah);
                    tvInfoRole.setText(capitalize(role));

                } else {
                    Toast.makeText(
                            ProfilUserActivity.this,
                            "Data user tidak ditemukan",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call,
                                  Throwable t) {

                Toast.makeText(
                        ProfilUserActivity.this,
                        "Gagal load profil",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void loadStatistik() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) return;

        ApiService api =
                RetrofitClient.getClient().create(ApiService.class);

        Call<List<ScanHistory>> call =
                api.getScanByUser(
                        "eq." + currentUser.getUid()
                );

        call.enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(
                    Call<List<ScanHistory>> call,
                    Response<List<ScanHistory>> response) {

                if (response.isSuccessful()
                        && response.body() != null) {

                    List<ScanHistory> list = response.body();

                    int total = list.size();

                    tvTotalScan.setText(
                            "📸 Total Scan : " + total
                    );

                    HashMap<String,Integer> map =
                            new HashMap<>();

                    for (ScanHistory item : list) {

                        String jenis =
                                item.getJenisSampah();

                        if (jenis == null) continue;

                        if (!map.containsKey(jenis)) {
                            map.put(jenis,1);
                        } else {
                            map.put(
                                    jenis,
                                    map.get(jenis)+1
                            );
                        }
                    }

                    String terbanyak = "-";
                    int max = 0;

                    for (Map.Entry<String,Integer> e :
                            map.entrySet()) {

                        if (e.getValue() > max) {
                            max = e.getValue();
                            terbanyak = e.getKey();
                        }
                    }

                    tvJenisTerbanyak.setText(
                            "♻️ Jenis Terbanyak : "
                                    + terbanyak
                    );
                }
            }

            @Override
            public void onFailure(
                    Call<List<ScanHistory>> call,
                    Throwable t) {

                tvTotalScan.setText(
                        "📸 Total Scan : 0"
                );

                tvJenisTerbanyak.setText(
                        "♻️ Jenis Terbanyak : -"
                );
            }
        });
    }

    private String safe(String text) {
        return text == null || text.isEmpty()
                ? "-"
                : text;
    }

    private String capitalize(String text) {

        if (text == null || text.isEmpty())
            return "-";

        return text.substring(0,1).toUpperCase()
                + text.substring(1).toLowerCase();
    }
}