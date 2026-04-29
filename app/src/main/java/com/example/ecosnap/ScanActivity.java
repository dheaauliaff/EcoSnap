package com.example.ecosnap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScanActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 200;
    private static final String TAG = "EcoSnapScanner";

    TextView tvHasil;
    TFLiteHelper tflite;
    OverlayView overlayView;
    PreviewView viewFinder;
    Button btnCapture;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private boolean isPaused = false;
    private String currentLabel = "Tidak terdeteksi";
    private float currentConfidence = 0f;
    private Bitmap currentBitmap = null;
    private long lastAnalyzedTimestamp = 0L;
    private long framesReceived = 0L;
    private int liveFps = 0;
    private final ArrayDeque<Long> fpsSamples = new ArrayDeque<>();
    private final Object detectionLock = new Object();
    private List<TFLiteHelper.Result> latestStableDetections = new ArrayList<>();
    private ExecutorService analysisExecutor;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        tvHasil = findViewById(R.id.tvHasil);
        overlayView = findViewById(R.id.overlayView);
        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);

        tflite = new TFLiteHelper(this);
        analysisExecutor = Executors.newSingleThreadExecutor();

        btnCapture.setOnClickListener(v -> captureResult());
        setupTouchFocus();

        checkPermissionAndOpenCamera();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchFocus() {
        viewFinder.setOnTouchListener((view, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP || camera == null) {
                return true;
            }

            MeteringPoint point = viewFinder.getMeteringPointFactory()
                    .createPoint(event.getX(), event.getY());
            FocusMeteringAction action = new FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
            ).setAutoCancelDuration(2, TimeUnit.SECONDS).build();

            camera.getCameraControl().startFocusAndMetering(action);
            view.performClick();
            return true;
        });
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeFrame);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeFrame(ImageProxy imageProxy) {
        framesReceived++;
        long currentTime = System.currentTimeMillis();

        try {
            if (isPaused) {
                return;
            }

            if (currentTime - lastAnalyzedTimestamp < 80) {
                return;
            }
            lastAnalyzedTimestamp = currentTime;
            updateFps(currentTime);

            Bitmap bitmap = imageProxy.toBitmap();
            if (bitmap == null) {
                Log.w(TAG, "Analyzer frame skipped: ImageProxy.toBitmap() returned null");
                return;
            }

            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            Bitmap analyzedBitmap;
            if (rotationDegrees != 0) {
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.postRotate(rotationDegrees);
                analyzedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } else {
                analyzedBitmap = bitmap;
            }

            currentBitmap = analyzedBitmap;
            List<TFLiteHelper.Result> detections = tflite.detect(analyzedBitmap);
            TFLiteHelper.DebugStats stats = tflite.getLastDebugStats();
            synchronized (detectionLock) {
                latestStableDetections = copyDetections(detections);
            }

            Log.d(TAG, "frames=" + framesReceived
                    + " fps=" + liveFps
                    + " inferenceMs=" + stats.inferenceMs
                    + " raw=" + stats.rawDetections
                    + " preNms=" + stats.preNmsDetections
                    + " postNms=" + stats.postNmsDetections
                    + " tracks=" + stats.trackedDetections);

            runOnUiThread(() -> {
                overlayView.setFrameInfo(analyzedBitmap.getWidth(), analyzedBitmap.getHeight(), liveFps);
                overlayView.updateBoxes(detections);
                updateResultText(detections);
            });
        } catch (Exception e) {
            Log.e(TAG, "Analyzer failed while processing camera frame", e);
        } finally {
            imageProxy.close();
        }
    }

    private void updateFps(long currentTime) {
        fpsSamples.addLast(currentTime);
        long cutoff = currentTime - 2000L;
        while (!fpsSamples.isEmpty() && fpsSamples.peekFirst() < cutoff) {
            fpsSamples.removeFirst();
        }

        if (fpsSamples.size() <= 1) {
            liveFps = Math.max(liveFps, 1);
            return;
        }

        long elapsed = Math.max(1L, fpsSamples.peekLast() - fpsSamples.peekFirst());
        liveFps = Math.max(1, Math.round(((fpsSamples.size() - 1) * 1000f) / elapsed));
    }

    private void updateResultText(List<TFLiteHelper.Result> detections) {
        if (detections.isEmpty()) {
            tvHasil.setText("Arahkan kamera ke sampah");
            currentLabel = "Tidak terdeteksi";
            currentConfidence = 0f;
            return;
        }

        float maxConf = 0;
        String nama = "Tidak terdeteksi";
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        boolean anyLocked = false;
        boolean anyLowConfidence = false;

        for (TFLiteHelper.Result r : detections) {
            if (r.confidence > maxConf) {
                maxConf = r.confidence;
                nama = r.label;
            }
            if (r.isLocked) {
                anyLocked = true;
            }
            if (r.isLowConfidence || r.confidence < 70f) {
                anyLowConfidence = true;
            }
            counts.put(r.label, counts.getOrDefault(r.label, 0) + 1);
        }

        currentLabel = nama;
        currentConfidence = maxConf;

        String status = anyLocked ? "LOCKED" : (anyLowConfidence ? "Low Confidence" : "Stable");
        String hasilText = "Detected: " + nama + "\n"
                + String.format("%.0f%%", maxConf) + " Confidence \u2022 " + status;
        tvHasil.setText(hasilText);
    }

    private void captureResult() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Belum ada gambar yang terdeteksi", Toast.LENGTH_SHORT).show();
            return;
        }

        List<TFLiteHelper.Result> frozenDetections = getCaptureDetections();
        isPaused = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        String nama = currentLabel;
        float confidence = currentConfidence;
        if (!frozenDetections.isEmpty()) {
            TFLiteHelper.Result dominant = frozenDetections.get(0);
            nama = dominant.label;
            confidence = dominant.confidence;
        }

        String kategori = nama.equals("Organik") ? "Organik" :
                (nama.equals("Bukan Sampah") ? "Bukan Sampah" : "Anorganik");

        String saran;
        String funfact;
        switch (nama.toLowerCase()) {
            case "organik":
                saran = "Kumpulkan dan jadikan kompos untuk menyuburkan tanaman.";
                funfact = "Sisa makanan membusuk di TPA dan menghasilkan gas metana yang 25x lebih berbahaya dari CO2.";
                break;
            case "kardus":
                saran = "Lipat kardus hingga pipih agar hemat tempat, lalu serahkan ke bank sampah.";
                funfact = "Mendaur ulang 1 ton kardus dapat menyelamatkan 17 pohon besar.";
                break;
            case "kertas":
                saran = "Pastikan kertas tidak basah, kumpulkan dan berikan ke pengepul atau bank sampah.";
                funfact = "Kertas dapat diproses ulang menjadi kertas baru, mengurangi penebangan hutan.";
                break;
            case "kaca":
                saran = "Bilas bersih dan pisahkan dari sampah lain. Hati-hati jika pecah.";
                funfact = "Kaca dapat didaur ulang tanpa batas tanpa menurunkan kualitasnya.";
                break;
            case "plastik":
                saran = "Bersihkan dan remas botol/plastik untuk menghemat ruang, lalu daur ulang.";
                funfact = "Plastik butuh ratusan tahun untuk terurai dan bisa mencemari lautan sebagai mikroplastik.";
                break;
            case "logam":
                saran = "Kumpulkan kaleng atau logam lain dan jual ke pengepul barang bekas.";
                funfact = "Daur ulang aluminium menghemat 95% energi dibandingkan membuat dari bijih bauksit.";
                break;
            case "bukan sampah":
                saran = "Ini bukan sampah. Jangan membuangnya ke tempat sampah.";
                funfact = "Menjaga benda tetap berguna adalah langkah pertama zero waste.";
                break;
            default:
                saran = "Simpan atau serahkan ke bank sampah untuk didaur ulang.";
                funfact = "Memilah sampah di rumah adalah pahlawan lingkungan sebenarnya.";
                break;
        }

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("imageBase64", imageBase64);
        intent.putExtra("nama", nama);
        intent.putExtra("confidence", confidence);
        intent.putExtra("kategori", kategori);
        intent.putExtra("saran", saran);
        intent.putExtra("funfact", funfact);
        intent.putExtra("sourceWidth", currentBitmap.getWidth());
        intent.putExtra("sourceHeight", currentBitmap.getHeight());
        putFrozenDetections(intent, frozenDetections);

        startActivity(intent);
    }

    private List<TFLiteHelper.Result> getCaptureDetections() {
        List<TFLiteHelper.Result> snapshot;
        synchronized (detectionLock) {
            snapshot = copyDetections(latestStableDetections);
        }

        long now = System.currentTimeMillis();
        List<TFLiteHelper.Result> fresh = new ArrayList<>();
        for (TFLiteHelper.Result result : snapshot) {
            if (result.rect == null) {
                continue;
            }
            if (result.missedFrames == 0 && now - result.lastUpdated <= 500L) {
                fresh.add(result);
            }
        }

        if (fresh.isEmpty()) {
            return fresh;
        }

        TFLiteHelper.Result dominant = null;
        float bestScore = -1f;
        float imageArea = Math.max(1f, currentBitmap.getWidth() * currentBitmap.getHeight());
        for (TFLiteHelper.Result result : fresh) {
            float areaWeight = Math.min(25f, (result.rect.width() * result.rect.height() / imageArea) * 100f);
            float score = result.confidence + areaWeight;
            if (score > bestScore) {
                bestScore = score;
                dominant = result;
            }
        }

        List<TFLiteHelper.Result> captureDetections = new ArrayList<>();
        if (dominant != null) {
            captureDetections.add(new TFLiteHelper.Result(dominant));
        }
        return captureDetections;
    }

    private List<TFLiteHelper.Result> copyDetections(List<TFLiteHelper.Result> source) {
        List<TFLiteHelper.Result> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
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
    protected void onResume() {
        super.onResume();
        isPaused = false;
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
