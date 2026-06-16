package com.example.ecosnap;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainDashboardActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        bottomNav = findViewById(R.id.bottomNav);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
            navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                    bottomNav.post(this::applyFeaturedScanNav));
        }
        bottomNav.post(this::applyFeaturedScanNav);
    }

    private void applyFeaturedScanNav() {
        if (bottomNav == null) return;
        ViewGroup menuView = bottomNav.getChildCount() > 0 && bottomNav.getChildAt(0) instanceof ViewGroup
                ? (ViewGroup) bottomNav.getChildAt(0)
                : null;
        if (menuView == null || menuView.getChildCount() <= 2) return;

        View scanItem = menuView.getChildAt(2);
        ImageView icon = findFirstImageView(scanItem);
        if (icon == null) return;

        int size = dp(42);
        ViewGroup.LayoutParams params = icon.getLayoutParams();
        params.width = size;
        params.height = size;
        icon.setLayoutParams(params);
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));
        icon.setBackgroundResource(R.drawable.bg_scan_nav_circle);
        icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        icon.setElevation(dp(4));
    }

    private ImageView findFirstImageView(View view) {
        if (view instanceof ImageView) return (ImageView) view;
        if (!(view instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ImageView found = findFirstImageView(group.getChildAt(i));
            if (found != null) return found;
        }
        return null;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
