package com.example.ecosnap.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.model.User;
import com.example.ecosnap.admin.DashboardAdminActivity;
import com.example.ecosnap.MainDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText etEmail, etPassword;
    MaterialButton btnLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            btnLogin.setEnabled(false);
            btnLogin.setText("Memproses...");

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi",
                        Toast.LENGTH_SHORT).show();
                resetLoginButton();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show();
                resetLoginButton();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            if (mAuth.getCurrentUser() == null) {
                                Toast.makeText(this, "User login tidak terbaca",
                                        Toast.LENGTH_SHORT).show();
                                resetLoginButton();
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

                            resetLoginButton();
                        }
                    });
        });

        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());
    }

    private void sendPasswordReset() {
        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";

        if (email.isEmpty()) {
            Toast.makeText(this, "Masukkan email dulu untuk reset password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Link reset password dikirim ke: " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";

                        Toast.makeText(this,
                                "Reset password gagal: " + msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void cekRoleUser(String uid) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<User>> call = apiService.getUserByFirebaseUid("eq." + uid);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {

                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    User user = response.body().get(0);
                    String role = user.getRole() != null ? user.getRole() : "user";

                    Intent intent;

                    if (role.equals("admin")) {
                        intent = new Intent(LoginActivity.this, DashboardAdminActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainDashboardActivity.class);
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this,
                            "User tidak ditemukan di database",
                            Toast.LENGTH_SHORT).show();
                    resetLoginButton();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "Koneksi gagal: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                resetLoginButton();
            }
        });
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
    }
}