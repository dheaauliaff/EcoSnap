package com.example.ecosnap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG_IMAGE_FILE = "ecosnap_capture.jpg";

    TextView tvHasil;
    TFLiteHelper tflite;
    OverlayView overlayView;
    ImageView viewFinder;
    MaterialButton btnCapture, btnGallery;
    ProgressBar progressScan;

    private Bitmap currentBitmap = null;
    private List<TFLiteHelper.Result> latestDetections = new ArrayList<>();
    private ExecutorService analysisExecutor;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    processImageUri(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    processImageUri(cameraImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        tvHasil = findViewById(R.id.tvHasil);
        overlayView = findViewById(R.id.overlayView);
        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        progressScan = findViewById(R.id.progressScan);

        tflite = new TFLiteHelper(this);
        analysisExecutor = Executors.newSingleThreadExecutor();

        btnCapture.setOnClickListener(v -> openCameraIntent());
        btnGallery.setOnClickListener(v -> openGalleryIntent());
    }

    private void openGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void openCameraIntent() {
        try {
            File imageFile = new File(getExternalFilesDir(null), TAG_IMAGE_FILE);
            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    imageFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Kamera tidak bisa dibuka", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImageUri(@Nullable Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "Gambar tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        tvHasil.setText("Memproses gambar...");
        overlayView.updateBoxes(new ArrayList<>());

        analysisExecutor.execute(() -> {
            try {
                Bitmap bitmap = readBitmap(uri);
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(this, "Gagal membaca gambar", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                currentBitmap = bitmap;
                List<TFLiteHelper.Result> detections = tflite.detect(bitmap);
                latestDetections = copyDetections(detections);

                runOnUiThread(() -> {
                    viewFinder.setImageBitmap(bitmap);
                    overlayView.setImageSource(bitmap.getWidth(), bitmap.getHeight());
                    overlayView.updateBoxes(detections);
                    updateResultText(detections);
                    showLoading(false);
                    if (!detections.isEmpty()) {
                        openResult(detections);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
                    tvHasil.setText("Pilih gambar lain untuk mencoba lagi");
                });
            }
        });
    }

    private Bitmap readBitmap(Uri uri) throws Exception {
        Bitmap bitmap;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        }
        if (bitmap == null) return null;

        int rotation = readRotation(uri);
        if (rotation == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private int readRotation(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return 0;
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void updateResultText(List<TFLiteHelper.Result> detections) {
        if (detections.isEmpty()) {
            tvHasil.setText("Tidak ada objek terdeteksi. Coba gambar lain.");
            return;
        }

        TFLiteHelper.Result dominant = findDominant(detections);
        tvHasil.setText(dominant.label + " terdeteksi - " + String.format("%.0f%%", dominant.confidence));
    }

    private void openResult(List<TFLiteHelper.Result> detections) {
        if (currentBitmap == null || detections.isEmpty()) return;

        TFLiteHelper.Result dominant = findDominant(detections);
        String nama = dominant.label;
        float confidence = dominant.confidence;
        String kategori = mapCategory(nama);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos);
        String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("imageBase64", imageBase64);
        intent.putExtra("nama", nama);
        intent.putExtra("confidence", confidence);
        intent.putExtra("kategori", kategori);
        intent.putExtra("saran", buildSaran(nama));
        intent.putExtra("funfact", buildFunfact(nama));
        intent.putExtra("sourceWidth", currentBitmap.getWidth());
        intent.putExtra("sourceHeight", currentBitmap.getHeight());
        putFrozenDetections(intent, latestDetections);
        startActivity(intent);
    }

    private TFLiteHelper.Result findDominant(List<TFLiteHelper.Result> detections) {
        TFLiteHelper.Result dominant = detections.get(0);
        for (TFLiteHelper.Result result : detections) {
            if (result.confidence > dominant.confidence) {
                dominant = result;
            }
        }
        return dominant;
    }

    private String mapCategory(String label) {
        if ("Organik".equalsIgnoreCase(label)) return "Organik";
        if ("Kardus".equalsIgnoreCase(label)
                || "Kaca".equalsIgnoreCase(label)
                || "Logam".equalsIgnoreCase(label)
                || "Kertas".equalsIgnoreCase(label)) return "Recycle";
        if ("Plastik".equalsIgnoreCase(label)) return "Anorganik";
        if ("Bukan Sampah".equalsIgnoreCase(label)
                || "Bukan_sampah".equalsIgnoreCase(label)) return "Bukan Sampah";
        return "Anorganik";
    }

    private String buildSaran(String nama) {
        switch (nama.toLowerCase()) {
            case "organik":
                return "Kumpulkan dan jadikan kompos untuk menyuburkan tanaman.";
            case "kardus":
                return "Lipat kardus hingga pipih, lalu serahkan ke bank sampah.";
            case "kertas":
                return "Pastikan kertas kering sebelum dikumpulkan untuk daur ulang.";
            case "kaca":
                return "Bilas bersih dan pisahkan dari sampah lain. Hati-hati jika pecah.";
            case "plastik":
                return "Bersihkan dan remas plastik untuk menghemat ruang sebelum didaur ulang.";
            case "logam":
                return "Kumpulkan kaleng atau logam lain dan serahkan ke pengepul.";
            case "bukan sampah":
                return "Ini bukan sampah. Simpan agar tetap dapat digunakan.";
            default:
                return "Pisahkan sesuai kategori agar proses pengelolaan lebih mudah.";
        }
    }

    private String buildFunfact(String nama) {
        switch (nama.toLowerCase()) {
            case "organik":
                return "Sampah organik dapat menjadi kompos dan mengurangi timbunan TPA.";
            case "kardus":
                return "Kardus yang dipipihkan membuat proses pengumpulan jauh lebih efisien.";
            case "kertas":
                return "Kertas bersih lebih mudah masuk proses daur ulang.";
            case "kaca":
                return "Kaca dapat didaur ulang berulang kali tanpa banyak menurunkan kualitas.";
            case "plastik":
                return "Plastik yang dipilah membantu mencegah pencemaran lingkungan.";
            case "logam":
                return "Daur ulang logam dapat menghemat energi produksi bahan baru.";
            case "bukan sampah":
                return "Menggunakan benda lebih lama adalah langkah awal mengurangi sampah.";
            default:
                return "Pemilahan kecil di rumah membantu pengelolaan sampah kota.";
        }
    }

    private void showLoading(boolean isLoading) {
        progressScan.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCapture.setEnabled(!isLoading);
        btnGallery.setEnabled(!isLoading);
    }

    private List<TFLiteHelper.Result> copyDetections(List<TFLiteHelper.Result> source) {
        List<TFLiteHelper.Result> copy = new ArrayList<>();
        if (source == null) return copy;
        for (TFLiteHelper.Result result : source) {
            if (result != null && result.rect != null) {
                copy.add(new TFLiteHelper.Result(result));
            }
        }
        return copy;
    }

    private void putFrozenDetections(Intent intent, List<TFLiteHelper.Result> detections) {
        int count = detections.size();
        float[] left = new float[count];
        float[] top = new float[count];
        float[] right = new float[count];
        float[] bottom = new float[count];
        float[] confidence = new float[count];
        int[] classId = new int[count];
        int[] trackingId = new int[count];
        int[] stableFrames = new int[count];
        boolean[] locked = new boolean[count];
        boolean[] lowConfidence = new boolean[count];
        String[] labels = new String[count];

        for (int i = 0; i < count; i++) {
            TFLiteHelper.Result result = detections.get(i);
            RectF rect = result.rect;
            left[i] = rect.left;
            top[i] = rect.top;
            right[i] = rect.right;
            bottom[i] = rect.bottom;
            confidence[i] = result.confidence;
            classId[i] = result.classId;
            trackingId[i] = result.trackingId;
            stableFrames[i] = result.stableFrames;
            locked[i] = result.isLocked;
            lowConfidence[i] = result.isLowConfidence;
            labels[i] = result.label;
        }

        intent.putExtra("frozenDetectionCount", count);
        intent.putExtra("frozenLeft", left);
        intent.putExtra("frozenTop", top);
        intent.putExtra("frozenRight", right);
        intent.putExtra("frozenBottom", bottom);
        intent.putExtra("frozenConfidence", confidence);
        intent.putExtra("frozenClassId", classId);
        intent.putExtra("frozenTrackingId", trackingId);
        intent.putExtra("frozenStableFrames", stableFrames);
        intent.putExtra("frozenLocked", locked);
        intent.putExtra("frozenLowConfidence", lowConfidence);
        intent.putExtra("frozenLabels", labels);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
        }
    }
}
