package com.example.ecosnap.user;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cloudinary.android.MediaManager;
import com.example.ecosnap.network.ApiService;
import com.example.ecosnap.helper.OverlayView;
import com.example.ecosnap.R;
import com.example.ecosnap.network.RetrofitClient;
import com.example.ecosnap.helper.TFLiteHelper;
import com.example.ecosnap.SharedPrototypeData;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.util.*;

import retrofit2.*;

public class ResultActivity extends AppCompatActivity {

    ImageView imgHasil;
    TextView tvTotalObjek, tvKategoriTerbanyak, tvSaran, tvFunfact;
    LinearLayout llDetectedObjects;
    OverlayView overlayView;
    MaterialButton btnSimpan;

    Bitmap bitmapHasil;

    String primaryNama = "-";
    float primaryConfidence = 0f;
    String primaryKategori = "-";

    boolean cloudinaryReady = false;

    double latitude = 0.0;
    double longitude = 0.0;

    FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imgHasil = findViewById(R.id.imgHasil);
        tvTotalObjek = findViewById(R.id.tvTotalObjek);
        tvKategoriTerbanyak = findViewById(R.id.tvKategoriTerbanyak);
        llDetectedObjects = findViewById(R.id.llDetectedObjects);
        tvSaran = findViewById(R.id.tvSaran);
        tvFunfact = findViewById(R.id.tvFunfact);
        overlayView = findViewById(R.id.overlayView);
        btnSimpan = findViewById(R.id.btnSimpan);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ambilLokasi();
        initCloudinary();
        loadIntentData();

