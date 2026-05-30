package com.example.ecosnap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsFragment extends Fragment {

    private static final String[] CATEGORIES = {"Organik", "Plastik", "Kertas", "Kaca", "Kardus", "Logam", "Bukan Sampah"};
    private static final double ZONE_RADIUS_METERS = 80.0; // radius lingkaran zona (meter)

    // Views
    private MapView osmMap;
    private LinearLayout legendRow;
    private LinearLayout topRegionRow;
    private LinearLayout categoryBreakdownContainer;
    private LinearLayout filterBadgeRow;
    private TextView tvFilterBadge;
    private TextView btnResetFilter;

    // OSMDroid
    private MyLocationNewOverlay myLocationOverlay;

    // Location
    private FusedLocationProviderClient fusedLocationClient;

    // Data
    private int totalReports = 0;
    private List<ScanHistory> allScans = new ArrayList<>();
    private final List<ScanHistory> displayedScans = new ArrayList<>();
    private final Map<String, Integer> globalCategoryCount = new HashMap<>();
    private final Map<String, Integer> rtCount = new HashMap<>();

    // Filter state
    private String activeFilter = null;
    private String activePeriodFilter = "Bulan Ini";
    private String activeRtFilter = "Semua RT";
    private TextView tvFilterPeriod;
    private TextView tvFilterArea;
    private final List<View> legendItemViews = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_maps, container, false);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        osmMap                     = view.findViewById(R.id.osmMapView);
        legendRow                  = view.findViewById(R.id.legendRow);
        topRegionRow               = view.findViewById(R.id.topRegionRow);
        categoryBreakdownContainer = view.findViewById(R.id.categoryBreakdownContainer);
        filterBadgeRow             = view.findViewById(R.id.filterBadgeRow);
        tvFilterBadge              = view.findViewById(R.id.tvFilterBadge);
        btnResetFilter             = view.findViewById(R.id.btnResetFilter);
        tvFilterPeriod             = view.findViewById(R.id.tvFilterPeriod);
        tvFilterArea               = view.findViewById(R.id.tvFilterArea);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Minta izin lokasi runtime
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1001);
        }

        setupMap();
        setupControls(view);
        buildLegend();
        buildTopRegions(new ArrayList<>());
        animatePage(view);

        // Dropdown listeners
        View btnFilterPeriod = view.findViewById(R.id.btnFilterPeriod);
        View btnFilterArea = view.findViewById(R.id.btnFilterArea);

        if (btnFilterPeriod != null) {
            btnFilterPeriod.setOnClickListener(v -> {
                if (getContext() == null) return;
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(getContext(), btnFilterPeriod);
                popup.getMenu().add("Bulan Ini");
                popup.getMenu().add("Semua Waktu");
                popup.setOnMenuItemClickListener(item -> {
                    activePeriodFilter = item.getTitle().toString();
                    if (tvFilterPeriod != null) tvFilterPeriod.setText(activePeriodFilter);
                    applyFilters();
                    return true;
                });
                popup.show();
            });
        }

        if (btnFilterArea != null) {
            btnFilterArea.setOnClickListener(v -> {
                if (getContext() == null) return;
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(getContext(), btnFilterArea);
                popup.getMenu().add("Semua RT");

                List<String> rts = new ArrayList<>();
                for (ScanHistory s : allScans) {
                    if (s.getRtId() != null && !s.getRtId().isEmpty() && !rts.contains(s.getRtId())) {
                        rts.add(s.getRtId());
                    }
                }
                Collections.sort(rts);
                for (String r : rts) {
                    popup.getMenu().add(r);
                }

                popup.setOnMenuItemClickListener(item -> {
                    activeRtFilter = item.getTitle().toString();
                    if (tvFilterArea != null) tvFilterArea.setText(activeRtFilter);
                    applyFilters();
                    return true;
                });
                popup.show();
            });
        }

        // Reset filter
        if (btnResetFilter != null) {
            btnResetFilter.setOnClickListener(v -> {
                activeFilter = null;
                if (filterBadgeRow != null) filterBadgeRow.setVisibility(View.GONE);
                refreshLegendSelection(-1);
                refreshMapMarkers();
                buildCategoryBreakdown();
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (osmMap != null) osmMap.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        loadDataFromSupabase();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (osmMap != null) osmMap.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
    }

    // ─── Setup OSMDroid ───────────────────────────────────────────────────────

    private void setupMap() {
        if (osmMap == null) return;
        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setMultiTouchControls(true);
        osmMap.setBuiltInZoomControls(false);

        osmMap.getController().setZoom(14.0);
        osmMap.getController().setCenter(new GeoPoint(-6.8900, 107.5400));

        // Overlay titik biru GPS user
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()), osmMap);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        osmMap.getOverlays().add(myLocationOverlay);
    }

    private void setupControls(View root) {
        View btnLocate  = root.findViewById(R.id.btnLocate);
        TextView zoomIn  = root.findViewById(R.id.btnZoomIn);
        TextView zoomOut = root.findViewById(R.id.btnZoomOut);

        if (btnLocate != null) btnLocate.setOnClickListener(v -> centerOnMyLocation());
        if (zoomIn    != null) zoomIn.setOnClickListener(v  -> osmMap.getController().zoomIn());
        if (zoomOut   != null) zoomOut.setOnClickListener(v -> osmMap.getController().zoomOut());
    }

    private void centerOnMyLocation() {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            osmMap.getController().animateTo(myLocationOverlay.getMyLocation());
            osmMap.getController().setZoom(17.0);
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    osmMap.getController().animateTo(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                    osmMap.getController().setZoom(17.0);
                } else {
                    Toast.makeText(getContext(), "Lokasi belum tersedia", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ─── Supabase ─────────────────────────────────────────────────────────────

    private void loadDataFromSupabase() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getAllScans().enqueue(new Callback<List<ScanHistory>>() {
            @Override
            public void onResponse(Call<List<ScanHistory>> call,
                                   Response<List<ScanHistory>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    processData(response.body());
                } else {
                    showError("Gagal memuat data peta");
                }
            }
            @Override
            public void onFailure(Call<List<ScanHistory>> call, Throwable t) {
                if (isAdded()) showError("Tidak dapat terhubung ke server");
            }
        });
    }

    private void processData(List<ScanHistory> list) {
        allScans = list;
        applyFilters();
    }

    private void applyFilters() {
        displayedScans.clear();
        String currentMonthStr = getCurrentYearMonth();

        for (ScanHistory s : allScans) {
            // 1. Period filter
            boolean matchesPeriod = true;
            if ("Bulan Ini".equals(activePeriodFilter)) {
                matchesPeriod = s.getCreatedAt() != null && s.getCreatedAt().startsWith(currentMonthStr);
            }

            // 2. RT filter
            boolean matchesRt = true;
            if (!"Semua RT".equals(activeRtFilter)) {
                matchesRt = s.getRtId() != null && activeRtFilter.equalsIgnoreCase(s.getRtId());
            }

            if (matchesPeriod && matchesRt) {
                displayedScans.add(s);
            }
        }

        totalReports = displayedScans.size();
        globalCategoryCount.clear();
        rtCount.clear();

        for (ScanHistory s : displayedScans) {
            String nama = s.getJenisSampah();
            String rt   = s.getRtId();
            if (nama != null) globalCategoryCount.put(nama,
                    globalCategoryCount.getOrDefault(nama, 0) + 1);
            if (rt != null && !rt.isEmpty()) rtCount.put(rt,
                    rtCount.getOrDefault(rt, 0) + 1);
        }

        refreshMapMarkers();
        updateStatCards();
        buildTopRegions(sortedRtList());
        buildLegend();
    }

    private String getCurrentYearMonth() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US);
        return sdf.format(new java.util.Date());
    }

    // ─── Map Markers with optional filter ─────────────────────────────────────

    private void refreshMapMarkers() {
        if (osmMap == null || !isAdded()) return;
        osmMap.getOverlays().removeIf(o -> (o instanceof Marker) || (o instanceof Polygon));

        List<ScanHistory> toShow = (activeFilter == null) ? displayedScans : new ArrayList<>();
        if (activeFilter != null) {
            for (ScanHistory s : displayedScans) {
                if (activeFilter.equalsIgnoreCase(s.getJenisSampah())) toShow.add(s);
            }
        }

        for (ScanHistory s : toShow) {
            if (s.getLatitude() != null && s.getLongitude() != null
                    && s.getLatitude() != 0.0 && s.getLongitude() != 0.0) {
                addScanPinAndZone(s);
            }
        }
        osmMap.invalidate();
    }

    private void refreshLegendSelection(int selectedIdx) {
        for (int i = 0; i < legendItemViews.size(); i++) {
            View v = legendItemViews.get(i);
            v.setAlpha(selectedIdx < 0 || i == selectedIdx ? 1f : 0.4f);
            float scale = (i == selectedIdx) ? 1.08f : 1f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(180).start();
        }
    }

    // ─── Pin berwarna + lingkaran zona ────────────────────────────────────────

    private void addScanPinAndZone(ScanHistory scan) {
        double lat  = scan.getLatitude();
        double lng  = scan.getLongitude();
        String nama = scan.getJenisSampah() != null ? scan.getJenisSampah() : "Sampah";
        String kat  = scan.getKategori()    != null ? scan.getKategori()    : "-";
        String rt   = scan.getRtId()        != null ? scan.getRtId()        : "-";
        String rw   = scan.getRwId()        != null ? scan.getRwId()        : "-";
        int    color = colorForNama(nama);

        // 1. Lingkaran zona semi-transparan
        Polygon zone = new Polygon(osmMap);
        zone.setPoints(circlePoints(lat, lng, ZONE_RADIUS_METERS));
        zone.setFillColor(withAlpha(color, 45));
        zone.setStrokeColor(withAlpha(color, 160));
        zone.setStrokeWidth(2.5f);
        osmMap.getOverlays().add(zone);

        // 2. Pin ikon kategori dengan koordinat presisi 6 desimal di InfoWindow
        Marker marker = new Marker(osmMap);
        marker.setPosition(new GeoPoint(lat, lng));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(nama + " — " + rt + " / " + rw);
        // Koordinat ditampilkan dengan presisi 6 desimal
        marker.setSnippet(String.format(Locale.US,
                "Jenis: %s | Kategori: %s\nLat: %.6f | Lng: %.6f\nWilayah: %s / %s",
                nama, kat, lat, lng, rt, rw));
        marker.setIcon(new android.graphics.drawable.BitmapDrawable(
                getResources(), createPinBitmap(nama, color)));
        osmMap.getOverlays().add(marker);
    }


    // ─── Canvas: lingkaran berwarna + ikon sampah ────────────────────────────

    private Bitmap createPinBitmap(String nama, int color) {
        int size = dp(52);
        int total = size + dp(10); // extra space for shadow tail
        Bitmap bmp = Bitmap.createBitmap(total, total, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        float cx = total / 2f;
        float cy = size / 2f;
        float r  = size / 2f - dp(2);

        // Shadow drop
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(withAlpha(Color.BLACK, 40));
        canvas.drawCircle(cx + dp(1.5f), cy + dp(2), r, shadow);

        // Filled circle (category color)
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        canvas.drawCircle(cx, cy, r, fill);

        // White border ring
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(Color.WHITE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(2.5f));
        canvas.drawCircle(cx, cy, r - dp(1), stroke);

        // Tail / pointer
        Paint tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setColor(color);
        Path tail = new Path();
        float tailW = dp(7);
        float tailTop = cy + r - dp(3);
        float tailBot = cy + r + dp(8);
        tail.moveTo(cx - tailW, tailTop);
        tail.lineTo(cx + tailW, tailTop);
        tail.lineTo(cx, tailBot);
        tail.close();
        canvas.drawPath(tail, tailPaint);

        // Category icon (white tint)
        if (isAdded() && getContext() != null) {
            int iconRes = iconFor(nama);
            Drawable d = ContextCompat.getDrawable(requireContext(), iconRes);
            if (d != null) {
                d = DrawableCompat.wrap(d).mutate();
                DrawableCompat.setTint(d, Color.WHITE);
                int iconSize = dp(26);
                int left  = (int)(cx - iconSize / 2f);
                int top   = (int)(cy - iconSize / 2f);
                d.setBounds(left, top, left + iconSize, top + iconSize);
                d.draw(canvas);
            }
        }

        return bmp;
    }

    // ─── Hitung titik-titik lingkaran (Polygon) ───────────────────────────────

    private List<GeoPoint> circlePoints(double lat, double lng, double radiusM) {
        List<GeoPoint> pts = new ArrayList<>();
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double angle  = Math.toRadians(i * 360.0 / segments);
            double dLat   = (radiusM / 111320.0) * Math.cos(angle);
            double dLng   = (radiusM / (111320.0 * Math.cos(Math.toRadians(lat)))) * Math.sin(angle);
            pts.add(new GeoPoint(lat + dLat, lng + dLng));
        }
        return pts;
    }

    // ─── Warna berdasarkan jenis sampah ──────────────────────────────────────

    private int colorForNama(String nama) {
        if (nama == null) return Color.parseColor("#9E9E9E");
        switch (nama.toLowerCase()) {
            case "organik":      return Color.parseColor("#4CAF50"); // hijau
            case "plastik":      return Color.parseColor("#FF9800"); // oranye
            case "kertas":       return Color.parseColor("#FFC107"); // kuning amber
            case "kaca":         return Color.parseColor("#00BCD4"); // cyan
            case "kardus":       return Color.parseColor("#2196F3"); // biru
            case "logam":        return Color.parseColor("#9C27B0"); // ungu
            case "bukan sampah": return Color.parseColor("#FF5252"); // merah terang bukan sampah
            default:             return Color.parseColor("#F44336"); // merah
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

    // ─── Stat cards ───────────────────────────────────────────────────────────

    private void updateStatCards() {
        if (!isAdded() || getView() == null) return;
        String dominant = getDominant(globalCategoryCount);
        int domCount = globalCategoryCount.getOrDefault(dominant, 0);
        int pct = totalReports == 0 ? 0 : Math.round((domCount * 100f) / totalReports);

        bindMetric(getView().findViewById(R.id.metricKelurahan),
                R.drawable.ic_user_outline, String.valueOf(rtCount.size()), "RT Aktif");
        bindMetric(getView().findViewById(R.id.metricLaporan),
                R.drawable.ic_document_outline, String.valueOf(totalReports), "Total Scan");
        bindMetric(getView().findViewById(R.id.metricDominan),
                iconFor(dominant), dominant, "Dominan");

        // Update category breakdown detail
        buildCategoryBreakdown();
    }

    private void buildCategoryBreakdown() {
        if (categoryBreakdownContainer == null || getContext() == null) return;
        categoryBreakdownContainer.removeAllViews();

        // Jika filter aktif: tampilkan hanya kategori itu
        Map<String, Integer> counts;
        if (activeFilter != null) {
            counts = new HashMap<>();
            int c = globalCategoryCount.getOrDefault(activeFilter, 0);
            counts.put(activeFilter, c);
        } else {
            counts = globalCategoryCount;
        }

        // Sort by count descending
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        for (Map.Entry<String, Integer> entry : sorted) {
            String nama = entry.getKey();
            int count = entry.getValue();
            int color = colorForNama(nama);
            int pct = totalReports == 0 ? 0 : Math.round((count * 100f) / totalReports);

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 10, 0, 10);

            // Color dot
            android.widget.FrameLayout dot = new android.widget.FrameLayout(getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
            dotParams.setMarginEnd(dp(10));
            dot.setLayoutParams(dotParams);
            android.graphics.drawable.GradientDrawable dotBg = new android.graphics.drawable.GradientDrawable();
            dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dotBg.setColor(color);
            dot.setBackground(dotBg);

            // Nama
            TextView tvNama = new TextView(getContext());
            tvNama.setText(nama);
            tvNama.setTextColor(android.graphics.Color.parseColor("#424242"));
            tvNama.setTextSize(13f);
            LinearLayout.LayoutParams namaParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvNama.setLayoutParams(namaParams);

            // Count
            TextView tvCount = new TextView(getContext());
            tvCount.setText(count + " item  " + pct + "%");
            tvCount.setTextColor(color);
            tvCount.setTextSize(12f);
            tvCount.setTypeface(null, Typeface.BOLD);

            row.addView(dot);
            row.addView(tvNama);
            row.addView(tvCount);
            categoryBreakdownContainer.addView(row);

            // Divider
            View divider = new View(getContext());
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"));
            categoryBreakdownContainer.addView(divider);
        }

        if (sorted.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Belum ada data sampah");
            empty.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            empty.setTextSize(13f);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, dp(16), 0, dp(16));
            categoryBreakdownContainer.addView(empty);
        }
    }

    private void bindMetric(View metric, int iconRes, String value, String label) {
        if (metric == null) return;
        ImageView icon = metric.findViewById(R.id.ivMetricIcon);
        TextView  val  = metric.findViewById(R.id.tvMetricValue);
        TextView  lab  = metric.findViewById(R.id.tvMetricLabel);
        if (icon != null) icon.setImageResource(iconRes);
        if (val  != null) val.setText(value);
        if (lab  != null) lab.setText(label);
    }

    // ─── Legenda ──────────────────────────────────────────────────────────────

    private void buildLegend() {
        if (legendRow == null || getContext() == null) return;
        legendRow.removeAllViews();
        legendItemViews.clear();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            final int idx = i;

            View item      = inflater.inflate(R.layout.item_legend_category, legendRow, false);
            FrameLayout circle   = item.findViewById(R.id.legendCircle);
            ImageView icon       = item.findViewById(R.id.ivLegendIcon);
            TextView label       = item.findViewById(R.id.tvLegendLabel);
            TextView tvPercent   = item.findViewById(R.id.tvLegendPercent);

            int color = colorForNama(cat);
            if (circle != null) circle.setBackgroundTintList(ColorStateList.valueOf(softColor(color)));
            if (icon != null) {
                icon.setImageResource(iconFor(cat));
                icon.setImageTintList(ColorStateList.valueOf(color));
            }
            if (label != null) label.setText(cat);

            // Tampilkan persentase jika data sudah ada
            if (tvPercent != null && totalReports > 0) {
                int count = globalCategoryCount.getOrDefault(cat, 0);
                int pct = Math.round((count * 100f) / totalReports);
                tvPercent.setText(pct + "%");
                tvPercent.setTextColor(color);
                tvPercent.setVisibility(View.VISIBLE);
            } else if (tvPercent != null) {
                tvPercent.setVisibility(View.GONE);
            }

            legendItemViews.add(item);
            legendRow.addView(item);

            // Click: toggle filter peta ke kategori ini
            item.setOnClickListener(v -> {
                if (cat.equals(activeFilter)) {
                    // Klik lagi → reset
                    activeFilter = null;
                    if (filterBadgeRow != null) filterBadgeRow.setVisibility(View.GONE);
                    refreshLegendSelection(-1);
                } else {
                    activeFilter = cat;
                    if (tvFilterBadge != null) tvFilterBadge.setText("Filter aktif: " + cat);
                    if (filterBadgeRow != null) filterBadgeRow.setVisibility(View.VISIBLE);
                    refreshLegendSelection(idx);
                }
                refreshMapMarkers();
                buildCategoryBreakdown();
            });
        }
    }

    // ─── Top RT ───────────────────────────────────────────────────────────────

    private List<Map.Entry<String, Integer>> sortedRtList() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(rtCount.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list;
    }

    private void buildTopRegions(List<Map.Entry<String, Integer>> list) {
        if (topRegionRow == null || getContext() == null) return;
        topRegionRow.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(getContext());
        for (int i = 0; i < 3; i++) {
            View item    = inf.inflate(R.layout.item_top_kelurahan, topRegionRow, false);
            TextView rank  = item.findViewById(R.id.tvRank);
            TextView name  = item.findViewById(R.id.tvTopName);
            TextView total = item.findViewById(R.id.tvTopTotal);
            if (rank != null) {
                rank.setText(String.valueOf(i + 1));
                rank.setBackgroundResource(i == 0 ? R.drawable.bg_rank_badge_green
                        : i == 1 ? R.drawable.bg_rank_badge_blue
                        : R.drawable.bg_rank_badge_orange);
            }
            if (i < list.size()) {
                Map.Entry<String, Integer> e = list.get(i);
                if (name  != null) name.setText(e.getKey());
                if (total != null) total.setText(e.getValue() + " scan");
            } else {
                if (name  != null) name.setText("-");
                if (total != null) total.setText("0");
            }
            topRegionRow.addView(item);
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String getDominant(Map<String, Integer> counts) {
        String dom = "-"; int max = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet())
            if (e.getValue() > max) { max = e.getValue(); dom = e.getKey(); }
        return dom;
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

    private int softColor(int color) {
        return Color.rgb(
                Math.min(255, (int) (Color.red(color)   * 0.18f + 230)),
                Math.min(255, (int) (Color.green(color) * 0.18f + 230)),
                Math.min(255, (int) (Color.blue(color)  * 0.18f + 230)));
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void animatePage(View root) {
        root.setAlpha(0f);
        root.setTranslationY(20f);
        root.animate().alpha(1f).translationY(0f)
                .setDuration(320L).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void showError(String msg) {
        if (isAdded() && getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
