package com.example.ecosnap;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilAdminActivity extends AppCompatActivity {

    TextView tvAvatarInisial, tvNamaProfil, tvRoleProfil;
    TextView tvInfoNama, tvInfoEmail, tvInfoWilayah, tvInfoRole, tvInfoWilayahHeader;
    LinearLayout layoutDaftarRT;
    AppCompatButton btnEditProfil, btnLogout;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil_admin);

        mAuth = FirebaseAuth.getInstance();

        tvAvatarInisial = findViewById(R.id.tvAvatarInisial);
        tvNamaProfil = findViewById(R.id.tvNamaProfil);
        tvRoleProfil = findViewById(R.id.tvRoleProfil);
        tvInfoNama = findViewById(R.id.tvInfoNama);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        tvInfoWilayah = findViewById(R.id.tvInfoWilayah);
        tvInfoRole = findViewById(R.id.tvInfoRole);
        tvInfoWilayahHeader = findViewById(R.id.tvInfoWilayahHeader);
        layoutDaftarRT = findViewById(R.id.layoutDaftarRT);
        btnEditProfil = findViewById(R.id.btnEditProfil);
        btnLogout = findViewById(R.id.btnLogout);

        loadDataProfil();

        btnEditProfil.setOnClickListener(v ->
                Toast.makeText(this, "Fitur edit profil coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
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
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User admin = response.body().get(0);

                    String nama = admin.getNama() != null ? admin.getNama() : "-";
                    String email = admin.getEmail() != null ? admin.getEmail() : "-";
                    String wilayah = admin.getWilayah() != null ? admin.getWilayah() : "-";
                    String role = admin.getRole() != null ? admin.getRole() : "admin";
                    String rwId = admin.getRwId() != null ? admin.getRwId() : "-";

                    String inisial = nama.equals("-") || nama.isEmpty()
                            ? "A"
                            : String.valueOf(nama.charAt(0)).toUpperCase();

                    tvAvatarInisial.setText(inisial);
                    tvNamaProfil.setText(nama);
                    tvRoleProfil.setText(capitalize(role));

                    tvInfoNama.setText(nama);
                    tvInfoEmail.setText(email);
                    tvInfoWilayah.setText(wilayah);
                    tvInfoRole.setText(capitalize(role));
                    tvInfoWilayahHeader.setText(rwId);

                    loadDaftarRT(admin.getRwId());
                } else {
                    Toast.makeText(ProfilAdminActivity.this,
                            "Data admin tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(ProfilAdminActivity.this,
                        "Gagal load data: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDaftarRT(String rwId) {
        if (rwId == null || rwId.isEmpty()) {
            layoutDaftarRT.removeAllViews();
            TextView empty = new TextView(this);
            empty.setText("RW belum terisi");
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
                    List<User> listRT = response.body();

                    if (listRT.isEmpty()) {
                        TextView empty = new TextView(ProfilAdminActivity.this);
                        empty.setText("Belum ada RT terdaftar");
                        empty.setTextColor(Color.parseColor("#81C784"));
                        empty.setTextSize(13);
                        layoutDaftarRT.addView(empty);
                        return;
                    }

                    for (int i = 0; i < listRT.size(); i++) {
                        User rt = listRT.get(i);

                        LinearLayout row = new LinearLayout(ProfilAdminActivity.this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setPadding(0, 10, 0, 10);

                        TextView tvIcon = new TextView(ProfilAdminActivity.this);
                        tvIcon.setText("🏠");
                        tvIcon.setTextSize(16);
                        tvIcon.setPadding(0, 0, 12, 0);

                        LinearLayout info = new LinearLayout(ProfilAdminActivity.this);
                        info.setOrientation(LinearLayout.VERTICAL);
                        info.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                        TextView tvNamaRT = new TextView(ProfilAdminActivity.this);
                        tvNamaRT.setText(rt.getRtId() != null ? rt.getRtId() : rt.getNama());
                        tvNamaRT.setTextColor(Color.parseColor("#1B5E20"));
                        tvNamaRT.setTextSize(14);
                        tvNamaRT.setTypeface(null, android.graphics.Typeface.BOLD);

                        TextView tvWilayahRT = new TextView(ProfilAdminActivity.this);
                        tvWilayahRT.setText(rt.getWilayah() != null ? rt.getWilayah() : "-");
                        tvWilayahRT.setTextColor(Color.parseColor("#81C784"));
                        tvWilayahRT.setTextSize(12);

                        info.addView(tvNamaRT);
                        info.addView(tvWilayahRT);
                        row.addView(tvIcon);
                        row.addView(info);
                        layoutDaftarRT.addView(row);

                        if (i < listRT.size() - 1) {
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
                empty.setText("Gagal memuat daftar RT");
                empty.setTextColor(Color.parseColor("#81C784"));
                layoutDaftarRT.addView(empty);
            }
        });
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "-";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}