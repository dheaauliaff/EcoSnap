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

    // Ganti etEmail jadi etNomorHp
    EditText etNama, etNomorHp, etPassword, etRT, etWilayahRW, etKodeRW;
    MaterialButton btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;

    // Kode RW = password untuk masuk ke RW tertentu
    // Admin RW kasih kode ini ke ketua RT di wilayahnya
    private static final Map<String, String> KODE_RW = new HashMap<String, String>() {{
        put("RW01-2024", "RW 01");
        put("RW02-2024", "RW 02");
        put("RW03-2024", "RW 03");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Init semua field
        etNama = findViewById(R.id.etNama);
        etNomorHp = findViewById(R.id.etNomorHp); // ganti dari etEmail
        etPassword = findViewById(R.id.etPassword);
        etRT = findViewById(R.id.etRT);
        etKodeRW = findViewById(R.id.etKodeRW);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        etWilayahRW = findViewById(R.id.etWilayahRW);

        btnRegister.setOnClickListener(v -> prosesRegister());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void prosesRegister() {
        String nama = etNama.getText().toString().trim();
        String nomorHp = etNomorHp.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String rtId = etRT.getText().toString().trim();
        String wilayahRW = etWilayahRW.getText().toString().trim(); // tambah ini
        String kodeRW = etKodeRW.getText().toString().trim();

        // Validasi semua field harus terisi
        if (nama.isEmpty() || nomorHp.isEmpty() || password.isEmpty() ||
                rtId.isEmpty() || wilayahRW.isEmpty() || kodeRW.isEmpty()) {
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

        if (!KODE_RW.containsKey(kodeRW)) {
            Toast.makeText(this, "Kode RW tidak valid! Hubungi admin RW kamu.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String rwId = KODE_RW.get(kodeRW);

        // Wilayah = RT + RW yang diinput user
        String wilayah = rtId + " - " + wilayahRW;

        String emailFiktif = nomorHp + "@ecosnap.com";

        mAuth.createUserWithEmailAndPassword(emailFiktif, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() == null) {
                            Toast.makeText(this, "User Firebase kosong",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String uid = mAuth.getCurrentUser().getUid();
                        simpanKeSupabase(uid, nama, nomorHp, wilayah, rwId, rtId);
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(this, "Gagal daftar: " + msg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanKeSupabase(String uid, String nama, String nomorHp,
                                  String wilayah, String rwId, String rtId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Data yang disimpan ke tabel user di Supabase
        Map<String, String> data = new HashMap<>();
        data.put("firebase_uid", uid);       // UID dari Firebase
        data.put("nama", nama);               // nama lengkap
        data.put("nomor_hp", nomorHp);        // nomor HP asli — bukan email fiktif
        data.put("role", "user");             // semua daftar lewat app = RT
        data.put("wilayah", wilayah);         // contoh: "RT 02 - RW 01"
        data.put("rw_id", rwId);              // contoh: "RW 01"
        data.put("rt_id", rtId);              // contoh: "RT 02"

        Call<Void> call = apiService.insertUser(data);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201 || response.code() == 204) {
                    Toast.makeText(RegisterActivity.this,
                            "Registrasi berhasil! Silakan login.",
                            Toast.LENGTH_SHORT).show();
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