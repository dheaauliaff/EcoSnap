package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText etNama, etEmail, etPassword, etWilayah, etKodeRW, etRT;
    Button btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;

    private static final Map<String, String> KODE_RW = new HashMap<String, String>() {{
        put("RW01-2024", "RW 01");
        put("RW01-2024", "RW 02");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etNama = findViewById(R.id.etNama);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etWilayah = findViewById(R.id.etWilayah);
        etKodeRW = findViewById(R.id.etKodeRW);
        etRT = findViewById(R.id.etRT);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> prosesRegister());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void prosesRegister() {
        String nama = etNama.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String wilayah = etWilayah.getText().toString().trim();
        String kodeRW = etKodeRW.getText().toString().trim();
        String rtId = etRT.getText().toString().trim();

        if (nama.isEmpty() || email.isEmpty() || password.isEmpty() ||
                wilayah.isEmpty() || kodeRW.isEmpty() || rtId.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!KODE_RW.containsKey(kodeRW)) {
            Toast.makeText(this, "Kode RW tidak valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        String rwId = KODE_RW.get(kodeRW);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() == null) {
                            Toast.makeText(this, "User Firebase kosong", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = mAuth.getCurrentUser().getUid();
                        simpanKeSupabase(uid, nama, email, wilayah, rwId, rtId);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Gagal daftar: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanKeSupabase(String uid, String nama, String email,
                                  String wilayah, String rwId, String rtId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        Map<String, String> data = new HashMap<>();
        data.put("firebase_uid", uid);
        data.put("nama", nama);
        data.put("email", email);
        data.put("role", "user");
        data.put("wilayah", wilayah);
        data.put("rw_id", rwId);
        data.put("rt_id", rtId);

        Call<Void> call = apiService.insertUser(data);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201 || response.code() == 204) {
                    Toast.makeText(RegisterActivity.this,
                            "Registrasi berhasil! Silakan login.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Gagal simpan data: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}