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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsFragment extends Fragment {

    private static final String[] CATEGORIES = {"Organik", "Plastik", "Kertas", "Kaca", "Kardus", "Logam"};
    private static final double ZONE_RADIUS_METERS = 80.0; // radius lingkaran zona (meter)

    // Views
    private MapView osmMap;
    private LinearLayout legendRow;
    private LinearLayout topRegionRow;

    // OSMDroid
    private MyLocationNewOverlay myLocationOverlay;

    // Location
    private FusedLocationProviderClient fusedLocationClient;

    // Data stats
    private int totalReports = 0;
    private final Map<String, Integer> globalCategoryCount = new HashMap<>();
    private final Map<String, Integer> rtCount = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_maps, container, false);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        osmMap       = view.findViewById(R.id.osmMapView);
        legendRow    = view.findViewById(R.id.legendRow);
        topRegionRow = view.findViewById(R.id.topRegionRow);

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
        totalReports = list.size();
        globalCategoryCount.clear();
        rtCount.clear();

        // Hapus semua marker & polygon lama (kecuali GPS overlay)
        osmMap.getOverlays().removeIf(o -> (o instanceof Marker) || (o instanceof Polygon));

        for (ScanHistory s : list) {
            String nama = s.getJenisSampah();
            String rt   = s.getRtId();

            if (nama != null) globalCategoryCount.put(nama,
                    globalCategoryCount.getOrDefault(nama, 0) + 1);
            if (rt   != null && !rt.isEmpty()) rtCount.put(rt,
                    rtCount.getOrDefault(rt, 0) + 1);

            // Tambah marker + zona hanya jika ada koordinat valid
            if (s.getLatitude()  != null && s.getLongitude() != null
                    && s.getLatitude()  != 0.0
                    && s.getLongitude() != 0.0) {
                addScanPinAndZone(s);
            }
        }

        osmMap.invalidate();
        updateStatCards();
        buildTopRegions(sortedRtList());
    }

    // ─── Pin berwarna + lingkaran zona ────────────────────────────────────────

    private void addScanPinAndZone(ScanHistory scan) {
        double lat  = scan.getLatitude();
        double lng  = scan.getLongitude();
        String nama = scan.getJenisSampah() != null ? scan.getJenisSampah() : "Sampah";
        String kat  = scan.getKategori()    != null ? scan.getKategori()    : "-";
        int    color = colorForNama(nama);

        // 1. Lingkaran zona semi-transparan
        Polygon zone = new Polygon(osmMap);
        zone.setPoints(circlePoints(lat, lng, ZONE_RADIUS_METERS));
        zone.setFillColor(withAlpha(color, 45));     // sangat transparan
        zone.setStrokeColor(withAlpha(color, 160));  // outline sedikit lebih solid
        zone.setStrokeWidth(2.5f);
        osmMap.getOverlays().add(zone);

        // 2. Pin berwarna custom (gambar via Canvas)
        Marker marker = new Marker(osmMap);
        marker.setPosition(new GeoPoint(lat, lng));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(nama);
        marker.setSnippet("Kategori: " + kat);
        marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(color)));
        osmMap.getOverlays().add(marker);
    }

    // ─── Canvas: gambar custom pin tearDrop ───────────────────────────────────

    private Bitmap createPinBitmap(int color) {
        int w = dp(36);
        int h = dp(52);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        float cx = w / 2f;
        float r  = w * 0.42f;           // radius lingkaran kepala

        Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFill.setColor(color);
        paintFill.setStyle(Paint.Style.FILL);

        Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStroke.setColor(withAlpha(darken(color), 200));
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(dp(1.5f));

        Paint paintWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintWhite.setColor(Color.WHITE);
        paintWhite.setStyle(Paint.Style.FILL);

        // Ekor / ujung pin (segitiga ke bawah)
        Path tail = new Path();
        tail.moveTo(cx - r * 0.5f, r * 1.05f);
        tail.lineTo(cx + r * 0.5f, r * 1.05f);
        tail.lineTo(cx, h - dp(2));
        tail.close();
        canvas.drawPath(tail, paintFill);

        // Kepala lingkaran
        canvas.drawCircle(cx, r, r, paintFill);
        canvas.drawCircle(cx, r, r, paintStroke);

        // Titik putih di tengah
        canvas.drawCircle(cx, r, r * 0.38f, paintWhite);

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
            case "organik":  return Color.parseColor("#4CAF50"); // hijau
            case "plastik":  return Color.parseColor("#FF9800"); // oranye
            case "kertas":   return Color.parseColor("#FFC107"); // kuning amber
            case "kaca":     return Color.parseColor("#00BCD4"); // cyan
            case "kardus":   return Color.parseColor("#2196F3"); // biru
            case "logam":    return Color.parseColor("#9C27B0"); // ungu
            default:         return Color.parseColor("#F44336"); // merah
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
                R.drawable.ic_user_outline, String.valueOf(rtCount.size()), "RT");
        bindMetric(getView().findViewById(R.id.metricLaporan),
                R.drawable.ic_document_outline, String.valueOf(totalReports), "Total Laporan");
        bindMetric(getView().findViewById(R.id.metricDominan),
                R.drawable.ic_leaf, dominant, "Dominan");
        bindMetric(getView().findViewById(R.id.metricPersentase),
                R.drawable.ic_pie_chart, pct + "%", "Persentase");
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
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String cat : CATEGORIES) {
            View item        = inflater.inflate(R.layout.item_legend_category, legendRow, false);
            FrameLayout circle = item.findViewById(R.id.legendCircle);
            ImageView icon     = item.findViewById(R.id.ivLegendIcon);
            TextView label     = item.findViewById(R.id.tvLegendLabel);
            int color = colorForNama(cat);
            if (circle != null) circle.setBackgroundTintList(
                    ColorStateList.valueOf(softColor(color)));
            if (icon != null) {
                icon.setImageResource(iconFor(cat));
                icon.setImageTintList(ColorStateList.valueOf(color));
            }
            if (label != null) label.setText(cat);
            legendRow.addView(item);
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
        switch (cat) {
            case "Kardus":  return R.drawable.ic_box_outline;
            case "Kaca":    return R.drawable.ic_bottle_outline;
            case "Logam":   return R.drawable.ic_can_outline;
            case "Kertas":  return R.drawable.ic_document_outline;
            case "Plastik": return R.drawable.ic_plastic_bottle_outline;
            default:        return R.drawable.ic_leaf;
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
