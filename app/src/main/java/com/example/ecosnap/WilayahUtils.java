package com.example.ecosnap;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Locale;

public final class WilayahUtils {

    private WilayahUtils() {}

    public static String formatRtId(String rtId) {
        if (rtId == null) return "";
        String clean = rtId.trim().replaceAll("(?i)^rt\\s*", "").trim();
        if (clean.isEmpty()) return "";
        try {
            return "RT " + String.format(Locale.US, "%02d", Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toUpperCase(Locale.US).startsWith("RT") ? clean : "RT " + clean;
        }
    }

    public static String formatRwId(String rwId) {
        if (rwId == null) return "";
        String clean = rwId.trim().replaceAll("(?i)^rw\\s*", "").trim();
        if (clean.isEmpty()) return "";
        try {
            return "RW " + String.format(Locale.US, "%02d", Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toUpperCase(Locale.US).startsWith("RW") ? clean : "RW " + clean;
        }
    }

    public static boolean isMatchingRw(String scanRwId, String filterRwId) {
        String scan = normalizeRw(scanRwId);
        String filter = normalizeRw(filterRwId);
        return !scan.isEmpty() && scan.equals(filter);
    }

    public static boolean isMatchingRt(String scanRtId, String filterRtId) {
        String scan = normalizeRt(scanRtId);
        String filter = normalizeRt(filterRtId);
        return !scan.isEmpty() && scan.equals(filter);
    }

    public static String normalizeRw(String rw) {
        if (rw == null) return "";
        String clean = rw.trim().replaceAll("(?i)^rw\\s*", "").trim();
        try {
            return String.valueOf(Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toLowerCase(Locale.US);
        }
    }

    public static String normalizeRt(String rt) {
        if (rt == null) return "";
        String clean = rt.trim().replaceAll("(?i)^rt\\s*", "").trim();
        try {
            return String.valueOf(Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toLowerCase(Locale.US);
        }
    }

    public static String normalizeJenis(String jenisSampah) {
        if (jenisSampah == null) return "";
        String clean = jenisSampah.toLowerCase(Locale.US).replace("_", " ").trim();
        if (clean.contains("organik")) return "Organik";
        if (clean.contains("kardus")) return "Kardus";
        if (clean.contains("kaca")) return "Kaca";
        if (clean.contains("logam") || clean.contains("kaleng")) return "Logam";
        if (clean.contains("kertas")) return "Kertas";
        if (clean.contains("plastik") || clean.contains("botol")) return "Plastik";
        if (clean.contains("bukan")) return "Bukan Sampah";
        return jenisSampah.trim();
    }

    public static ValueFormatter integerValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf(Math.round(value));
            }
        };
    }
}
