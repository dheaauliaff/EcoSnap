package com.example.ecosnap.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etNama, etNomorHp, etPassword, etPasswordConfirm, etRT;
    MaterialButton btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etNama           = findViewById(R.id.etNama);
        etNomorHp        = findViewById(R.id.etNomorHp);
        etPassword       = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etRT             = findViewById(R.id.etRT);
        btnRegister      = findViewById(R.id.btnRegister);
        tvLogin          = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> prosesRegister());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void prosesRegister() {
        String nama            = etNama.getText().toString().trim();
        String nomorHp         = etNomorHp.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();
        String rtId            = etRT.getText().toString().trim();

        // Validasi field wajib
        if (nama.isEmpty() || nomorHp.isEmpty() || password.isEmpty() || rtId.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nomorHp.length() < 10) {
            Toast.makeText(this, "Nomor HP tidak valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Konversi nomor HP ke email fiktif untuk Firebase Auth
        String emailFiktif = nomorHp + "@ecosnap.com";

        mAuth.createUserWithEmailAndPassword(emailFiktif, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() == null) {
                            Toast.makeText(this, "User Firebase kosong", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String uid = mAuth.getCurrentUser().getUid();
                        simpanKeSupabase(uid, nama, nomorHp, rtId);
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Gagal daftar: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanKeSupabase(String uid, String nama, String nomorHp, String rtId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        Map<String, String> data = new HashMap<>();
        data.put("firebase_uid", uid);
        data.put("nama", nama);
        data.put("nomor_hp", nomorHp);
        data.put("role", "user");
        data.put("rt_id", rtId);
        data.put("wilayah", rtId);         // contoh: "RT 02"
        data.put("is_approved", "false");  // menunggu persetujuan admin

        Call<Void> call = apiService.insertUser(data);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201 || response.code() == 204) {
                    Toast.makeText(RegisterActivity.this,
                            "Pendaftaran berhasil! Menunggu persetujuan admin.",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Gagal simpan data: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "Koneksi gagal: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}