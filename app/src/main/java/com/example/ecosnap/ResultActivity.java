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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    // lokasi
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

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        ambilLokasi();
        initCloudinary();
        loadIntentData();

        btnSimpan.setOnClickListener(v -> uploadCloudinary());
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

    private void loadIntentData() {

        try {
            String imageBase64 =
                    getIntent().getStringExtra("imageBase64");

            nama = getIntent().getStringExtra("nama");
            confidence =
                    getIntent().getFloatExtra("confidence", 0f);
            kategori =
                    getIntent().getStringExtra("kategori");

            String saran =
                    getIntent().getStringExtra("saran");

            String funfact =
                    getIntent().getStringExtra("funfact");

            int sourceWidth =
                    getIntent().getIntExtra("sourceWidth", 0);

            int sourceHeight =
                    getIntent().getIntExtra("sourceHeight", 0);

            if (imageBase64 != null) {

                byte[] decoded =
                        Base64.decode(imageBase64, Base64.DEFAULT);

                bitmapHasil =
                        BitmapFactory.decodeByteArray(
                                decoded,
                                0,
                                decoded.length
                        );

                imgHasil.setImageBitmap(bitmapHasil);
            }

            tvNama.setText(safe(nama));
            tvKategori.setText(
                    "Akurasi: " +
                            String.format("%.0f", confidence) +
                            "%"
            );

            tvSaran.setText("Penanganan: " + safe(saran));
            tvFunfact.setText("Dampak: " + safe(funfact));

            List<TFLiteHelper.Result> frozenResults =
                    readFrozenDetections();

            if (!frozenResults.isEmpty()) {
                overlayView.setImageSource(
                        sourceWidth,
                        sourceHeight
                );
                overlayView.setResults(frozenResults);
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Gagal load hasil scan",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String safe(String text) {
        return (text == null || text.isEmpty()) ? "-" : text;
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

        if (!cloudinaryReady) {

            Toast.makeText(
                    this,
                    "Cloudinary belum siap",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (bitmapHasil == null) {

            Toast.makeText(
                    this,
                    "Gambar kosong",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        btnSimpan.setEnabled(false);
        btnSimpan.setText("Uploading...");

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        bitmapHasil.compress(
                Bitmap.CompressFormat.JPEG,
                80,
                baos
        );

        byte[] data = baos.toByteArray();

        MediaManager.get().upload(data)
                .option("folder", "ecosnap")
                .callback(
                        new com.cloudinary.android.callback.UploadCallback() {

                            @Override
                            public void onStart(String requestId) {
                            }

                            @Override
                            public void onProgress(
                                    String requestId,
                                    long bytes,
                                    long totalBytes
                            ) {
                            }

                            @Override
                            public void onSuccess(
                                    String requestId,
                                    Map resultData
                            ) {

                                String imageUrl =
                                        resultData.get("secure_url")
                                                .toString();

                                simpanKeSupabase(imageUrl);
                            }

                            @Override
                            public void onError(
                                    String requestId,
                                    com.cloudinary.android.callback.ErrorInfo error
                            ) {

                                btnSimpan.setEnabled(true);
                                btnSimpan.setText("💾 Simpan Hasil Scan");

                                Toast.makeText(
                                        ResultActivity.this,
                                        "Upload gagal",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onReschedule(
                                    String requestId,
                                    com.cloudinary.android.callback.ErrorInfo error
                            ) {
                            }
                        }
                ).dispatch();
    }

    private void simpanKeSupabase(String imageUrl) {

        FirebaseUser firebaseUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {

            Toast.makeText(
                    this,
                    "User belum login",
                    Toast.LENGTH_SHORT
            ).show();

            btnSimpan.setEnabled(true);
            btnSimpan.setText("💾 Simpan Hasil Scan");
            return;
        }

        ApiService api =
                RetrofitClient.getClient()
                        .create(ApiService.class);

        api.getUserByFirebaseUid(
                        "eq." + firebaseUser.getUid()
                )
                .enqueue(new Callback<List<User>>() {

                    @Override
                    public void onResponse(
                            Call<List<User>> call,
                            Response<List<User>> response
                    ) {

                        String rwId = "-";
                        String wilayah = "-";

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {

                            User user = response.body().get(0);

                            rwId = user.getRwId() != null ?
                                    user.getRwId() : "-";

                            wilayah = user.getWilayah() != null ?
                                    user.getWilayah() : "-";
                        }

                        insertScan(
                                firebaseUser.getUid(),
                                rwId,
                                wilayah,
                                imageUrl
                        );
                    }

                    @Override
                    public void onFailure(
                            Call<List<User>> call,
                            Throwable t
                    ) {

                        insertScan(
                                firebaseUser.getUid(),
                                "-",
                                "-",
                                imageUrl
                        );
                    }
                });
    }

    private void insertScan(
            String firebaseId,
            String rwId,
            String wilayah,
            String imageUrl
    ) {

        Map<String, Object> data = new HashMap<>();

        data.put("firebase_id", firebaseId);
        data.put("nama_sampah", nama);
        data.put("kategori", kategori);
        data.put("confidence", String.valueOf(confidence));
        data.put("image_url", imageUrl);
        data.put("wilayah", wilayah);
        data.put("rw_id", rwId);
        data.put("rt_id", "-");

        data.put("latitude", latitude);
        data.put("longitude", longitude);

        ApiService api =
                RetrofitClient.getClient()
                        .create(ApiService.class);

        api.insertScan(data)
                .enqueue(new Callback<Void>() {

                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {

                        btnSimpan.setEnabled(true);
                        btnSimpan.setText("💾 Simpan Hasil Scan");

                        if (response.isSuccessful()) {

                            Toast.makeText(
                                    ResultActivity.this,
                                    "Berhasil disimpan",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            try {

                                String err =
                                        response.errorBody().string();

                                Toast.makeText(
                                        ResultActivity.this,
                                        "400: " + err,
                                        Toast.LENGTH_LONG
                                ).show();

                            } catch (Exception e) {

                                Toast.makeText(
                                        ResultActivity.this,
                                        "Gagal simpan",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<Void> call,
                            Throwable t
                    ) {

                        btnSimpan.setEnabled(true);
                        btnSimpan.setText("💾 Simpan Hasil Scan");

                        Toast.makeText(
                                ResultActivity.this,
                                t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private List<TFLiteHelper.Result> readFrozenDetections() {

        List<TFLiteHelper.Result> results =
                new ArrayList<>();

        int count =
                getIntent().getIntExtra(
                        "frozenDetectionCount",
                        0
                );

        if (count <= 0) return results;

        float[] left =
                getIntent().getFloatArrayExtra("frozenLeft");

        float[] top =
                getIntent().getFloatArrayExtra("frozenTop");

        float[] right =
                getIntent().getFloatArrayExtra("frozenRight");

        float[] bottom =
                getIntent().getFloatArrayExtra("frozenBottom");

        float[] confidence =
                getIntent().getFloatArrayExtra("frozenConfidence");

        int[] classId =
                getIntent().getIntArrayExtra("frozenClassId");

        String[] labels =
                getIntent().getStringArrayExtra("frozenLabels");

        if (left == null || top == null ||
                right == null || bottom == null ||
                confidence == null ||
                classId == null ||
                labels == null) {
            return results;
        }

        int safeCount =
                Math.min(count, labels.length);

        for (int i = 0; i < safeCount; i++) {

            TFLiteHelper.Result result =
                    new TFLiteHelper.Result(
                            new RectF(
                                    left[i],
                                    top[i],
                                    right[i],
                                    bottom[i]
                            ),
                            labels[i],
                            confidence[i],
                            classId[i]
                    );

            results.add(result);
        }

        return results;
    }
}