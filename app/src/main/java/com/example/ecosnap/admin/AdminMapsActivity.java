package com.example.ecosnap.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.ScanHistory;
import com.example.ecosnap.WilayahUtils;
import com.example.ecosnap.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMapsActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {"Organik", "Plastik", "Kertas", "Kaca", "Kardus", "Logam", "Bukan Sampah"};
    private static final double ZONE_RADIUS_METERS = 80.0;

    // Views
    MapView mapView;
    BottomNavigationView bottomNav;
    LinearLayout legendRow;
    LinearLayout topRegionRow;
    LinearLayout categoryBreakdownContainer;
    LinearLayout filterBadgeRow;
    TextView tvDetailContent;
    TextView tvFilterBadge;
    LinearLayout detailMarkerCard;
    TextView btnResetFilter;
    TextView tvWilayahAdmin;
    NestedScrollView scrollView;

    // OSMDroid
    private MyLocationNewOverlay myLocationOverlay;

    // Auth & Data
    FirebaseAuth mAuth;
    String rwId = "";
    List<ScanHistory> allScans = new ArrayList<>();
    final List<String> registeredRtLabels = new ArrayList<>();

    // State
    int totalReports = 0;
    final Map<String, Integer> globalCategoryCount = new HashMap<>();
    final Map<String, Integer> rtCount = new HashMap<>();

    // Active filter (null = no filter = show all)
    String activeFilter = null;
    String activeRtFilter = "Semua RT";
    List<ScanHistory> displayedScans = new ArrayList<>();

    // Legend views for toggling selection state
    final List<View> legendItemViews = new ArrayList<>();
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_maps_admin);

        mAuth = FirebaseAuth.getInstance();

        mapView = findViewById(R.id.mapView);
        bottomNav = findViewById(R.id.bottomNav);
        legendRow = findViewById(R.id.legendRow);
        topRegionRow = findViewById(R.id.topRegionRow);
        filterBadgeRow = findViewById(R.id.filterBadgeRow);
        tvFilterBadge = findViewById(R.id.tvFilterBadge);
        btnResetFilter = findViewById(R.id.btnResetFilter);
        tvWilayahAdmin = findViewById(R.id.tvWilayahAdmin);
        //ini baruu yaa
        tvDetailContent = findViewById(R.id.tvDetailContent);
        detailMarkerCard = findViewById(R.id.detailMarkerCard);
        scrollView = findViewById(R.id.scrollViewMaps);

        // Request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }

        setupMap();
        setupControls();
        buildLegend();
        buildTopRegions(new ArrayList<>());
        animatePage();

        // Reset filter button
        if (btnResetFilter != null) {
            btnResetFilter.setOnClickListener(v -> {
                activeFilter = null;
                filterBadgeRow.setVisibility(View.GONE);
                refreshLegendSelection(-1);
                refreshMapMarkers();
                buildCategoryBreakdown();
            });
        }

        // Dropdowns Filter Maps
        View btnFilterPeriod = findViewById(R.id.btnFilterPeriod);
        View btnFilterArea = findViewById(R.id.btnFilterArea);
        TextView tvFilterPeriod = findViewById(R.id.tvFilterPeriod);

        if (btnFilterPeriod != null) {
            btnFilterPeriod.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, btnFilterPeriod);
                popup.getMenu().add("Bulan Ini");
                popup.getMenu().add("Semua Waktu");
                popup.setOnMenuItemClickListener(item -> {
                    if (tvFilterPeriod != null) tvFilterPeriod.setText(item.getTitle());
                    Toast.makeText(this, "Menampilkan data: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        if (btnFilterArea != null) {
            btnFilterArea.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, btnFilterArea);
                popup.getMenu().add("Semua RT");

                List<String> rtLabels = getDropdownRtLabels();
                for (String label : rtLabels) {
                    popup.getMenu().add(label);
                }

                popup.setOnMenuItemClickListener(item -> {
                    String selectedLabel = item.getTitle().toString();
                    if (tvWilayahAdmin != null) tvWilayahAdmin.setText(selectedLabel);

                    if (selectedLabel.equals("Semua RT")) {
                        activeRtFilter = "Semua RT";
                    } else {
                        // Map label back to raw ID or use fallback
                        activeRtFilter = selectedLabel;
                    }

                    applyFilters();
                    Toast.makeText(this, "RT dipilih: " + selectedLabel, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        loadDataAdmin();

        // Bottom navigation
        bottomNav.setSelectedItemId(R.id.nav_admin_maps);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_admin_dashboard) {
                startActivity(new Intent(this, DashboardAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_admin_rekap) {
                startActivity(new Intent(this, RekapAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_admin_ranking) {
                startActivity(new Intent(this, AdminRankingActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_admin_maps) {
                return true;
            } else if (id == R.id.nav_admin_profil) {
                startActivity(new Intent(this, ProfilAdminActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });
    }

    // ─── Map Setup ─────────────────────────────────────────────────────────────

    private void setupMap() {
        if (mapView == null) return;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(-6.8900, 107.5400));

        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void setupControls() {
        View btnLocate = findViewById(R.id.btnLocate);
        TextView zoomIn = findViewById(R.id.btnZoomIn);
        TextView zoomOut = findViewById(R.id.btnZoomOut);

        if (btnLocate != null) btnLocate.setOnClickListener(v -> centerOnMyLocation());
        if (zoomIn != null) zoomIn.setOnClickListener(v -> mapView.getController().zoomIn());
        if (zoomOut != null) zoomOut.setOnClickListener(v -> mapView.getController().zoomOut());
    }

    private void centerOnMyLocation() {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            mapView.getController().setZoom(17.0);
        }
    }

    // ─── Supabase ──────────────────────────────────────────────────────────────

    private void loadDataAdmin() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getUserByFirebaseUid("eq." + uid).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User admin = response.body().get(0);
                    rwId = admin.getRwId() != null ? admin.getRwId() : "";

                    TextView tvPetaSubtitle = findViewById(R.id.tvPetaSubtitle);
                    if (tvPetaSubtitle != null && admin.getWilayah() != null) {
                        tvPetaSubtitle.setText("Pantau kondisi sampah di wilayah " + admin.getWilayah() + " secara real-time");
                    }

                    activeRtFilter = "Semua RT";
                    if (tvWilayahAdmin != null) tvWilayahAdmin.setText(activeRtFilter);
                    loadRegisteredRtUsers(api);
                    loadDataSebaran();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(AdminMapsActivity.this, "Gagal load data admin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRegisteredRtUsers(ApiService api) {
        registeredRtLabels.clear();
        api.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (User user : response.body()) {
                        if (!"user".equalsIgnoreCase(user.getRole())) continue;
                        if (!WilayahUtils.isMatchingRw(user.getRwId(), rwId)) continue;
                        String rt = WilayahUtils.formatRtId(user.getRtId());
                        if (!rt.isEmpty() && !registeredRtLabels.contains(rt)) {
                            registeredRtLabels.add(rt);
                        }
                    }
                    Collections.sort(registeredRtLabels);
                    if (!allScans.isEmpty()) applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private List<String> getDropdownRtLabels() {
        if (!registeredRtLabels.isEmpty()) {
            return new ArrayList<>(registeredRtLabels);
        }

        List<String> rtLabels = new ArrayList<>();
        for (ScanHistory s : allScans) {
            if (WilayahUtils.isMatchingRw(s.getRwId(), rwId)) {
                String formatted = WilayahUtils.formatRtId(s.getRtId());
                if (!formatted.isEmpty() && !rtLabels.contains(formatted)) {
                    rtLabels.add(formatted);
                }
            }
        }
        Collections.sort(rtLabels);
        return rtLabels;
    }

    private void loadDataSebaran() {
        if (rwId == null || rwId.isEmpty()) return;
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call, Response<List<ScanHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processData(response.body());
                } else {
                    Toast.makeText(AdminMapsActivity.this, "Gagal memuat data peta", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                Toast.makeText(AdminMapsActivity.this, "Tidak dapat terhubung ke server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processData(List<ScanHistory> list) {
        allScans = list;
        applyFilters();
    }

    private void applyFilters() {
        displayedScans.clear();
        for (ScanHistory s : allScans) {
            if (WilayahUtils.isMatchingRw(s.getRwId(), rwId)) {
                boolean rtRegistered = registeredRtLabels.isEmpty()
                        || registeredRtLabels.contains(WilayahUtils.formatRtId(s.getRtId()));
                if (rtRegistered && ("Semua RT".equals(activeRtFilter)
                        || WilayahUtils.isMatchingRt(s.getRtId(), activeRtFilter))) {
                    displayedScans.add(s);
                }
            }
        }

        totalReports = displayedScans.size();
        globalCategoryCount.clear();
        rtCount.clear();

        for (String rt : registeredRtLabels) {
            rtCount.put(rt, 0);
        }

        for (ScanHistory s : displayedScans) {
            String nama = WilayahUtils.normalizeJenis(s.getJenisSampah());
            String rt = WilayahUtils.formatRtId(s.getRtId());
            if (nama != null) globalCategoryCount.put(nama,
                    globalCategoryCount.getOrDefault(nama, 0) + 1);
            if (!rt.isEmpty()) rtCount.put(rt,
                    rtCount.getOrDefault(rt, 0) + 1);
        }

        refreshMapMarkers();
        updateStatCards();
        buildTopRegions(sortedRtList());
        buildLegend();   // rebuild with percentages now known
        buildCategoryBreakdown();
    }

    // ─── Map Markers ───────────────────────────────────────────────────────────

    /**
     * Clears and re-draws map markers.
     * If activeFilter != null, only draws markers for that category.
     */
    private void refreshMapMarkers() {
        if (mapView == null) return;
        mapView.getOverlays().removeIf(o -> (o instanceof Marker) || (o instanceof Polygon));

        List<ScanHistory> toShow = (activeFilter == null) ? displayedScans : new ArrayList<>();
        if (activeFilter != null) {
            for (ScanHistory s : displayedScans) {
                if (activeFilter.equalsIgnoreCase(WilayahUtils.normalizeJenis(s.getJenisSampah()))) {
                    toShow.add(s);
                }
            }
        }

        for (ScanHistory s : toShow) {
            if (s.getLatitude() != null && s.getLongitude() != null
                    && s.getLatitude() != 0.0 && s.getLongitude() != 0.0) {
                addScanPinAndZone(s);
            }
        }
        mapView.invalidate();
    }

    private void addScanPinAndZone(ScanHistory scan) {
        double lat = scan.getLatitude();
        double lng = scan.getLongitude();
        String nama = !WilayahUtils.normalizeJenis(scan.getJenisSampah()).isEmpty()
                ? WilayahUtils.normalizeJenis(scan.getJenisSampah())
                : "Sampah";
        String kat = scan.getKategori() != null ? scan.getKategori() : "-";
        int color = colorForNama(nama);

        Polygon zone = new Polygon(mapView);
        zone.setPoints(circlePoints(lat, lng, ZONE_RADIUS_METERS));
        zone.setFillColor(withAlpha(color, 45));
        zone.setStrokeColor(withAlpha(color, 160));
        zone.setStrokeWidth(2.5f);
        mapView.getOverlays().add(zone);

        String rt = scan.getRtId() != null ? WilayahUtils.formatRtId(scan.getRtId()) : "-";
        String rw = scan.getRwId() != null ? WilayahUtils.formatRwId(scan.getRwId()) : "-";
        String alamat = scan.getAlamat() != null && !scan.getAlamat().trim().isEmpty()
                ? scan.getAlamat().trim()
                : "memuat...";
        String dominanArea = getDominantNearby(lat, lng);
        int totalArea = countNearby(lat, lng);

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(lat, lng));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("");
        marker.setSnippet("");
        marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(nama, color)));
        mapView.getOverlays().add(marker);
        if ("memuat...".equals(alamat)) {
            fetchAddressAsync(lat, lng, marker);
        }
            //BARU
        marker.setOnMarkerClickListener((m, mapView) -> {

            showMarkerDetail(
                    nama,
                    kat,
                    alamat,
                    rt,
                    rw,
                    totalArea,
                    dominanArea
            );

            mapView.getController().animateTo(
                    new GeoPoint(lat, lng)
            );

            return true;
        });
    }

    // ─── Legend with clickable category filter ─────────────────────────────────

    private void buildLegend() {
        if (legendRow == null) return;
        legendRow.removeAllViews();
        legendItemViews.clear();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            final int idx = i;

            View item = inflater.inflate(R.layout.item_legend_category, legendRow, false);
            FrameLayout circle = item.findViewById(R.id.legendCircle);
            ImageView icon = item.findViewById(R.id.ivLegendIcon);
            TextView label = item.findViewById(R.id.tvLegendLabel);
            TextView tvPercent = item.findViewById(R.id.tvLegendPercent);

            int color = colorForNama(cat);
            int softBg = softColor(color);

            if (circle != null) circle.setBackgroundTintList(ColorStateList.valueOf(softBg));
            if (icon != null) {
                icon.setImageResource(iconFor(cat));
                icon.setImageTintList(ColorStateList.valueOf(color));
            }
            if (label != null) label.setText(cat);

            // Show percentage if data is loaded
            if (tvPercent != null && totalReports > 0) {
                int count = globalCategoryCount.getOrDefault(cat, 0);
                int pct = Math.round((count * 100f) / totalReports);
                tvPercent.setText(pct + "%");
                tvPercent.setTextColor(color);
                tvPercent.setVisibility(View.VISIBLE);
            }

            legendItemViews.add(item);
            legendRow.addView(item);

            // Click: filter map to this category (toggle)
            item.setOnClickListener(v -> {
                if (cat.equals(activeFilter)) {
                    // Toggle off — reset to show all
                    activeFilter = null;
                    filterBadgeRow.setVisibility(View.GONE);
                    refreshLegendSelection(-1);
                } else {
                    activeFilter = cat;
                    tvFilterBadge.setText("Filter aktif: " + cat);
                    filterBadgeRow.setVisibility(View.VISIBLE);
                    refreshLegendSelection(idx);
                }
                refreshMapMarkers();
                buildCategoryBreakdown();
            });
        }
    }

    /** Highlights selected legend item; dims unselected ones */
    private void refreshLegendSelection(int selectedIdx) {
        for (int i = 0; i < legendItemViews.size(); i++) {
            View v = legendItemViews.get(i);
            if (selectedIdx < 0) {
                v.setAlpha(1f);
            } else {
                v.setAlpha(i == selectedIdx ? 1f : 0.4f);
            }
            // Scale pop effect on selected
            float scale = (i == selectedIdx) ? 1.08f : 1f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
        }
    }

    // ─── Stats ─────────────────────────────────────────────────────────────────

    private void updateStatCards() {
        View root = getWindow().getDecorView().getRootView();
        String dominant = getDominant(globalCategoryCount);
        int domCount = globalCategoryCount.getOrDefault(dominant, 0);
        int pct = totalReports == 0 ? 0 : Math.round((domCount * 100f) / totalReports);

        bindMetric(findViewById(R.id.metricKelurahan),
                R.drawable.ic_user_outline, String.valueOf(rtCount.size()), "RT Aktif");
        bindMetric(findViewById(R.id.metricLaporan),
                R.drawable.ic_document_outline, String.valueOf(totalReports), "Total Scan");
    }

    private void buildCategoryBreakdown() {
        if (categoryBreakdownContainer == null) return;
        categoryBreakdownContainer.removeAllViews();

        // Which counts to use: all or filtered
        Map<String, Integer> counts;
        int totalForPct;
        if (activeFilter != null) {
            counts = new HashMap<>();
            int c = globalCategoryCount.getOrDefault(activeFilter, 0);
            counts.put(activeFilter, c);
            totalForPct = totalReports;
        } else {
            counts = globalCategoryCount;
            totalForPct = totalReports;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        for (Map.Entry<String, Integer> entry : sorted) {
            String nama = entry.getKey();
            int count = entry.getValue();
            int color = colorForNama(nama);
            int pct = totalForPct == 0 ? 0 : Math.round((count * 100f) / totalForPct);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));

            // Color dot
            FrameLayout dot = new FrameLayout(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
            dotParams.setMarginEnd(dp(10));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(color);
            dot.setBackground(dotBg);

            // Nama
            TextView tvNama = new TextView(this);
            tvNama.setText(nama);
            tvNama.setTextColor(Color.parseColor("#424242"));
            tvNama.setTextSize(13f);
            LinearLayout.LayoutParams namaParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvNama.setLayoutParams(namaParams);

            // Count + pct
            TextView tvCount = new TextView(this);
            tvCount.setText(count + " item  " + pct + "%");
            tvCount.setTextColor(color);
            tvCount.setTextSize(12f);
            tvCount.setTypeface(null, Typeface.BOLD);

            row.addView(dot);
            row.addView(tvNama);
            row.addView(tvCount);
            categoryBreakdownContainer.addView(row);

            // Divider
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
            categoryBreakdownContainer.addView(divider);
        }

        if (sorted.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada data sampah");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setTextSize(13f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(16), 0, dp(16));
            categoryBreakdownContainer.addView(empty);
        }
    }

    private void bindMetric(View metric, int iconRes, String value, String label) {
        if (metric == null) return;
        ImageView icon = metric.findViewById(R.id.ivMetricIcon);
        TextView val = metric.findViewById(R.id.tvMetricValue);
        TextView lab = metric.findViewById(R.id.tvMetricLabel);
        if (icon != null) icon.setImageResource(iconRes);
        if (val != null) val.setText(value);
        if (lab != null) lab.setText(label);
    }
    //---------------DETAIL MARKER-------------------------
    private void showMarkerDetail(
            String jenis,
            String kategori,
            String alamat,
            String rt,
            String rw,
            int totalArea,
            String dominanArea) {

        if (tvDetailContent == null) return;

        String detail =
                "Jenis Sampah : " + jenis +
                        "\nKategori : " + kategori +
                        "\nAlamat : " + alamat +
                        "\nWilayah : " + rt + " / " + rw +
                        "\nTotal Scan Area : " + totalArea +
                        "\nSampah Dominan : " + dominanArea;

        tvDetailContent.setText(detail);

        if (scrollView != null && detailMarkerCard != null) {
            scrollView.post(() ->
                    scrollView.smoothScrollTo(0, detailMarkerCard.getTop())
            );
        }
    }

    // ─── Top RT ────────────────────────────────────────────────────────────────

    private List<Map.Entry<String, Integer>> sortedRtList() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(rtCount.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list;
    }

    private void buildTopRegions(List<Map.Entry<String, Integer>> list) {
        if (topRegionRow == null) return;
        topRegionRow.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < 3; i++) {
            View item = inf.inflate(R.layout.item_top_kelurahan, topRegionRow, false);
            TextView rank = item.findViewById(R.id.tvRank);
            TextView name = item.findViewById(R.id.tvTopName);
            TextView total = item.findViewById(R.id.tvTopTotal);
            if (rank != null) {
                rank.setText(String.valueOf(i + 1));
                rank.setBackgroundResource(i == 0 ? R.drawable.bg_rank_badge_green
                        : i == 1 ? R.drawable.bg_rank_badge_blue
                        : R.drawable.bg_rank_badge_orange);
            }
            if (i < list.size()) {
                Map.Entry<String, Integer> e = list.get(i);
                if (name != null) name.setText(e.getKey());
                if (total != null) total.setText(e.getValue() + " scan");
            } else {
                if (name != null) name.setText("-");
                if (total != null) total.setText("0");
            }
            topRegionRow.addView(item);
        }
    }

    // ─── Canvas: lingkaran berwarna + ikon sampah ──────────────────────────────

    private Bitmap createPinBitmap(String nama, int color) {
        int size = dp(52);
        int total = size + dp(10);
        Bitmap bmp = Bitmap.createBitmap(total, total, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        float cx = total / 2f;
        float cy = size / 2f;
        float r  = size / 2f - dp(2);

        // Shadow
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(withAlpha(Color.BLACK, 40));
        canvas.drawCircle(cx + dp(1.5f), cy + dp(2), r, shadow);

        // Filled circle (category color)
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        canvas.drawCircle(cx, cy, r, fill);

        // White border ring
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2.5f));
        canvas.drawCircle(cx, cy, r - dp(1), strokePaint);

        // Pointer tail
        Paint tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setColor(color);
        Path tail = new Path();
        float tailW   = dp(7);
        float tailTop = cy + r - dp(3);
        float tailBot = cy + r + dp(8);
        tail.moveTo(cx - tailW, tailTop);
        tail.lineTo(cx + tailW, tailTop);
        tail.lineTo(cx, tailBot);
        tail.close();
        canvas.drawPath(tail, tailPaint);

        // Category icon (white tint)
        int iconRes = iconFor(nama);
        Drawable d = ContextCompat.getDrawable(this, iconRes);
        if (d != null) {
            d = DrawableCompat.wrap(d).mutate();
            DrawableCompat.setTint(d, Color.WHITE);
            int iconSize = dp(26);
            int left = (int)(cx - iconSize / 2f);
            int top  = (int)(cy - iconSize / 2f);
            d.setBounds(left, top, left + iconSize, top + iconSize);
            d.draw(canvas);
        }

        return bmp;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private List<GeoPoint> circlePoints(double lat, double lng, double radiusM) {
        List<GeoPoint> pts = new ArrayList<>();
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double angle = Math.toRadians(i * 360.0 / segments);
            double dLat = (radiusM / 111320.0) * Math.cos(angle);
            double dLng = (radiusM / (111320.0 * Math.cos(Math.toRadians(lat)))) * Math.sin(angle);
            pts.add(new GeoPoint(lat + dLat, lng + dLng));
        }
        return pts;
    }

    private int colorForNama(String nama) {
        if (nama == null) return Color.parseColor("#9E9E9E");
        switch (nama.toLowerCase()) {
            case "organik":      return Color.parseColor("#4CAF50");
            case "plastik":      return Color.parseColor("#FF9800");
            case "kertas":       return Color.parseColor("#FFC107");
            case "kaca":         return Color.parseColor("#00BCD4");
            case "kardus":       return Color.parseColor("#2196F3");
            case "logam":        return Color.parseColor("#9C27B0");
            case "bukan sampah": return Color.parseColor("#FF5252"); // merah terang bukan sampah
            default:             return Color.parseColor("#F44336");
        }
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.65f;
        return Color.HSVToColor(hsv);
    }

    private int softColor(int color) {
        return Color.rgb(
                Math.min(255, (int) (Color.red(color) * 0.18f + 230)),
                Math.min(255, (int) (Color.green(color) * 0.18f + 230)),
                Math.min(255, (int) (Color.blue(color) * 0.18f + 230)));
    }

    private String getDominant(Map<String, Integer> counts) {
        String dom = "-";
        int max = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet())
            if (e.getValue() > max) {
                max = e.getValue();
                dom = e.getKey();
        }
        return dom;
    }

    private String getDominantNearby(double lat, double lng) {
        Map<String, Integer> nearby = new HashMap<>();
        float[] result = new float[1];
        for (ScanHistory s : displayedScans) {
            if (s.getLatitude() == null || s.getLongitude() == null) continue;
            if (s.getLatitude() == 0.0 || s.getLongitude() == 0.0) continue;
            android.location.Location.distanceBetween(lat, lng, s.getLatitude(), s.getLongitude(), result);
            if (result[0] <= ZONE_RADIUS_METERS && s.getJenisSampah() != null) {
                String jenis = WilayahUtils.normalizeJenis(s.getJenisSampah());
                nearby.put(jenis, nearby.getOrDefault(jenis, 0) + 1);
            }
        }
        if (nearby.isEmpty()) return "-";
        return getDominant(nearby);
    }

    private int countNearby(double lat, double lng) {
        int count = 0;
        float[] result = new float[1];
        for (ScanHistory s : displayedScans) {
            if (s.getLatitude() == null || s.getLongitude() == null) continue;
            if (s.getLatitude() == 0.0 || s.getLongitude() == 0.0) continue;
            android.location.Location.distanceBetween(lat, lng, s.getLatitude(), s.getLongitude(), result);
            if (result[0] <= ZONE_RADIUS_METERS) count++;
        }
        return count;
    }

    private void fetchAddressAsync(double lat, double lng, Marker marker) {
        geocodeExecutor.submit(() -> {
            String address = reverseGeocode(lat, lng);
            final String finalAddress = (address != null && !address.isEmpty()) ? address : "tidak tersedia";

            runOnUiThread(() -> {
                if (mapView == null) return;
                String current = marker.getSnippet();
                if (current != null) {
                    String updated = current.replaceFirst("Alamat:.*?(\\n|$)", "Alamat: " + finalAddress + "\n");
                    boolean wasOpen = marker.isInfoWindowShown();
                    marker.setSnippet(updated);
                    if (wasOpen) {
                        marker.closeInfoWindow();
                        marker.showInfoWindow();
                    }
                }
                mapView.invalidate();
            });
        });
    }

    private String reverseGeocode(double lat, double lng) {
        String address = "";
        try {
            String urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lng;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "EcoSnap/1.0 Android");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject json = new JSONObject(sb.toString());
            JSONObject addr = json.optJSONObject("address");

            if (addr != null) {
                String road = addr.optString("road", "");
                String neighbourhood = addr.optString("neighbourhood", "");
                String village = addr.optString("village", "");
                String suburb = addr.optString("suburb", "");
                String town = addr.optString("town", "");
                String city = addr.optString("city", "");
                String county = addr.optString("county", "");

                StringBuilder sb2 = new StringBuilder();
                if (!road.isEmpty()) sb2.append(road);

                String area = !neighbourhood.isEmpty() ? neighbourhood :
                        (!village.isEmpty() ? village :
                                (!suburb.isEmpty() ? suburb : ""));

                if (!area.isEmpty()) {
                    if (sb2.length() > 0) sb2.append(", ");
                    sb2.append(area);
                }

                String cityArea = !town.isEmpty() ? town :
                        (!city.isEmpty() ? city : county);

                if (!cityArea.isEmpty()) {
                    if (sb2.length() > 0) sb2.append(", ");
                    sb2.append(cityArea);
                }

                address = sb2.toString().trim().replaceAll(",$", "");
            }

            if (address.isEmpty()) {
                address = json.optString("display_name", "").split(",")[0].trim();
            }
        } catch (Exception e) {
            address = "";
        }

        return address;
    }

    private int iconFor(String cat) {
        if (cat == null) return R.drawable.ic_leaf;
        switch (cat.toLowerCase()) {
            case "kardus":       return R.drawable.ic_box_outline;
            case "kaca":         return R.drawable.ic_bottle_outline;
            case "logam":        return R.drawable.ic_can_outline;
            case "kertas":       return R.drawable.ic_document_outline;
            case "plastik":      return R.drawable.ic_plastic_bottle_outline;
            case "bukan sampah": return R.drawable.ic_bukan_sampah;
            default:             return R.drawable.ic_leaf;
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void animatePage() {
        View root = getWindow().getDecorView().getRootView();
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(320L)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private String formatRwId(String rwId) {
        if (rwId == null) return "";
        if (rwId.equals("001") || rwId.equals("01")) return "RW 01";
        if (rwId.equals("002") || rwId.equals("02")) return "RW 02";
        if (rwId.equals("003") || rwId.equals("03")) return "RW 03";
        if (rwId.equals("004") || rwId.equals("04")) return "RW 04";
        if (rwId.equals("005") || rwId.equals("05")) return "RW 05";
        return rwId;
    }

    private String parseRwLabel(String label) {
        if (label == null) return "";
        if (label.equals("RW 01")) return "001";
        if (label.equals("RW 02")) return "002";
        if (label.equals("RW 03")) return "003";
        if (label.equals("RW 04")) return "004";
        if (label.equals("RW 05")) return "005";
        return label;
    }

    private boolean isMatchingRw(String scanRwId, String filterRwId) {
        if (scanRwId == null || filterRwId == null) return false;
        return normalizeRw(scanRwId).equals(normalizeRw(filterRwId));
    }

    private String normalizeRw(String rw) {
        if (rw == null) return "";
        String clean = rw.replace("RW", "").replace("rw", "").trim();
        try {
            int num = Integer.parseInt(clean);
            return String.valueOf(num);
        } catch (NumberFormatException e) {
            return clean.toLowerCase();
        }
    }

    private String formatRtId(String rtId) {
        if (rtId == null) return "";
        String clean = rtId.replace("RT", "").replace("rt", "").trim();
        if (clean.isEmpty()) return "";
        try {
            return "RT " + String.format("%02d", Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toUpperCase().startsWith("RT") ? clean : "RT " + clean;
        }
    }

    private String parseRtLabel(String label) {
        if (label == null) return "";
        if (label.equals("RT 01")) return "001";
        if (label.equals("RT 02")) return "002";
        if (label.equals("RT 03")) return "003";
        if (label.equals("RT 04")) return "004";
        if (label.equals("RT 05")) return "005";
        return label;
    }

    private boolean isMatchingRt(String scanRtId, String filterRtId) {
        if (scanRtId == null || filterRtId == null) return false;
        return normalizeRt(scanRtId).equals(normalizeRt(filterRtId));
    }

    private String normalizeRt(String rt) {
        if (rt == null) return "";
        String clean = rt.replace("RT", "").replace("rt", "").trim();
        try {
            int num = Integer.parseInt(clean);
            return String.valueOf(num);
        } catch (NumberFormatException e) {
            return clean.toLowerCase();
        }
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!geocodeExecutor.isShutdown()) {
            geocodeExecutor.shutdown();
        }
    }
}
