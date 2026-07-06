package com.example.ecosnap;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ecosnap.helper.OverlayView;
import com.example.ecosnap.helper.TFLiteHelper;
import com.example.ecosnap.user.ResultActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanFragment extends Fragment {

    private static final String TAG = "ScanFragment";

    private TextView tvHasil, tvScanProgress;
    private TFLiteHelper tflite;
    private OverlayView overlayView;
    private PreviewView viewFinder;
    private FloatingActionButton btnCapture, btnGallery, btnReset;
    private ProgressBar progressScan;
    private View layoutScanProgress;

    private Bitmap currentBitmap = null;
    private List<TFLiteHelper.Result> latestDetections = new ArrayList<>();
    private ExecutorService analysisExecutor;

    private ProcessCameraProvider cameraProvider;
    private long lastAnalysisTime = 0;
    private int frameCount = 0;
    private int fps = 0;
    private boolean isProcessingCapture = false;

    // Reusable rotated bitmap buffer to prevent massive memory allocations
    private Bitmap rotatedBitmapBuffer = null;
    private android.graphics.Canvas rotatedBitmapCanvas = null;

    // ── Permission launcher ──
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT).show();
                }
            });

    // ── Multi-select gallery launcher ──
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    List<Uri> uris = new ArrayList<>();

                    // Multi-select: ambil dari clipData
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            uris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        // Fallback single select
                        uris.add(result.getData().getData());
                    }

                    if (!uris.isEmpty()) {
                        // Proses foto pertama untuk deteksi, sisanya dikirim ke ResultActivity
                        processImageUri(uris.get(0), uris);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_scan, container, false);

        tvHasil       = view.findViewById(R.id.tvHasil);
        tvScanProgress = view.findViewById(R.id.tvScanProgress);
        overlayView   = view.findViewById(R.id.overlayView);
        viewFinder    = view.findViewById(R.id.viewFinder);
        btnCapture    = view.findViewById(R.id.btnCapture);
        btnGallery    = view.findViewById(R.id.btnGallery);
        btnReset      = view.findViewById(R.id.btnReset);
        progressScan  = view.findViewById(R.id.progressScan);
        layoutScanProgress = view.findViewById(R.id.layoutScanProgress);

        tflite           = new TFLiteHelper(requireContext());
        analysisExecutor = Executors.newSingleThreadExecutor();

        // ── Tombol Capture: bounding box sudah tampil → baru proses ──
        if (btnCapture != null) {
            btnCapture.setOnClickListener(v -> {
                if (latestDetections.isEmpty()) {
                    Toast.makeText(getContext(),
                            "Arahkan kamera ke sampah terlebih dahulu",
                            Toast.LENGTH_SHORT).show();
                } else {
                    isProcessingCapture = true;
                    showLoading(true, "Menganalisis sampah...");
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> openResult(latestDetections), 350);
                }
            });
        }

        // ── Tombol Galeri: multi-select ──
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> openGalleryMultiIntent());
        }

        // ── Tombol Reset: bersihkan cache scan saat ini ──
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> resetScanState());
        }

        checkPermissions();
        return view;
    }

    // ─── Reset state scan (clear cache) ──────────────────────────────────────

    private void resetScanState() {
        latestDetections.clear();
        currentBitmap = null;
        if (overlayView != null) overlayView.updateBoxes(new ArrayList<>());
        isProcessingCapture = false;
        showLoading(false, "");
        if (tvHasil != null) tvHasil.setText("Arahkan kamera ke objek sampah untuk memulai deteksi");
        Toast.makeText(getContext(), "Cache scan direset ✓", Toast.LENGTH_SHORT).show();
    }

    // ─── Camera setup ─────────────────────────────────────────────────────────

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Gagal mendapatkan camera provider", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();
        imageAnalysis.setAnalyzer(analysisExecutor, this::processImageProxy);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Gagal binding camera use cases", e);
        }
    }

    // ─── Frame analysis — bounding box muncul dulu ───────────────────────────

    private void processImageProxy(ImageProxy image) {
        if (isProcessingCapture || tflite == null) {
            image.close();
            return;
        }

        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastAnalysisTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastAnalysisTime = now;
        }

        try {
            Bitmap bitmap = image.toBitmap();
            if (bitmap != null) {
                int rotation = image.getImageInfo().getRotationDegrees();
                Bitmap rotated = bitmap;
                if (rotation != 0) {
                    rotated = getRotatedBitmapBuffer(bitmap, rotation);
                    bitmap.recycle(); // Immediately recycle the raw frame bitmap
                }
                final Bitmap finalBitmap = rotated;

                List<TFLiteHelper.Result> detections = tflite.detect(finalBitmap);
                latestDetections = detections;
                currentBitmap    = finalBitmap;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (overlayView != null) {
                            overlayView.setFrameInfo(finalBitmap.getWidth(),
                                    finalBitmap.getHeight(), fps);
                            overlayView.updateBoxes(detections);
                        }
                        if (!isProcessingCapture) updateResultText(detections);
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        } finally {
            image.close();
        }
    }

    private Bitmap getRotatedBitmapBuffer(Bitmap source, int rotation) {
        int w = source.getWidth();
        int h = source.getHeight();
        int targetW = (rotation == 90 || rotation == 270) ? h : w;
        int targetH = (rotation == 90 || rotation == 270) ? w : h;

        if (rotatedBitmapBuffer == null || rotatedBitmapBuffer.getWidth() != targetW || rotatedBitmapBuffer.getHeight() != targetH) {
            if (rotatedBitmapBuffer != null) {
                rotatedBitmapBuffer.recycle();
            }
            rotatedBitmapBuffer = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
            rotatedBitmapCanvas = new android.graphics.Canvas(rotatedBitmapBuffer);
        }

        rotatedBitmapCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        rotatedBitmapCanvas.save();
        if (rotation == 90) {
            rotatedBitmapCanvas.translate(targetW, 0);
            rotatedBitmapCanvas.rotate(90);
        } else if (rotation == 180) {
            rotatedBitmapCanvas.translate(targetW, targetH);
            rotatedBitmapCanvas.rotate(180);
        } else if (rotation == 270) {
            rotatedBitmapCanvas.translate(0, targetH);
            rotatedBitmapCanvas.rotate(270);
        }
        rotatedBitmapCanvas.drawBitmap(source, 0, 0, null);
        rotatedBitmapCanvas.restore();

        return rotatedBitmapBuffer;
    }

    // ─── Gallery multi-select ─────────────────────────────────────────────────

    private void openGalleryMultiIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // ← multi-select
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(intent);
    }

    private void processImageUri(@Nullable Uri uri, List<Uri> allUris) {
        if (uri == null) return;

        showLoading(true, "Memproses gambar...");
        analysisExecutor.execute(() -> {
            try {
                Bitmap bitmap = readBitmap(uri);
                if (bitmap != null) {
                    currentBitmap = bitmap;
                    List<TFLiteHelper.Result> detections = tflite.detect(bitmap);
                    if (isAdded()) requireActivity().runOnUiThread(() -> {
                        showLoading(false, "");
                        if (!detections.isEmpty()) {
                            openResultMulti(detections, allUris);
                        } else {
                            updateResultText(detections);
                            Toast.makeText(getContext(),
                                    "Tidak ada sampah terdeteksi", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> showLoading(false, ""));
            }
        });
    }

    // ─── Open result ──────────────────────────────────────────────────────────

    private void openResult(List<TFLiteHelper.Result> detections) {
        openResultMulti(detections, null);
    }

    private void openResultMulti(List<TFLiteHelper.Result> detections, @Nullable List<Uri> extraUris) {
        if (currentBitmap == null || detections.isEmpty() || !isAdded()) return;
        try {
            TFLiteHelper.Result dominant = findDominant(detections);
            String nama       = dominant.label;
            float  confidence = dominant.confidence;
            String kategori   = mapCategory(nama);

            File file = new File(requireContext().getCacheDir(), "scan.jpg");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            }

            Intent intent = new Intent(requireContext(), ResultActivity.class);
            intent.putExtra("imagePath",   file.getAbsolutePath());
            intent.putExtra("nama",        nama);
            intent.putExtra("confidence",  confidence);
            intent.putExtra("kategori",    kategori);
            intent.putExtra("saran",       buildSaran(nama));
            intent.putExtra("funfact",     buildFunfact(nama));
            intent.putExtra("sourceWidth", currentBitmap.getWidth());
            intent.putExtra("sourceHeight",currentBitmap.getHeight());
            putFrozenDetections(intent, detections);

            // Kirim URI extra untuk multi-photo jika ada
            if (extraUris != null && extraUris.size() > 1) {
                ArrayList<String> uriStrings = new ArrayList<>();
                for (Uri u : extraUris) uriStrings.add(u.toString());
                intent.putStringArrayListExtra("extraPhotoUris", uriStrings);
            }

            startActivity(intent);
            isProcessingCapture = false;
            showLoading(false, "");
        } catch (Exception e) {
            isProcessingCapture = false;
            showLoading(false, "");
            Log.e(TAG, "Gagal membuka hasil", e);
        }
    }

    // ─── Helper methods ───────────────────────────────────────────────────────

    private Bitmap readBitmap(Uri uri) throws Exception {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (inputStream != null) inputStream.close();

        int rotation = readRotation(uri);
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    private int readRotation(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return 0;
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90)  return 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {}
        return 0;
    }

    private void updateResultText(List<TFLiteHelper.Result> detections) {
        if (tvHasil == null) return;
        if (detections.isEmpty()) {
            tvHasil.setText("Arahkan kamera ke objek sampah untuk memulai deteksi");
            return;
        }
        TFLiteHelper.Result dominant = findDominant(detections);
        tvHasil.setText(String.format(Locale.US,
                "✓ Objek terdeteksi: %s\nSilakan ambil foto untuk melanjutkan analisis",
                dominant.label));
    }

    private TFLiteHelper.Result findDominant(List<TFLiteHelper.Result> detections) {
        TFLiteHelper.Result dominant = detections.get(0);
        for (TFLiteHelper.Result result : detections) {
            if (result.confidence > dominant.confidence) dominant = result;
        }
        return dominant;
    }

    private String mapCategory(String label) {
        if ("Organik".equalsIgnoreCase(label))  return "Organik";
        if ("Kardus".equalsIgnoreCase(label) || "Kaca".equalsIgnoreCase(label)
                || "Logam".equalsIgnoreCase(label) || "Kertas".equalsIgnoreCase(label))
            return "Recycle";
        if ("Plastik".equalsIgnoreCase(label))  return "Anorganik";
        if ("Bukan Sampah".equalsIgnoreCase(label)) return "Bukan Sampah";
        return "Anorganik";
    }

    private String buildSaran(String nama) {
        switch (nama.toLowerCase()) {
            case "organik": return "🌿 Kumpulkan sisa makanan, sayuran, dan buah ke wadah kompos. Sampah organik bisa diolah menjadi pupuk kompos yang menyuburkan tanaman. Hindari mencampurnya dengan plastik atau logam.";
            case "kardus":  return "📦 Lipat kardus hingga pipih, pastikan dalam keadaan kering dan bersih. Serahkan ke bank sampah atau pengepul untuk didaur ulang menjadi produk baru.";
            case "kertas":  return "📄 Pastikan kertas kering dan tidak terkontaminasi minyak sebelum dikumpulkan. Pisahkan berdasarkan jenis untuk memudahkan daur ulang. Gunakan kedua sisi kertas saat mencetak.";
            case "kaca":    return "🔍 Bilas wadah kaca bersih dan pisahkan dari sampah lain. Bungkus pecahan kaca dengan koran untuk menghindari cedera. Botol kaca bisa digunakan ulang sebagai wadah penyimpanan.";
            case "plastik": return "♻️ Bersihkan dan remas botol plastik untuk menghemat ruang. Pisahkan berdasarkan jenis (PET, HDPE, PP). Kurangi plastik sekali pakai dan beralih ke alternatif reusable.";
            case "logam":   return "🔧 Kumpulkan kaleng dan logam dalam wadah terpisah. Bilas kaleng bekas sebelum dikumpulkan. Logam bernilai tinggi untuk didaur ulang, serahkan ke pengepul atau bank sampah.";
            case "bukan sampah": return "✅ Objek ini terdeteksi bukan sebagai sampah. Pastikan barang masih layak digunakan. Jika tidak diperlukan, pertimbangkan untuk mendonasikannya.";
            default:        return "Pisahkan sesuai kategori agar proses pengelolaan lebih mudah.";
        }
    }

    private String buildFunfact(String nama) {
        switch (nama.toLowerCase()) {
            case "organik": return "🌱 Sampah organik menyumbang 60% total sampah di Indonesia! Jika dikelola jadi kompos, bisa mengurangi emisi gas metana di TPA. 1 ton kompos menyuburkan hingga 1 hektar lahan.";
            case "kardus":  return "📦 Mendaur ulang 1 ton kardus menghemat 17 pohon besar, 26.000 liter air, dan 4.000 kWh listrik. Kardus bisa didaur ulang 5-7 kali sebelum seratnya terlalu pendek.";
            case "kertas":  return "📄 Setiap orang menggunakan rata-rata 55 kg kertas per tahun. Mendaur ulang kertas menghemat 70% energi. Satu pohon besar menghasilkan sekitar 8.000 lembar kertas HVS.";
            case "kaca":    return "🔍 Kaca bisa didaur ulang 100% tanpa kehilangan kualitas! Botol kaca daur ulang bisa kembali jadi botol baru dalam 30 hari. Menghemat 30% energi dibanding dari bahan mentah.";
            case "plastik": return "⚠️ 1 botol plastik butuh 450 tahun untuk terurai! Setiap tahun 8 juta ton plastik berakhir di lautan. Indonesia penyumbang sampah plastik laut terbesar kedua di dunia.";
            case "logam":   return "🔧 Mendaur ulang aluminium menghemat 95% energi! Kaleng aluminium bisa kembali ke rak toko dalam 60 hari. 75% aluminium yang pernah diproduksi masih digunakan hari ini.";
            case "bukan sampah": return "💡 Memperpanjang umur barang adalah cara paling efektif mengurangi jejak karbon. Memperbaiki daripada membuang menghemat rata-rata 5 kg CO₂ per barang.";
            default:        return "Pemilahan kecil di rumah membantu pengelolaan sampah kota.";
        }
    }

    private void showLoading(boolean isLoading, String message) {
        if (layoutScanProgress != null) {
            layoutScanProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (progressScan != null) {
            progressScan.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (tvScanProgress != null) {
            tvScanProgress.setText(message == null ? "" : message);
        }
        if (btnCapture != null) btnCapture.setEnabled(!isLoading);
        if (btnGallery != null) btnGallery.setEnabled(!isLoading);
        if (btnReset != null) btnReset.setEnabled(!isLoading);
    }

    private void putFrozenDetections(Intent intent, List<TFLiteHelper.Result> detections) {
        int count = detections.size();
        float[] left = new float[count], top = new float[count],
                right = new float[count], bottom = new float[count],
                confidence = new float[count];
        String[] labels = new String[count];

        for (int i = 0; i < count; i++) {
            TFLiteHelper.Result result = detections.get(i);
            RectF rect = result.rect;
            left[i] = rect.left; top[i] = rect.top;
            right[i] = rect.right; bottom[i] = rect.bottom;
            confidence[i] = result.confidence;
            labels[i] = result.label;
        }

        intent.putExtra("frozenDetectionCount", count);
        intent.putExtra("frozenLeft",       left);
        intent.putExtra("frozenTop",        top);
        intent.putExtra("frozenRight",      right);
        intent.putExtra("frozenBottom",     bottom);
        intent.putExtra("frozenConfidence", confidence);
        intent.putExtra("frozenLabels",     labels);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (analysisExecutor != null) analysisExecutor.shutdown();
        if (tflite != null) tflite.close();
        if (rotatedBitmapBuffer != null) {
            rotatedBitmapBuffer.recycle();
            rotatedBitmapBuffer = null;
            rotatedBitmapCanvas = null;
        }
    }
}
