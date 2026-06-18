package com.example.ecosnap.user;

import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.ecosnap.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DetailScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_scan);

        ImageView imgDetail       = findViewById(R.id.imgDetail);
        TextView tvDetailNama     = findViewById(R.id.tvDetailNama);
        TextView tvDetailKategori = findViewById(R.id.tvDetailKategori);
        TextView tvDetailConfidence = findViewById(R.id.tvDetailConfidence);
        TextView tvDetailTanggal  = findViewById(R.id.tvDetailTanggal);
        TextView tvDetailSaran    = findViewById(R.id.tvDetailSaran);
        TextView tvDetailFunfact  = findViewById(R.id.tvDetailFunfact);
        TextView tvDetailDampak   = findViewById(R.id.tvDetailDampak);
        TextView tvDetectionName  = findViewById(R.id.tvDetectionName);
        TextView tvDetectionPercent = findViewById(R.id.tvDetectionPercent);
        View viewDetectionDot     = findViewById(R.id.viewDetectionDot);
        View viewDetectionFill    = findViewById(R.id.viewDetectionFill);
        View viewDetectionEmpty   = findViewById(R.id.viewDetectionEmpty);
        ImageView btnBack         = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        android.view.View btnInfoSaran = findViewById(R.id.btnInfoSaran);
        if (btnInfoSaran != null) {
            btnInfoSaran.setOnClickListener(v -> showInfoDialog());
        }

        // Ambil data dari Intent
        String nama      = getIntent().getStringExtra("nama");
        String kategori  = getIntent().getStringExtra("kategori");
        float confidence = getIntent().getFloatExtra("confidence", 0f);
        String tanggal   = getIntent().getStringExtra("tanggal");
        String imageUrl  = getIntent().getStringExtra("imageUrl");

        // Isi data ke UI
        tvDetailNama.setText(safe(nama));
        tvDetailNama.setTextColor(getJenisColor(nama));

        tvDetailKategori.setText(safe(kategori));
        tvDetailKategori.getBackground().setTint(getKategoriColor(kategori));

        if (confidence > 0) {
            tvDetailConfidence.setText(String.format(Locale.US, "%.0f%%", confidence));
        } else {
            tvDetailConfidence.setText("-");
        }

        tvDetailTanggal.setText(formatDate(tanggal));
        bindDetectionBreakdown(
                tvDetectionName,
                tvDetectionPercent,
                viewDetectionDot,
                viewDetectionFill,
                viewDetectionEmpty,
                nama,
                confidence
        );

        // Load gambar dari Cloudinary
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .transform(new CenterCrop(), new RoundedCorners(32))
                    .placeholder(R.drawable.ic_scan)
                    .error(R.drawable.ic_scan)
                    .into(imgDetail);
        }

        // Isi konten edukatif berdasarkan jenis sampah
        tvDetailSaran.setText(buildSaran(nama));
        tvDetailFunfact.setText(buildFunfact(nama));
        tvDetailDampak.setText(buildDampak(nama));
    }

    private void showInfoDialog() {
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_info_penanganan, null);
        TextView tvDialogSaran = dialogView.findViewById(R.id.tvDialogSaran);
        TextView tvDialogFunfact = dialogView.findViewById(R.id.tvDialogFunfact);

        TextView tvSaran = findViewById(R.id.tvDetailSaran);
        TextView tvFunfact = findViewById(R.id.tvDetailFunfact);

        if (tvDialogSaran != null && tvSaran != null) {
            tvDialogSaran.setText(tvSaran.getText());
        }
        if (tvDialogFunfact != null && tvFunfact != null) {
            tvDialogFunfact.setText(tvFunfact.getText());
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        android.view.View btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);
        if (btnDialogClose != null) {
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        }

        android.view.View btnDialogOke = dialogView.findViewById(R.id.btnDialogOke);
        if (btnDialogOke != null) {
            btnDialogOke.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    // ─── Saran Penanganan per jenis sampah ─────────────────────────────────

    private String buildSaran(String nama) {
        if (nama == null) return defaultSaran();
        switch (nama.toLowerCase()) {
            case "organik":
                return "🌿 Kumpulkan sisa makanan, sayuran, dan buah-buahan ke dalam wadah khusus kompos. " +
                       "Sampah organik bisa diolah menjadi pupuk kompos yang sangat bermanfaat untuk menyuburkan tanaman. " +
                       "Hindari mencampurnya dengan sampah plastik atau logam. " +
                       "Jika memungkinkan, gunakan komposter rumahan untuk mengelola sampah organik secara mandiri.";
            case "kardus":
                return "📦 Lipat kardus hingga pipih agar tidak memakan banyak ruang saat dikumpulkan. " +
                       "Pastikan kardus dalam keadaan kering dan bersih dari sisa makanan atau cairan. " +
                       "Serahkan ke bank sampah atau pengepul untuk didaur ulang menjadi produk kertas baru. " +
                       "Kardus bekas juga bisa dikreasikan menjadi kerajinan tangan atau wadah penyimpanan.";
            case "kertas":
                return "📄 Pastikan kertas dalam keadaan kering dan tidak terkontaminasi minyak atau makanan sebelum dikumpulkan. " +
                       "Pisahkan kertas berdasarkan jenis (HVS, koran, majalah) untuk memudahkan proses daur ulang. " +
                       "Manfaatkan kertas bekas sebagai bahan kerajinan atau untuk keperluan lain sebelum dibuang. " +
                       "Gunakan kedua sisi kertas saat mencetak untuk mengurangi limbah kertas.";
            case "kaca":
                return "🔍 Bilas wadah kaca hingga bersih sebelum dikumpulkan dan pisahkan dari sampah lainnya. " +
                       "Bungkus pecahan kaca dengan koran atau kain tebal untuk menghindari cedera saat penanganan. " +
                       "Jangan campur kaca berwarna dengan kaca bening karena memiliki proses daur ulang berbeda. " +
                       "Botol atau toples kaca bisa digunakan kembali sebagai wadah penyimpanan di rumah.";
            case "plastik":
                return "♻️ Bersihkan dan remas botol atau wadah plastik untuk menghemat ruang penyimpanan. " +
                       "Pisahkan plastik berdasarkan jenisnya (PET, HDPE, PP) yang biasanya tertera di bagian bawah wadah. " +
                       "Kurangi penggunaan plastik sekali pakai dan beralih ke alternatif yang dapat digunakan berulang kali. " +
                       "Serahkan plastik bersih ke bank sampah atau fasilitas daur ulang terdekat.";
            case "logam":
                return "🔧 Kumpulkan kaleng, tutup botol, dan benda logam lainnya dalam satu wadah terpisah. " +
                       "Bilas kaleng bekas minuman atau makanan sebelum dikumpulkan untuk menghindari bau tidak sedap. " +
                       "Logam merupakan material yang sangat berharga untuk didaur ulang dan memiliki nilai jual tinggi. " +
                       "Serahkan ke pengepul atau bank sampah untuk proses daur ulang yang tepat.";
            case "bukan sampah":
                return "✅ Objek ini terdeteksi bukan sebagai sampah. Pastikan barang ini masih layak digunakan. " +
                       "Jika sudah tidak diperlukan, pertimbangkan untuk mendonasikan kepada yang membutuhkan. " +
                       "Sebelum membuang sesuatu, pikirkan apakah barang tersebut masih bisa dimanfaatkan atau diperbaiki. " +
                       "Memperpanjang umur penggunaan barang adalah cara terbaik untuk mengurangi produksi sampah.";
            default:
                return defaultSaran();
        }
    }

    // ─── Fun Fact per jenis sampah ─────────────────────────────────────────

    private String buildFunfact(String nama) {
        if (nama == null) return defaultFunfact();
        switch (nama.toLowerCase()) {
            case "organik":
                return "🌱 Tahukah kamu? Sampah organik menyumbang sekitar 60% dari total sampah di Indonesia! " +
                       "Jika dikelola dengan baik menjadi kompos, sampah organik bisa mengurangi emisi gas metana di TPA " +
                       "dan menghasilkan pupuk berkualitas tinggi yang setara dengan pupuk komersial. " +
                       "1 ton kompos bisa menyuburkan hingga 1 hektar lahan pertanian.";
            case "kardus":
                return "📦 Mendaur ulang 1 ton kardus bisa menghemat 17 pohon besar, 26.000 liter air, " +
                       "dan 4.000 kWh listrik. Kardus merupakan salah satu material yang paling mudah didaur ulang " +
                       "dan bisa diproses ulang hingga 5-7 kali sebelum seratnya menjadi terlalu pendek. " +
                       "Indonesia menghasilkan jutaan ton limbah kardus dari e-commerce setiap tahunnya.";
            case "kertas":
                return "📄 Setiap orang di dunia menggunakan rata-rata 55 kg kertas per tahun. " +
                       "Mendaur ulang kertas menghemat 70% energi dibandingkan membuat kertas dari bahan baku baru. " +
                       "Satu pohon besar bisa menghasilkan sekitar 8.000 lembar kertas HVS. " +
                       "Kertas yang didaur ulang bisa digunakan untuk membuat tisu, karton, hingga bahan bangunan.";
            case "kaca":
                return "🔍 Kaca adalah material ajaib yang bisa didaur ulang 100% tanpa kehilangan kualitas, " +
                       "bahkan setelah diproses berulang kali! Botol kaca yang didaur ulang bisa kembali " +
                       "menjadi botol baru dalam waktu hanya 30 hari. Mendaur ulang kaca menghemat 30% energi " +
                       "dibandingkan membuat kaca dari bahan mentah (pasir silika).";
            case "plastik":
                return "⚠️ 1 botol plastik membutuhkan waktu hingga 450 tahun untuk terurai di alam! " +
                       "Setiap tahun, lebih dari 8 juta ton plastik berakhir di lautan dunia. " +
                       "Indonesia merupakan penyumbang sampah plastik laut terbesar kedua di dunia. " +
                       "Dengan memilah plastik dengan benar, kamu membantu mengurangi polusi yang mengancam 700 spesies laut.";
            case "logam":
                return "🔧 Mendaur ulang aluminium menghemat hingga 95% energi dibandingkan memproduksi dari bauksit! " +
                       "Sebuah kaleng aluminium yang didaur ulang bisa kembali ke rak toko dalam waktu 60 hari. " +
                       "Logam bisa didaur ulang berulang kali tanpa kehilangan kualitasnya. " +
                       "Seluruh aluminium yang pernah diproduksi manusia, 75%-nya masih digunakan hingga hari ini.";
            case "bukan sampah":
                return "💡 Memperpanjang umur penggunaan barang adalah salah satu cara paling efektif " +
                       "mengurangi jejak karbon. Memilih untuk memperbaiki daripada membuang bisa menghemat " +
                       "rata-rata 5 kg CO₂ per barang. Konsep 'circular economy' mendorong kita untuk " +
                       "meminimalkan limbah dengan menggunakan kembali dan mendaur ulang semua material.";
            default:
                return defaultFunfact();
        }
    }

    // ─── Dampak Lingkungan per jenis sampah ─────────────────────────────────

    private String buildDampak(String nama) {
        if (nama == null) return defaultDampak();
        switch (nama.toLowerCase()) {
            case "organik":
                return "Jika dibuang ke TPA tanpa diolah, sampah organik akan menghasilkan gas metana (CH₄) " +
                       "yang merupakan gas rumah kaca 25x lebih kuat dari CO₂. " +
                       "Dengan mengompos, kamu membantu mengurangi emisi gas rumah kaca dan " +
                       "mengembalikan nutrisi ke tanah secara alami. 🌍";
            case "kardus":
                return "Setiap ton kardus yang didaur ulang mengurangi 2.5 meter kubik ruang di TPA. " +
                       "Produksi kardus dari bahan daur ulang menghasilkan 25% lebih sedikit polusi udara " +
                       "dibandingkan dari kayu baru. Memilah kardus berarti menyelamatkan hutan! 🌳";
            case "kertas":
                return "Industri kertas menyumbang sekitar 4% emisi gas rumah kaca global. " +
                       "Mendaur ulang kertas mengurangi kebutuhan penebangan pohon yang merupakan " +
                       "paru-paru bumi dan habitat ribuan spesies. " +
                       "Setiap lembar kertas yang kamu daur ulang berkontribusi terhadap kelestarian hutan. 🌲";
            case "kaca":
                return "Kaca yang dibuang sembarangan memerlukan lebih dari 1 juta tahun untuk terurai! " +
                       "Namun, jika didaur ulang, kaca bisa diproses ulang tanpa batas waktu. " +
                       "Mendaur ulang kaca juga mengurangi penambangan pasir yang merusak ekosistem sungai dan pantai. 🏖️";
            case "plastik":
                return "Plastik yang tidak terkelola dengan baik mengancam kehidupan 700+ spesies laut. " +
                       "Mikroplastik sudah ditemukan di air minum, makanan, bahkan darah manusia. " +
                       "Setiap plastik yang kamu pilah dengan benar membantu mencegah kerusakan ekosistem " +
                       "yang tidak bisa diperbaiki. 🌊";
            case "logam":
                return "Penambangan logam mentah menghasilkan polusi air dan udara yang signifikan. " +
                       "Mendaur ulang 1 kaleng aluminium bisa menghemat energi yang cukup untuk " +
                       "menyalakan TV selama 3 jam. Setiap logam yang kamu kumpulkan untuk daur ulang " +
                       "membantu mengurangi kerusakan lingkungan dari aktivitas pertambangan. ⛏️";
            case "bukan sampah":
                return "Dengan tidak membuang barang yang masih layak pakai, kamu sudah mengurangi " +
                       "kebutuhan produksi barang baru yang memerlukan energi dan sumber daya alam. " +
                       "Setiap barang yang diperpanjang umurnya mengurangi jejak karbon dan " +
                       "membantu mewujudkan gaya hidup berkelanjutan. 🌿";
            default:
                return defaultDampak();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String defaultSaran() {
        return "Pisahkan sesuai kategori agar proses pengelolaan sampah lebih mudah dan efisien.";
    }

    private String defaultFunfact() {
        return "Pemilahan kecil di rumah membantu pengelolaan sampah kota secara keseluruhan.";
    }

    private String defaultDampak() {
        return "Setiap tindakan pemilahan sampah berkontribusi pada lingkungan yang lebih bersih dan sehat.";
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private int getKategoriColor(String kategori) {
        if (kategori == null) return 0xFF4CAF50;
        switch (kategori.toLowerCase()) {
            case "organik":       return 0xFF4CAF50;
            case "anorganik":     return 0xFFFF9800;
            case "recycle":       return 0xFF2196F3;
            case "bukan sampah":  return 0xFFFF5252;
            default:              return 0xFF9E9E9E;
        }
    }

    private void bindDetectionBreakdown(
            TextView tvName,
            TextView tvPercent,
            View dot,
            View fill,
            View empty,
            String nama,
            float confidence
    ) {
        int percent = Math.round(clamp(confidence, 0f, 100f));
        int color = getJenisColor(nama);

        if (tvName != null) {
            tvName.setText(safe(nama));
        }
        if (tvPercent != null) {
            tvPercent.setText(percent > 0 ? percent + "%" : "-");
            tvPercent.setTextColor(color);
        }
        if (dot != null) {
            dot.setBackground(makeRoundedDrawable(color, dp(99)));
        }
        if (fill != null && empty != null) {
            fill.setBackground(makeRoundedDrawable(color, dp(4)));
            LinearLayout.LayoutParams fillParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.max(percent, 0)
            );
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.max(100 - percent, 0)
            );
            fill.setLayoutParams(fillParams);
            empty.setLayoutParams(emptyParams);
        }
    }

    private int getJenisColor(String nama) {
        if (nama == null) return 0xFF9E9E9E;
        String clean = nama.toLowerCase(Locale.US).replace("_", " ").trim();
        if (clean.contains("organik")) return 0xFF4CAF50;
        if (clean.contains("plastik")) return 0xFFFF9800;
        if (clean.contains("kertas")) return 0xFFFFC107;
        if (clean.contains("kardus")) return 0xFF2196F3;
        if (clean.contains("kaca")) return 0xFF00BCD4;
        if (clean.contains("logam")) return 0xFF9C27B0;
        if (clean.contains("bukan")) return 0xFFFF5252;
        return 0xFF9E9E9E;
    }

    private GradientDrawable makeRoundedDrawable(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "-";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            input.setTimeZone(TimeZone.getTimeZone("UTC"));
            String clean = isoDate;
            if (clean.contains(".")) clean = clean.substring(0, clean.indexOf('.'));
            if (clean.contains("+")) clean = clean.substring(0, clean.indexOf('+'));
            Date date = input.parse(clean);
            if (date == null) return isoDate;
            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
            output.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            return output.format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
