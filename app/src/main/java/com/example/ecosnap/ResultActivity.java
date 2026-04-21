package com.example.ecosnap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    private ImageView imgHasil;
    private OverlayView overlayView;
    private TextView tvNamaSampah, tvKategori, tvSaran, tvFunfact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imgHasil = findViewById(R.id.imgHasil);
        overlayView = findViewById(R.id.overlayView);
        tvNamaSampah = findViewById(R.id.tvNamaSampah);
        tvKategori = findViewById(R.id.tvKategori);
        tvSaran = findViewById(R.id.tvSaran);
        tvFunfact = findViewById(R.id.tvFunfact);

        String imageBase64 = getIntent().getStringExtra("imageBase64");
        String nama = getIntent().getStringExtra("nama");
        String kategori = getIntent().getStringExtra("kategori");
        String saran = getIntent().getStringExtra("saran");
        String funfact = getIntent().getStringExtra("funfact");

        if (imageBase64 != null) {
            byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imgHasil.setImageBitmap(bitmap);
        }

        tvNamaSampah.setText("Nama sampah: " + safe(nama));
        tvKategori.setText("Kategori: " + safe(kategori));
        tvSaran.setText("Saran: " + safe(saran));
        tvFunfact.setText("Fun fact: " + safe(funfact));

        ArrayList<OverlayView.DetectionBox> listBox = new ArrayList<>();
        listBox.add(new OverlayView.DetectionBox(new RectF(120, 80, 420, 430), safe(nama)));
        overlayView.setResults(listBox);
    }

    private String safe(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }
}