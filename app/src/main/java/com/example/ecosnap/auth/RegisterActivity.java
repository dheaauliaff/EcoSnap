package com.example.ecosnap.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.R;
import com.example.ecosnap.WilayahUtils;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // Kode RW = password untuk masuk ke RW tertentu
    // Admin RW kasih kode ini ke ketua RT di wilayahnya
    //ini jadiin dropdown aja di registrasi
    private static final Map<String, String> KODE_RW = new HashMap<String, String>() {{
        put("RW01-2024", "RW 01");
        put("RW02-2024", "RW 02");
        put("RW03-2024", "RW 03");
        put("RW04-2024", "RW 04");
    }};

    EditText etNama, etEmail, etNomorHp, etPassword, etPasswordConfirm, etRT, etRW;
    MaterialButton btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etNama           = findViewById(R.id.etNama);
        etEmail          = findViewById(R.id.etEmail);
        etNomorHp        = findViewById(R.id.etNomorHp);
        etPassword       = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etRT             = findViewById(R.id.etRT);
        etRW             = findViewById(R.id.etRW);
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
        String email           = etEmail.getText().toString().trim();
        String nomorHp         = etNomorHp.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();
        String rtId            = etRT.getText().toString().trim();
        String kodeRwInput     = etRW.getText().toString().trim();

        // Validasi field wajib
        if (nama.isEmpty() || email.isEmpty() || nomorHp.isEmpty() || password.isEmpty() || rtId.isEmpty() || kodeRwInput.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        //validaso gmail
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Format email tidak valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cek apakah Kode RW valid
        if (!KODE_RW.containsKey(kodeRwInput)) {
            Toast.makeText(this, "Kode RW tidak valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ambil nama RW yang sebenarnya berdasarkan kode
        String rwId = WilayahUtils.formatRwId(KODE_RW.get(kodeRwInput));
        String formattedRtId = WilayahUtils.formatRtId(rtId);

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
        //String emailFiktif = nomorHp + "@ecosnap.com";



        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() == null) {
                            Toast.makeText(this, "User Firebase kosong", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = mAuth.getCurrentUser().getUid();
                        simpanKeSupabase(uid, nama, email, nomorHp, formattedRtId, rwId);

                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";

                        Toast.makeText(this,
                                "Gagal daftar: " + msg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanKeSupabase(String uid, String nama, String email, String nomorHp, String rtId, String rwId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        Map<String, String> data = new HashMap<>();
        data.put("firebase_uid", uid);
        data.put("email", email);
        data.put("nama", nama);
        data.put("nomor_hp", nomorHp);
        data.put("role", "user");
        data.put("rt_id", rtId);
        data.put("rw_id", rwId);
        data.put("wilayah", rtId + " " + rwId); // contoh: "RT 02 RW 05"

        Call<Void> call = apiService.insertUser(data);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201 || response.code() == 204) {
                    Toast.makeText(RegisterActivity.this,
                            "Pendaftaran berhasil!",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                } else {
                    String errorMsg = "Gagal simpan data: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    rollbackFirebase();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "Koneksi gagal: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                rollbackFirebase();
            }
        });
    }

    private void rollbackFirebase() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().delete().addOnCompleteListener(task -> {
                // Berhasil dihapus agar bisa coba daftar lagi
            });
        }
    }
}
