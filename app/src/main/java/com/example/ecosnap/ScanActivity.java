package com.example.ecosnap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int PERMISSION_CODE = 200;

    ImageView imgHasil;
    TFLiteHelper tflite;

    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        imgHasil = findViewById(R.id.imgHasil);
        tflite = new TFLiteHelper(this);

        checkPermissionAndOpenCamera();
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CODE);

        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = new File(getExternalFilesDir(null),
                "IMG_" + System.currentTimeMillis() + ".jpg");

        photoUri = FileProvider.getUriForFile(
                this,
                "com.example.ecosnap.provider",
                photoFile
        );

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            Bitmap bitmap = null;

            // 🔥 AMANIN DATA
            if (data != null && data.getExtras() != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            // 🔥 CEK NULL (WAJIB)
            if (bitmap == null) {
                Toast.makeText(this, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show();
                return;
            }

            imgHasil.setImageBitmap(bitmap);

            // 🔥 convert ke base64 (AMAN)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            // 🔥 DETEKSI MODEL
            List<TFLiteHelper.Result> detections = tflite.detect(bitmap);

            String nama = "Tidak terdeteksi";
            float maxConf = 0;

            for (TFLiteHelper.Result r : detections) {
                if (r.confidence > maxConf) {
                    maxConf = r.confidence;
                    nama = r.label;
                }
            }

            // 🔥 kategori
            String kategori = nama.equals("Organik") ? "Organik" :
                    (nama.equals("Bukan Sampah") ? "Bukan Sampah" : "Anorganik");

            String saran = "Buang sesuai jenisnya";
            String funfact = "Jaga lingkungan tetap bersih";

            // 🔥 pindah ke result
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("imageBase64", imageBase64);
            intent.putExtra("nama", nama);
            intent.putExtra("kategori", kategori);
            intent.putExtra("saran", saran);
            intent.putExtra("funfact", funfact);

            startActivity(intent);
        }
    }
}