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

    // Field form — etRT untuk input nomor RT manual
    EditText etNama, etEmail, etPassword, etRT, etKodeRW;
    Button btnRegister;
    TextView tvLogin;
    FirebaseAuth mAuth;

    // Kode RW = "password" untuk masuk ke RW tertentu
    // Admin RW kasih kode ini ke ketua RT di wilayahnya
    // Format: kode → nama RW
    private static final Map<String, String> KODE_RW = new HashMap<String, String>() {{
        put("RW01-2024", "001");
        put("RW02-2024", "002");
        put("RW03-2024", "003");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Init semua field
        etNama = findViewById(R.id.etNama);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRT = findViewById(R.id.etRT);         // input RT manual
        etKodeRW = findViewById(R.id.etKodeRW);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        // Klik daftar → proses register
        btnRegister.setOnClickListener(v -> prosesRegister());

        // Klik link login → ke halaman login
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void prosesRegister() {
        // Ambil semua nilai input
        String nama = etNama.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String rtId = etRT.getText().toString().trim();       // RT yang diinput manual
        String kodeRW = etKodeRW.getText().toString().trim(); // kode rahasia dari admin RW

        // Validasi semua field harus terisi
        if (nama.isEmpty() || email.isEmpty() || password.isEmpty() ||
                rtId.isEmpty() || kodeRW.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi panjang password minimal 6 karakter
        if (password.length() < 6) {
            Toast.makeText(this, "Password minimal 6 karakter!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi kode RW — harus sesuai daftar kode yang terdaftar
        if (!KODE_RW.containsKey(kodeRW)) {
            Toast.makeText(this, "Kode RW tidak valid! Hubungi admin RW kamu.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Ambil nama RW dari kode yang diinput
        // contoh: "RW01-2024" → "RW 01"
        String rwId = KODE_RW.get(kodeRW);

        // wilayah = gabungan RT + RW untuk tampilan
        // contoh: "RT 02 - RW 01"
        String wilayah = rtId + " - " + rwId;

        // Daftarkan akun ke Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Pastikan user Firebase tidak null
                        if (mAuth.getCurrentUser() == null) {
                            Toast.makeText(this, "User Firebase kosong",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Ambil UID unik dari Firebase
                        String uid = mAuth.getCurrentUser().getUid();

                        // Simpan semua data ke Supabase
                        simpanKeSupabase(uid, nama, email, wilayah, rwId, rtId);

                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(this, "Gagal daftar: " + msg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void simpanKeSupabase(String uid, String nama, String email,
                                  String wilayah, String rwId, String rtId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Data yang disimpan ke tabel user di Supabase
        Map<String, String> data = new HashMap<>();
        data.put("firebase_uid", uid);   // UID dari Firebase
        data.put("nama", nama);           // nama lengkap
        data.put("email", email);         // email login
        data.put("role", "user");         // semua yang daftar lewat app = RT (user)
        data.put("wilayah", wilayah);     // contoh: "RT 02 - RW 01"
        data.put("rw_id", rwId);          // contoh: "RW 01" — dari kode RW
        data.put("rt_id", rtId);          // contoh: "RT 02" — input manual user

        Call<Void> call = apiService.insertUser(data);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // 201 = created, 204 = no content — keduanya berarti sukses
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