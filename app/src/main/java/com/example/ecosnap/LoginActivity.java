package com.example.ecosnap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // Ganti etEmail jadi etNomorHp
    TextInputEditText etNomorHp, etPassword;
    MaterialButton btnLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Init views sesuai id baru
        etNomorHp = findViewById(R.id.etNomorHp);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String nomorHp = etNomorHp.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validasi field tidak boleh kosong
            if (nomorHp.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nomor HP dan password harus diisi",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Validasi format nomor HP minimal 10 digit
            if (nomorHp.length() < 10) {
                Toast.makeText(this, "Nomor HP tidak valid",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Konversi nomor HP ke format email untuk Firebase
            // contoh: "081234567890" → "081234567890@ecosnap.com"
            String emailFiktif = nomorHp + "@ecosnap.com";

            // Login ke Firebase pakai email fiktif
            mAuth.signInWithEmailAndPassword(emailFiktif, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (mAuth.getCurrentUser() == null) {
                                Toast.makeText(this, "User login tidak terbaca",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String uid = mAuth.getCurrentUser().getUid();
                            cekRoleUser(uid);
                        } else {
                            String msg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Unknown error";
                            Toast.makeText(this, "Login gagal: " + msg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Link ke halaman register
        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void cekRoleUser(String uid) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + uid);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    String role = user.getRole() != null ? user.getRole() : "user";

                    if (role.equals("admin")) {
                        Intent intent = new Intent(LoginActivity.this,
                                DashboardAdminActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(LoginActivity.this,
                                DashboardUserActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "User tidak ditemukan di database",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "Koneksi gagal: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}