package com.example.ecosnap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

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

        String imageBase64 = getIntent().getStringExtra("imageBase64");
        String nama = getIntent().getStringExtra("nama");
        String kategori = getIntent().getStringExtra("kategori");
        String saran = getIntent().getStringExtra("saran");
        String funfact = getIntent().getStringExtra("funfact");

        if (imageBase64 != null) {
            byte[] decoded = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            imgHasil.setImageBitmap(bitmap);
        }

        tvNama.setText("Nama: " + safe(nama));
        tvKategori.setText("Kategori: " + safe(kategori));
        tvSaran.setText("Saran: " + safe(saran));
        tvFunfact.setText("Fun Fact: " + safe(funfact));

        // 🔥 AMBIL BOX
        ArrayList<TFLiteHelper.Result> results =
                (ArrayList<TFLiteHelper.Result>) getIntent().getSerializableExtra("detections");

        if (results != null) {
            overlayView.setResults(results);
        }
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }
}