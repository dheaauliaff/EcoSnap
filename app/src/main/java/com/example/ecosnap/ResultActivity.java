package com.example.ecosnap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
            String kategori = getIntent().getStringExtra("kategori");
            String saran = getIntent().getStringExtra("saran");
            String funfact = getIntent().getStringExtra("funfact");

            Bitmap bitmap = null;

            if (imageBase64 != null) {
                byte[] decoded = Base64.decode(imageBase64, Base64.DEFAULT);

                // 🔥 decode aman (anti crash)
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // kecilin gambar biar ringan
                bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length, options);

                imgHasil.setImageBitmap(bitmap);
            }

            tvNama.setText("Nama: " + safe(nama));
            tvKategori.setText("Kategori: " + safe(kategori));
            tvSaran.setText("Saran: " + safe(saran));
            tvFunfact.setText("Fun Fact: " + safe(funfact));

            // 🔥 DETEKSI DI SINI (AMAN)
            if (bitmap != null) {
                TFLiteHelper helper = new TFLiteHelper(this);
                List<TFLiteHelper.Result> results = helper.detect(bitmap);

                overlayView.setResults(results);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Terjadi error", Toast.LENGTH_LONG).show();
        }
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }
}