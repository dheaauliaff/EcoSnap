package com.example.ecosnap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int PERMISSION_CODE = 200;

    ImageView imgHasil;
    TFLiteHelper tflite;

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
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            imgHasil.setImageBitmap(bitmap);

            List<TFLiteHelper.Result> detections = tflite.detect(bitmap);

            if (detections.isEmpty()) {
                Toast.makeText(this, "Model belum yakin", Toast.LENGTH_SHORT).show();
            }

            String nama = detections.isEmpty() ? "Tidak terdeteksi" : detections.get(0).label;

            String kategori = nama.equals("Organik") ? "Organik" : "Anorganik";

            String saran = "Buang sesuai jenisnya";
            String funfact = "Dideteksi oleh AI";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("imageBase64", imageBase64);
            intent.putExtra("nama", nama);
            intent.putExtra("kategori", kategori);
            intent.putExtra("saran", saran);
            intent.putExtra("funfact", funfact);

            intent.putExtra("detections", new ArrayList<>(detections));

            startActivity(intent);
        }
    }
}