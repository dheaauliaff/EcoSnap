package com.example.ecosnap.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class RekapAdminActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekap_admin);

        bottomNav = findViewById(R.id.bottomNav);

        setupBottomNavigation();
        setupBackPressed();
    }

    private void setupBottomNavigation() {

        // halaman aktif sekarang = Rekap
        bottomNav.setSelectedItemId(R.id.nav_admin_rekap);

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_admin_dashboard) {

                startActivity(new Intent(this, DashboardAdminActivity.class));
                finish();
                return true;

            } else if (id == R.id.nav_admin_rekap) {

                // sedang di halaman ini
                return true;

            } else if (id == R.id.nav_admin_ranking) {

                startActivity(new Intent(this, AdminRankingActivity.class));
                finish();
                return true;

            } else if (id == R.id.nav_admin_maps) {

                startActivity(new Intent(this, AdminMapsActivity.class));
                finish();
                return true;

            } else if (id == R.id.nav_admin_profil) {

                startActivity(new Intent(this, ProfilAdminActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void setupBackPressed() {

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                });
    }
}