        if (btnSimpan != null) btnSimpan.setOnClickListener(v -> uploadCloudinary());
    }

    private void loadIntentData() {
        try {
            String imageBase64 = getIntent().getStringExtra("imageBase64");
            String imagePath = getIntent().getStringExtra("imagePath");
            primaryNama = getIntent().getStringExtra("nama");
            primaryConfidence = getIntent().getFloatExtra("confidence", 0f);
            primaryKategori = getIntent().getStringExtra("kategori");
            String saran = getIntent().getStringExtra("saran");
            String funfact = getIntent().getStringExtra("funfact");

            int sourceWidth = getIntent().getIntExtra("sourceWidth", 0);
            int sourceHeight = getIntent().getIntExtra("sourceHeight", 0);

            if (imagePath != null && !imagePath.isEmpty()) {
                bitmapHasil = BitmapFactory.decodeFile(imagePath);
                if (bitmapHasil != null && imgHasil != null) {
                    imgHasil.setImageBitmap(bitmapHasil);
                }
            } else if (imageBase64 != null && !imageBase64.isEmpty()) {
                try {
                    byte[] decoded = Base64.decode(imageBase64, Base64.DEFAULT);
                    bitmapHasil = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bitmapHasil != null && imgHasil != null) {
                        imgHasil.setImageBitmap(bitmapHasil);
                    }
                } catch (OutOfMemoryError | Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Gambar terlalu besar untuk ditampilkan.", Toast.LENGTH_SHORT).show();
                }
            }

            if (tvSaran != null) tvSaran.setText(safe(saran));
            if (tvFunfact != null) tvFunfact.setText(safe(funfact));

            List<TFLiteHelper.Result> frozenResults = readFrozenDetections();

            if (!frozenResults.isEmpty()) {
                if (overlayView != null) {
                    overlayView.setImageSource(sourceWidth, sourceHeight);
                    overlayView.setResults(frozenResults);
                }
                populateDetectedList(frozenResults);
            } else if (primaryNama != null && !primaryNama.equals("-")) {
                TFLiteHelper.Result r = new TFLiteHelper.Result(new RectF(), primaryNama, primaryConfidence, -1);
                populateDetectedList(Collections.singletonList(r));
            }

        } catch (Exception e) {
            Toast.makeText(this, "Gagal load hasil scan", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateDetectedList(List<TFLiteHelper.Result> list) {
        if (tvTotalObjek != null) tvTotalObjek.setText(String.valueOf(list.size()));

        if (list.isEmpty()) return;

        HashMap<String, Integer> counts = new HashMap<>();
        for (TFLiteHelper.Result r : list) {
            counts.put(r.label, counts.getOrDefault(r.label, 0) + 1);
        }
        String maxLabel = "-";
        int max = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                maxLabel = e.getKey();
            }
        }

        if (tvKategoriTerbanyak != null) {
            String catLabel = getKategoriForLabel(maxLabel);
            tvKategoriTerbanyak.setText(catLabel);
            tvKategoriTerbanyak.getBackground().setTint(getColorForLabel(maxLabel));
        }

        if (llDetectedObjects != null) {
            llDetectedObjects.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < Math.min(list.size(), 7); i++) {
                TFLiteHelper.Result r = list.get(i);
                View view = inflater.inflate(R.layout.item_detected_object, llDetectedObjects, false);

                TextView tvNamaObjek = view.findViewById(R.id.tvNamaObjek);
                TextView tvKategoriObjek = view.findViewById(R.id.tvKategoriObjek);
                TextView tvAkurasi = view.findViewById(R.id.tvAkurasi);
                ImageView ivIcon = view.findViewById(R.id.ivIcon);

                int icon = getIconForLabel(r.label);
                int color = getColorForLabel(r.label);

                tvNamaObjek.setText((i + 1) + ". " + r.label);
                tvKategoriObjek.setText(getKategoriForLabel(r.label));
                tvKategoriObjek.getBackground().setTint(color);
                tvAkurasi.setText(String.format(Locale.US, "%.0f%%", r.confidence));

                ivIcon.setImageResource(icon);
                ivIcon.setColorFilter(color);

                llDetectedObjects.addView(view);
            }
        }
    }

    private int getIconForLabel(String label) {
        if (label == null) return R.drawable.ic_total;
        String l = label.toLowerCase();
        if (l.contains("organik")) return R.drawable.ic_organik;
        if (l.contains("plastik") || l.contains("styrofoam")) return R.drawable.ic_anorganik;
        if (l.contains("kertas") || l.contains("kardus") || l.contains("kaca") || l.contains("logam")) return R.drawable.ic_recycle;
        if (l.contains("bukan")) return R.drawable.ic_bukan_sampah;
        return R.drawable.ic_total;
    }

    private int getColorForLabel(String label) {
        if (label == null) return 0xFF9E9E9E;
        String l = label.toLowerCase();
        if (l.contains("organik")) return 0xFF4CAF50;
        if (l.contains("kardus")) return 0xFF2196F3;
        if (l.contains("kaca")) return 0xFF00BCD4;
        if (l.contains("logam")) return 0xFF9C27B0;
        if (l.contains("kertas")) return 0xFFFFC107;
        if (l.contains("plastik") || l.contains("styrofoam")) return 0xFFFF9800;
        if (l.contains("bukan")) return 0xFFFF5252;
        return 0xFF9E9E9E;
    }

    private String getKategoriForLabel(String label) {
        if (label == null) return "Lainnya";
        String l = label.toLowerCase();
        if (l.contains("organik")) return "Organik";
        if (l.contains("plastik") || l.contains("styrofoam")) return "Anorganik";
        if (l.contains("kertas") || l.contains("kardus") || l.contains("kaca") || l.contains("logam")) return "Recycle";
        if (l.contains("bukan")) return "Bukan Sampah";
        return "Lainnya";
    }

    private String safe(String text) {
        return (text == null || text.isEmpty()) ? "-" : text;
    }

    private void ambilLokasi() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    1001
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                });
    }

    private void initCloudinary() {
        try {
            Map config = new HashMap();
            config.put("cloud_name", "degqcksgm");
            config.put("api_key", "137543667976958");
            config.put("api_secret", "7gniTF71lnqNOdnNBud_COJFO48");

            MediaManager.init(this, config);
            cloudinaryReady = true;

        } catch (Exception e) {
            cloudinaryReady = true;
        }
    }

    private void uploadCloudinary() {
        if (!cloudinaryReady || bitmapHasil == null) return;

        if (btnSimpan != null) {
            btnSimpan.setEnabled(false);
            btnSimpan.setText("Uploading...");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmapHasil.compress(Bitmap.CompressFormat.JPEG, 80, baos);

        MediaManager.get().upload(baos.toByteArray())
                .option("folder", "ecosnap")
                .callback(new com.cloudinary.android.callback.UploadCallback() {

                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString();
                        simpanKeSupabase(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        if (btnSimpan != null) {
                            btnSimpan.setEnabled(true);
                            btnSimpan.setText("Simpan Hasil");
                        }
                        Toast.makeText(ResultActivity.this, "Upload gagal", Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
                }).dispatch();
    }

    private void simpanKeSupabase(String imageUrl) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.insertScan(new HashMap<String, Object>() {{
            put("user_id", firebaseUser.getUid());
            put("jenis_sampah", primaryNama);
            put("kategori", primaryKategori);
            put("akurasi", primaryConfidence);
            put("foto_url", imageUrl);
            put("latitude", latitude);
            put("longitude", longitude);
        }}).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (btnSimpan != null) {
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Hasil");
                }
                if (response.isSuccessful()) {
                    Toast.makeText(ResultActivity.this, "Berhasil disimpan", Toast.LENGTH_SHORT).show();
                    SharedPrototypeData.getInstance().addScan("RT 01", primaryNama, primaryKategori, "Baru saja");
                } else {
                    Toast.makeText(ResultActivity.this, "Gagal menyimpan data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (btnSimpan != null) {
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Hasil");
                }
                Toast.makeText(ResultActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<TFLiteHelper.Result> readFrozenDetections() {
        List<TFLiteHelper.Result> results = new ArrayList<>();
        int count = getIntent().getIntExtra("frozenDetectionCount", 0);
        if (count == 0) return results;

        float[] left = getIntent().getFloatArrayExtra("frozenLeft");
        float[] top = getIntent().getFloatArrayExtra("frozenTop");
        float[] right = getIntent().getFloatArrayExtra("frozenRight");
        float[] bottom = getIntent().getFloatArrayExtra("frozenBottom");
        float[] confidence = getIntent().getFloatArrayExtra("frozenConfidence");
        int[] classId = getIntent().getIntArrayExtra("frozenClassId");
        int[] trackingId = getIntent().getIntArrayExtra("frozenTrackingId");
        int[] stableFrames = getIntent().getIntArrayExtra("frozenStableFrames");
        boolean[] locked = getIntent().getBooleanArrayExtra("frozenLocked");
        boolean[] lowConfidence = getIntent().getBooleanArrayExtra("frozenLowConfidence");
        String[] labels = getIntent().getStringArrayExtra("frozenLabels");

        if (left == null || top == null || right == null || bottom == null || confidence == null || labels == null) {
            return results;
        }

        for (int i = 0; i < count; i++) {
            TFLiteHelper.Result res = new TFLiteHelper.Result(
                    new RectF(left[i], top[i], right[i], bottom[i]),
                    labels[i],
                    confidence[i],
                    classId != null ? classId[i] : -1
            );
            if (trackingId != null) res.trackingId = trackingId[i];
            if (stableFrames != null) res.stableFrames = stableFrames[i];
            if (locked != null) res.isLocked = locked[i];
            if (lowConfidence != null) res.isLowConfidence = lowConfidence[i];
            results.add(res);
        }
        return results;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bitmapHasil = null;
    }
}
