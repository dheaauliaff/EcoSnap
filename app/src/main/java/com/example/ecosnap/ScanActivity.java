package com.example.ecosnap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class ScanActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int PERMISSION_CODE = 200;

    ImageView imgHasil;
    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        imgHasil = findViewById(R.id.imgHasil);

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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // WAJIB

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                openCamera();

            } else {
                Toast.makeText(this, "Permission kamera ditolak", Toast.LENGTH_SHORT).show();
            }
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
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); // WAJIB
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            // ✅ INI YANG PENTING
            imgHasil.setImageURI(photoUri);

            Toast.makeText(this,
                    "Foto berhasil ditampilkan",
                    Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this,
                    "Gagal mengambil foto",
                    Toast.LENGTH_SHORT).show();
        }
    }
}