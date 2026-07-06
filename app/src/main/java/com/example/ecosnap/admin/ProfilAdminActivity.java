package com.example.ecosnap.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.auth.LoginActivity;
import com.example.ecosnap.R;
import com.example.ecosnap.WilayahUtils;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.ecosnap.user.HistoryActivity;

public class ProfilAdminActivity extends AppCompatActivity {

    TextView tvAvatarInisial, tvNamaProfil, tvRoleProfil;
    TextView tvInfoNama, tvInfoNomorHp, tvInfoWilayah, tvInfoRole, tvInfoWilayahHeader;
    LinearLayout layoutDaftarRT;
    AppCompatButton btnEditProfil, btnLogout, btnLihatRiwayat;
    FirebaseAuth mAuth;
    String currentRwId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil_admin);

        mAuth = FirebaseAuth.getInstance();

        // Init semua views
        tvAvatarInisial = findViewById(R.id.tvAvatarInisial);
        tvNamaProfil = findViewById(R.id.tvNamaProfil);
        tvRoleProfil = findViewById(R.id.tvRoleProfil);
        tvInfoNama = findViewById(R.id.tvInfoNama);
        tvInfoNomorHp = findViewById(R.id.tvInfoNomorHp); // ganti dari tvInfoEmail
        tvInfoWilayah = findViewById(R.id.tvInfoWilayah);
        tvInfoRole = findViewById(R.id.tvInfoRole);
        tvInfoWilayahHeader = findViewById(R.id.tvInfoWilayahHeader);
        layoutDaftarRT = findViewById(R.id.layoutDaftarRT);
        btnEditProfil = findViewById(R.id.btnEditProfil);
        btnLogout     = findViewById(R.id.btnLogout);
        btnLihatRiwayat = findViewById(R.id.btnLihatRiwayat);

        // Setup bottom navigation admin
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_admin_profil);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_rekap) {
                startActivity(new Intent(this, RekapAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_maps) {
                startActivity(new Intent(this, AdminMapsActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_admin_profil) {
                return true;
            }
            return false;
        });

        loadDataProfil();

        if (btnEditProfil != null) {
            btnEditProfil.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditProfilAdminActivity.class);
                intent.putExtra("nama", tvInfoNama.getText().toString());
                intent.putExtra("nomor_hp", tvInfoNomorHp.getText().toString());
                intent.putExtra("rw_id", currentRwId);
                startActivityForResult(intent, 1001);
            });
        }

        // Tombol lihat riwayat scan semua user
        if (btnLihatRiwayat != null) {
            btnLihatRiwayat.setOnClickListener(v -> {
                Intent intent = new Intent(this, HistoryActivity.class);
                intent.putExtra("admin_view", true);
                startActivity(intent);
            });
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadDataProfil();
        }
    }

    private void loadDataProfil() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session login habis", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + currentUser.getUid());

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    User admin = response.body().get(0);

                    String nama = admin.getNama() != null ? admin.getNama() : "-";
                    // Ambil nomor HP bukan email
                    String nomorHp = admin.getNomorHp() != null ? admin.getNomorHp() : "-";
                    String wilayah = admin.getWilayah() != null ? admin.getWilayah() : "-";
                    String role = admin.getRole() != null ? admin.getRole() : "admin";
                    String rwId = admin.getRwId() != null ? admin.getRwId() : "";
                    currentRwId = rwId;

                    // Inisial dari huruf pertama nama
                    String inisial = nama.equals("-") || nama.isEmpty()
                            ? "A"
                            : String.valueOf(nama.charAt(0)).toUpperCase();

                    // Set header profil
                    tvAvatarInisial.setText(inisial);
                    tvNamaProfil.setText(nama);
                    tvRoleProfil.setText(capitalize(role));
                    String areaSource = rwId != null && !rwId.trim().isEmpty() ? rwId : wilayah;
                    String rtLabel = WilayahUtils.formatAdminAreaFromLegacyRw(areaSource);
                    tvInfoWilayahHeader.setText(rtLabel);

                    // Set info akun
                    tvInfoNama.setText(nama);
                    tvInfoNomorHp.setText(nomorHp); // tampilkan nomor HP
                    tvInfoWilayah.setText(rtLabel);
                    tvInfoRole.setText(capitalize(role));

                    // Load daftar warga di bawah RT ini
                    loadDaftarRT(admin.getRwId());

                } else {
                    Toast.makeText(ProfilAdminActivity.this,
                            "Data admin tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(ProfilAdminActivity.this,
                        "Gagal load data: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDaftarRT(String rwId) {
        if (rwId == null || rwId.isEmpty()) {
            layoutDaftarRT.removeAllViews();
            TextView empty = new TextView(this);
            empty.setText("RT belum terisi");
            empty.setTextColor(Color.parseColor("#81C784"));
            layoutDaftarRT.addView(empty);
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByRwId("eq." + rwId, "eq.user");

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                layoutDaftarRT.removeAllViews();

                if (response.isSuccessful() && response.body() != null) {
                    List<User> listWarga = response.body();

                    // Kalau belum ada warga terdaftar
                    if (listWarga.isEmpty()) {
                        TextView empty = new TextView(ProfilAdminActivity.this);
                        empty.setText("Belum ada warga terdaftar");
                        empty.setTextColor(Color.parseColor("#81C784"));
                        empty.setTextSize(13);
                        layoutDaftarRT.addView(empty);
                        return;
                    }

                    // Loop semua warga dan tampilkan
                    for (int i = 0; i < listWarga.size(); i++) {
                        User warga = listWarga.get(i);

                        LinearLayout row = new LinearLayout(ProfilAdminActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setPadding(0, 10, 0, 10);

                        // Icon pengguna
                        ImageView tvIcon = new ImageView(ProfilAdminActivity.this);
                        tvIcon.setImageResource(R.drawable.ic_user_outline);
                        tvIcon.setColorFilter(Color.parseColor("#1B5E20"));
                        tvIcon.setPadding(0, 0, 12, 0);
                        tvIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(16)));

                        // Info warga
                        LinearLayout info = new LinearLayout(ProfilAdminActivity.this);
                        info.setOrientation(LinearLayout.VERTICAL);
                        info.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                        // Tampilkan nama warga
                        TextView tvNamaRT = new TextView(ProfilAdminActivity.this);
                        tvNamaRT.setText(warga.getNama() != null ? warga.getNama() : "-");
                        tvNamaRT.setTextColor(Color.parseColor("#81C784"));
                        tvNamaRT.setTextSize(12);

                        TextView tvNomorHpRT = new TextView(ProfilAdminActivity.this);
                        tvNomorHpRT.setText("");
                        tvNomorHpRT.setTextColor(Color.parseColor("#81C784"));
                        tvNomorHpRT.setTextSize(12);

                        info.addView(tvNamaRT);
                        info.addView(tvNomorHpRT);
                        row.addView(tvIcon);
                        row.addView(info);
                        layoutDaftarRT.addView(row);

                        // Divider antar warga kecuali yang terakhir
                        if (i < listWarga.size() - 1) {
                            View divider = new View(ProfilAdminActivity.this);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                            divider.setLayoutParams(params);
                            divider.setBackgroundColor(Color.parseColor("#E8F5E9"));
                            layoutDaftarRT.addView(divider);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                TextView empty = new TextView(ProfilAdminActivity.this);
                empty.setText("Gagal memuat daftar warga");
                empty.setTextColor(Color.parseColor("#81C784"));
                layoutDaftarRT.addView(empty);
            }
        });
    }

    // Helper capitalize huruf pertama
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "-";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
