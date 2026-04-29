package com.example.ecosnap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    ImageView imgHasil;
    TextView tvNama, tvKategori, tvSaran, tvFunfact;
    OverlayView overlayView;

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

        try {
            String imageBase64 = getIntent().getStringExtra("imageBase64");
            String nama = getIntent().getStringExtra("nama");
            float confidence = getIntent().getFloatExtra("confidence", 0f);
            String saran = getIntent().getStringExtra("saran");
            String funfact = getIntent().getStringExtra("funfact");
            int sourceWidth = getIntent().getIntExtra("sourceWidth", 0);
            int sourceHeight = getIntent().getIntExtra("sourceHeight", 0);

            Bitmap bitmap = null;
            if (imageBase64 != null) {
                byte[] decoded = Base64.decode(imageBase64, Base64.DEFAULT);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length, options);

                imgHasil.setImageBitmap(bitmap);
            }

            tvNama.setText(safe(nama));
            tvKategori.setText("Tingkat Akurasi: " + String.format("%.0f", confidence) + "%");
            tvSaran.setText("Penanganan: " + safe(saran));
            tvFunfact.setText("Dampak Lingkungan: " + safe(funfact));

            List<TFLiteHelper.Result> frozenResults = readFrozenDetections();
            if (!frozenResults.isEmpty()) {
                overlayView.setImageSource(sourceWidth, sourceHeight);
                overlayView.setResults(frozenResults);
            } else if (bitmap != null) {
                TFLiteHelper helper = new TFLiteHelper(this);
                List<TFLiteHelper.Result> results = helper.detect(bitmap);
                overlayView.setResults(results);
                helper.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Terjadi error", Toast.LENGTH_LONG).show();
        }
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private List<TFLiteHelper.Result> readFrozenDetections() {
        List<TFLiteHelper.Result> results = new ArrayList<>();
        int count = getIntent().getIntExtra("frozenDetectionCount", 0);
        if (count <= 0) {
            return results;
        }

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

        if (left == null || top == null || right == null || bottom == null
                || confidence == null || classId == null || labels == null) {
            return results;
        }

        int safeCount = Math.min(count, Math.min(labels.length, Math.min(confidence.length, left.length)));
        for (int i = 0; i < safeCount; i++) {
            TFLiteHelper.Result result = new TFLiteHelper.Result(
                    new RectF(left[i], top[i], right[i], bottom[i]),
                    labels[i],
                    confidence[i],
                    classId[i]
            );
            if (trackingId != null && i < trackingId.length) result.trackingId = trackingId[i];
            if (stableFrames != null && i < stableFrames.length) result.stableFrames = stableFrames[i];
            if (locked != null && i < locked.length) result.isLocked = locked[i];
            if (lowConfidence != null && i < lowConfidence.length) result.isLowConfidence = lowConfidence[i];
            results.add(result);
        }
        return results;
    }
}
