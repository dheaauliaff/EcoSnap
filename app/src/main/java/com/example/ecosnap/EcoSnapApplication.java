package com.example.ecosnap;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class EcoSnapApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Memaksa aplikasi agar selalu menggunakan Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
