package com.example.ecosnap.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.ecosnap.R;
import com.example.ecosnap.ScanHistory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RiwayatScanAdapter extends RecyclerView.Adapter<RiwayatScanAdapter.ViewHolder> {

    private final List<ScanHistory> items;
    private final Context context;

    public RiwayatScanAdapter(Context context, List<ScanHistory> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_riwayat_scan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ScanHistory item = items.get(position);

        // Nama Sampah
        h.tvNamaSampah.setText(safe(item.getJenisSampah()));

        // Kategori
        String kategori = safe(item.getKategori());
        h.tvKategori.setText(kategori);
        h.tvKategori.getBackground().setTint(getKategoriColor(kategori));
        h.tvKategori.setTextColor(getKategoriTextColor(kategori));

        // Wilayah
        String wilayah = "";
        if (item.getRwId() != null && !item.getRwId().isEmpty()) {
            wilayah = item.getRwId();
        }
        if (item.getRtId() != null && !item.getRtId().isEmpty()) {
            wilayah += (wilayah.isEmpty() ? "" : " / ") + item.getRtId();
        }
        h.tvWilayah.setText(wilayah.isEmpty() ? "-" : wilayah);

        // Tanggal
        h.tvTanggal.setText(formatDate(item.getCreatedAt()));

        // Confidence
        Float conf = item.getAkurasi();
        if (conf != null && conf > 0) {
            h.tvConfidence.setText(String.format(Locale.US, "%.0f%%", conf));
        } else {
            h.tvConfidence.setText("-");
        }

        // Image dari Cloudinary
        String imageUrl = item.getFotoUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .placeholder(R.drawable.ic_scan)
                    .error(R.drawable.ic_scan)
                    .into(h.ivThumbnail);
        } else {
            h.ivThumbnail.setImageResource(R.drawable.ic_scan);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvNamaSampah, tvKategori, tvWilayah, tvTanggal, tvConfidence;

        ViewHolder(@NonNull View v) {
            super(v);
            ivThumbnail   = v.findViewById(R.id.ivThumbnail);
            tvNamaSampah  = v.findViewById(R.id.tvNamaSampah);
            tvKategori    = v.findViewById(R.id.tvKategori);
            tvWilayah     = v.findViewById(R.id.tvWilayah);
            tvTanggal     = v.findViewById(R.id.tvTanggal);
            tvConfidence  = v.findViewById(R.id.tvConfidence);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "-";
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            input.setTimeZone(TimeZone.getTimeZone("UTC"));
            // Handle fractional seconds and timezone suffix
            String clean = isoDate;
            if (clean.contains(".")) {
                clean = clean.substring(0, clean.indexOf('.'));
            }
            if (clean.contains("+")) {
                clean = clean.substring(0, clean.indexOf('+'));
            }
            Date date = input.parse(clean);
            if (date == null) return isoDate;

            SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
            output.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            return output.format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    private int getKategoriColor(String kategori) {
        if (kategori == null) return 0xFFE8F5E9;
        switch (kategori.toLowerCase()) {
            case "organik":       return 0xFFE8F5E9;
            case "anorganik":     return 0xFFFFF3E0;
            case "recycle":       return 0xFFE3F2FD;
            case "bukan sampah":  return 0xFFFFEBEE;
            default:              return 0xFFF5F5F5;
        }
    }

    private int getKategoriTextColor(String kategori) {
        if (kategori == null) return 0xFF2E7D32;
        switch (kategori.toLowerCase()) {
            case "organik":       return 0xFF2E7D32;
            case "anorganik":     return 0xFFE65100;
            case "recycle":       return 0xFF1565C0;
            case "bukan sampah":  return 0xFFC62828;
            default:              return 0xFF616161;
        }
    }
}
