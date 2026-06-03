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
import com.example.ecosnap.model.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.ByteArrayOutputStream;
import java.util.*;

import retrofit2.*;

public class ResultActivity extends AppCompatActivity {

    ImageView imgHasil;
    TextView tvTotalObjek, tvKategoriTerbanyak, tvSaran, tvFunfact;
    LinearLayout llDetectedObjects;
    com.example.ecosnap.helper.OverlayView overlayView;
    MaterialButton btnSimpan;

    Bitmap bitmapHasil;

    String primaryNama = "-";
    float primaryConfidence = 0f;
    String primaryKategori = "-";
    String alamat = "";

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

        if (btnSimpan != null) {
            btnSimpan.setOnClickListener(v -> uploadCloudinary());
        }
        // CATATAN: Upload TIDAK dipanggil otomatis di sini
        // agar tidak terjadi double-upload. User harus tekan tombol Simpan.

        View btnInfoSaran = findViewById(R.id.btnInfoSaran);
        if (btnInfoSaran != null) {
            btnInfoSaran.setOnClickListener(v -> showInfoDialog());
        }
    }

    private void showInfoDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_info_penanganan, null);
        TextView tvDialogSaran = dialogView.findViewById(R.id.tvDialogSaran);
        TextView tvDialogFunfact = dialogView.findViewById(R.id.tvDialogFunfact);

        if (tvDialogSaran != null && tvSaran != null) {
            tvDialogSaran.setText(tvSaran.getText());
        }
        if (tvDialogFunfact != null && tvFunfact != null) {
            tvDialogFunfact.setText(tvFunfact.getText());
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);
        if (btnDialogClose != null) {
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        }

        View btnDialogOke = dialogView.findViewById(R.id.btnDialogOke);
        if (btnDialogOke != null) {
            btnDialogOke.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
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

    private void initCloudinary() {
        try {
            // Cek apakah MediaManager sudah diinisialisasi sebelumnya
            // Kalau sudah, jangan init ulang — ini yang sering menyebabkan crash
            MediaManager.get();
            cloudinaryReady = true;
            android.util.Log.d("CLOUDINARY", "MediaManager sudah aktif sebelumnya, skip init.");
        } catch (IllegalStateException notInitYet) {
            // Belum diinit, lakukan init sekarang
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "degqcksgm");
                config.put("api_key",    "137543667976958");
                config.put("api_secret", "7gniTF71lnqNOdnNBud_COJFO48");
                MediaManager.init(this, config);
                cloudinaryReady = true;
                android.util.Log.d("CLOUDINARY", "MediaManager berhasil diinisialisasi.");
            } catch (Exception e) {
                cloudinaryReady = false;
                android.util.Log.e("CLOUDINARY", "Gagal init MediaManager: " + e.getMessage());
            }
        }
    }

    private void uploadCloudinary() {
        if (!cloudinaryReady) {
            Toast.makeText(this, "Cloudinary belum siap, coba lagi", Toast.LENGTH_SHORT).show();
            android.util.Log.e("CLOUDINARY", "Upload dibatalkan: cloudinaryReady=false");
            return;
        }
        if (bitmapHasil == null) {
            Toast.makeText(this, "Gambar belum tersedia", Toast.LENGTH_SHORT).show();
            android.util.Log.e("CLOUDINARY", "Upload dibatalkan: bitmapHasil=null");
            return;
        }

        if (btnSimpan != null) {
            btnSimpan.setEnabled(false);
            btnSimpan.setText("Sedang Menyimpan...");
        }

        android.util.Log.d("CLOUDINARY", "Mulai upload gambar ke Cloudinary...");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmapHasil.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        android.util.Log.d("CLOUDINARY", "Ukuran gambar: " + imageBytes.length + " bytes");

        MediaManager.get().upload(imageBytes)
                .option("folder", "ecosnap")
                .callback(new com.cloudinary.android.callback.UploadCallback() {

                    @Override
                    public void onStart(String requestId) {
                        android.util.Log.d("CLOUDINARY", "Upload dimulai, requestId=" + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        android.util.Log.d("CLOUDINARY", "Progress: " + bytes + "/" + totalBytes);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString();
                        android.util.Log.d("CLOUDINARY", "Upload berhasil! URL: " + imageUrl);
                        fetchUserAndSave(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        android.util.Log.e("CLOUDINARY", "Upload GAGAL: " + error.getDescription());
                        runOnUiThread(() -> {
                            if (btnSimpan != null) {
                                btnSimpan.setEnabled(true);
                                btnSimpan.setText("Simpan Ulang");
                            }
                            Toast.makeText(ResultActivity.this,
                                    "Cloudinary gagal: " + error.getDescription(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        android.util.Log.w("CLOUDINARY", "Upload dijadwalkan ulang: " + error.getDescription());
                    }
                }).dispatch();
    }

    private void fetchUserAndSave(String imageUrl) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getUserByFirebaseUid("eq." + firebaseUser.getUid()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    simpanKeSupabase(imageUrl, user);
                } else {
                    Toast.makeText(ResultActivity.this, "Gagal mengambil profil user", Toast.LENGTH_SHORT).show();
                    if (btnSimpan != null) {
                        btnSimpan.setEnabled(true);
                        btnSimpan.setText("Simpan Ulang");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(ResultActivity.this, "Gagal terhubung ke database profil", Toast.LENGTH_SHORT).show();
                if (btnSimpan != null) {
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Ulang");
                }
            }
        });
    }

    private void simpanKeSupabase(String imageUrl, User user) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        String firebaseId = user.getFirebaseUid();

        double finalLat = latitude != 0 ? latitude : -6.8900;
        double finalLng = longitude != 0 ? longitude : 107.5400;

        new Thread(() -> {
            String alamatScan = reverseGeocode(finalLat, finalLng);

            Map<String, Object> data = new HashMap<>();
            data.put("firebase_id", firebaseId);
            data.put("nama_sampah", primaryNama);
            data.put("kategori", primaryKategori);
            data.put("confidence", primaryConfidence);
            data.put("image_url", imageUrl);
            data.put("latitude", finalLat);
            data.put("longitude", finalLng);
            data.put("alamat", alamatScan);

            if (user.getWilayah() != null && !user.getWilayah().isEmpty()) {
                data.put("wilayah", user.getWilayah());
            }
            if (user.getRwId() != null && !user.getRwId().isEmpty()) {
                data.put("rw_id", user.getRwId());
            }
            if (user.getRtId() != null && !user.getRtId().isEmpty()) {
                data.put("rt_id", user.getRtId());
            }

            android.util.Log.d("SUPABASE_INSERT", "Mengirim data: " + data.toString());

            runOnUiThread(() -> {
                api.insertScan(data).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            if (btnSimpan != null) {
                                btnSimpan.setEnabled(false);
                                btnSimpan.setText("Berhasil Disimpan ✓");
                            }
                            Toast.makeText(ResultActivity.this, "Data Tersimpan!", Toast.LENGTH_SHORT).show();

                            String rt = (user.getRtId() != null && !user.getRtId().isEmpty())
                                    ? user.getRtId()
                                    : "RT 01";

                            SharedPrototypeData.getInstance().addScan(rt, primaryNama, primaryKategori, "Baru saja");
                        } else {
                            String err = "Gagal: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    android.util.Log.e("SUPABASE_ERROR", "HTTP " + response.code() + " → " + errorBody);
                                    android.util.Log.e("SUPABASE_ERROR", "Data yang dikirim: " + data.toString());
                                    err += " - " + errorBody;
                                }
                            } catch (Exception ignored) {}

                            Toast.makeText(ResultActivity.this, err, Toast.LENGTH_LONG).show();

                            if (btnSimpan != null) {
                                btnSimpan.setEnabled(true);
                                btnSimpan.setText("Simpan Ulang");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(ResultActivity.this,
                                "Koneksi database gagal: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        android.util.Log.e("SUPABASE_ERROR", "Network failure: " + t.getMessage());

                        if (btnSimpan != null) {
                            btnSimpan.setEnabled(true);
                            btnSimpan.setText("Simpan Ulang");
                        }
                    }
                });
            });
        }).start();
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
