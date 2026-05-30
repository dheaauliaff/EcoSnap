package com.example.ecosnap.user;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecosnap.R;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        LinearLayout btnContactWhatsApp = findViewById(R.id.btnContactWhatsApp);
        if (btnContactWhatsApp != null) {
            btnContactWhatsApp.setOnClickListener(v -> openWhatsApp());
        }

        LinearLayout btnContactEmail = findViewById(R.id.btnContactEmail);
        if (btnContactEmail != null) {
            btnContactEmail.setOnClickListener(v -> openEmail());
        }
    }

    private void openWhatsApp() {
        String phoneNumber = "6281234567890"; // Dummy admin phone number
        String message = "Halo Admin EcoSnap, saya butuh bantuan terkait aplikasi.";
        String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message);

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aplikasi WhatsApp tidak terinstal. Silakan hubungi " + phoneNumber, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Tidak dapat membuka WhatsApp.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmail() {
        String recipient = "support@ecosnap.id";
        String subject = "Pertanyaan / Kendala Aplikasi EcoSnap";
        
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);

        try {
            startActivity(Intent.createChooser(intent, "Kirim email dengan..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Tidak ditemukan aplikasi Email pada perangkat Anda.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal mengirim email.", Toast.LENGTH_SHORT).show();
        }
    }
}
