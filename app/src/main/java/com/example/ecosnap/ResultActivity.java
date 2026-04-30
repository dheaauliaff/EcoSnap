package com.example.ecosnap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cloudinary.android.MediaManager;
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
    TextView tvNama, tvKategori, tvSaran, tvFunfact;
    OverlayView overlayView;
    MaterialButton btnSimpan;

    Bitmap bitmapHasil;

    String nama = "-";
    float confidence = 0f;
    String kategori = "-";

    boolean cloudinaryReady = false;

    double latitude = 0.0;
    double longitude = 0.0;

    FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imgHasil = findViewById(R.id.imgHasil);
        tvNama = findViewById(R.id.tvNamaSampah);
        tvKategori = findViewById(R.id.tvKategori);
        tvSaran = findViewById(R.id.tvSaran);
        tvFunfact = findViewById(R.id.tvFunfact);
        overlayView = findViewById(R.id.overlayView);
        btnSimpan = findViewById(R.id.btnSimpan);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ambilLokasi();
        initCloudinary();
        loadIntentData();

        btnSimpan.setOnClickListener(v -> uploadCloudinary());
    }

    private void loadIntentData() {
        try {

            String imageBase64 = getIntent().getStringExtra("imageBase64");
            nama = getIntent().getStringExtra("nama");
            confidence = getIntent().getFloatExtra("confidence", 0f);
            kategori = getIntent().getStringExtra("kategori");
            String saran = getIntent().getStringExtra("saran");
            String funfact = getIntent().getStringExtra("funfact");

            int sourceWidth = getIntent().getIntExtra("sourceWidth", 0);
            int sourceHeight = getIntent().getIntExtra("sourceHeight", 0);

            if (imageBase64 != null) {
                byte[] decoded = Base64.decode(imageBase64, Base64.DEFAULT);
                bitmapHasil = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imgHasil.setImageBitmap(bitmapHasil);
            }

            tvNama.setText(safe(nama));
            tvKategori.setText(safe(kategori) + " • " + String.format("%.0f", confidence) + "%");
            tvSaran.setText("Penanganan: " + safe(saran));
            tvFunfact.setText("Dampak: " + safe(funfact));

            List<TFLiteHelper.Result> frozenResults = readFrozenDetections();

            if (!frozenResults.isEmpty()) {
                overlayView.setImageSource(sourceWidth, sourceHeight);
                overlayView.setResults(frozenResults);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Gagal load hasil scan", Toast.LENGTH_SHORT).show();
        }
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

        btnSimpan.setEnabled(false);
        btnSimpan.setText("Uploading...");

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
                        btnSimpan.setEnabled(true);
                        btnSimpan.setText("Simpan Hasil");
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
            put("firebase_id", firebaseUser.getUid());
            put("nama_sampah", nama);
            put("kategori", kategori);
            put("confidence", String.valueOf(confidence));
            put("image_url", imageUrl);
            put("latitude", latitude);
            put("longitude", longitude);
        }}).enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSimpan.setEnabled(true);
                btnSimpan.setText("Simpan Hasil");
                Toast.makeText(ResultActivity.this, "Berhasil disimpan", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSimpan.setEnabled(true);
                btnSimpan.setText("Simpan Hasil");
            }
        });
    }

    private List<TFLiteHelper.Result> readFrozenDetections() {
        List<TFLiteHelper.Result> results = new ArrayList<>();
        return results;
    }
}