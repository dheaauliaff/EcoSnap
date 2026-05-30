package com.example.ecosnap.user;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.R;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfilActivity extends AppCompatActivity {

    private TextInputEditText etEditNama, etEditNomorHp, etEditRtId;
    private MaterialButton btnSimpanProfil;
    private ImageView btnBack;
    private View progressBar;

    private String firebaseUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profil);

        etEditNama    = findViewById(R.id.etEditNama);
        etEditNomorHp = findViewById(R.id.etEditNomorHp);
        etEditRtId    = findViewById(R.id.etEditRtId);
        btnSimpanProfil = findViewById(R.id.btnSimpanProfil);
        btnBack       = findViewById(R.id.btnBack);
        progressBar   = findViewById(R.id.progressBar);

        // Pre-fill dari data yang dikirim ProfilFragment
        String prefillNama   = getIntent().getStringExtra("nama");
        String prefillHp     = getIntent().getStringExtra("nomor_hp");
        String prefillRt     = getIntent().getStringExtra("rt_id");

        if (prefillNama != null && !prefillNama.equals("-")) etEditNama.setText(prefillNama);
        if (prefillHp   != null && !prefillHp.equals("-"))   etEditNomorHp.setText(prefillHp);
        if (prefillRt   != null && !prefillRt.equals("-"))   etEditRtId.setText(prefillRt);

        // Ambil UID dari Firebase
        firebaseUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        btnSimpanProfil.setOnClickListener(v -> simpanPerubahan());
        btnBack.setOnClickListener(v -> finish());
    }

    private void simpanPerubahan() {
        String nama    = etEditNama.getText()    != null ? etEditNama.getText().toString().trim()    : "";
        String nomorHp = etEditNomorHp.getText() != null ? etEditNomorHp.getText().toString().trim() : "";
        String rtId    = etEditRtId.getText()    != null ? etEditRtId.getText().toString().trim()    : "";

        if (nama.isEmpty() || nomorHp.isEmpty()) {
            Toast.makeText(this, "Nama dan nomor HP wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nomorHp.length() < 10) {
            Toast.makeText(this, "Nomor HP tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseUid.isEmpty()) {
            Toast.makeText(this, "Sesi tidak valid, silakan login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnSimpanProfil.setEnabled(false);

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        Map<String, String> updates = new HashMap<>();
        updates.put("nama",     nama);
        updates.put("nomor_hp", nomorHp);
        if (!rtId.isEmpty()) {
            updates.put("rt_id",   rtId);
            updates.put("wilayah", rtId);
        }

        // PATCH ke Supabase — filter by firebase_uid
        Call<Void> call = api.updateUserPatch("eq." + firebaseUid, updates);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSimpanProfil.setEnabled(true);

                if (response.isSuccessful() || response.code() == 204) {
                    Toast.makeText(EditProfilActivity.this,
                            "Profil berhasil diperbarui ✓", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProfilActivity.this,
                            "Gagal menyimpan (" + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnSimpanProfil.setEnabled(true);
                Toast.makeText(EditProfilActivity.this,
                        "Koneksi gagal: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
