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

    public static String formatAdminAreaFromLegacyRw(String rwId) {
        String normalized = normalizeRw(rwId);
        if (normalized.equals("-")) return "";
        return normalized.isEmpty() ? "" : formatRtId(normalized);
    }

    public static String formatWargaBadge(String legacyRwId) {
        String area = formatAdminAreaFromLegacyRw(legacyRwId);
        return area.isEmpty() ? "Warga" : "Warga • " + area;
    }

    public static String formatScanAreaLabel(String legacyRwId, String fallbackRtId) {
        String area = formatAdminAreaFromLegacyRw(legacyRwId);
        if (!area.isEmpty()) return area;
        return formatRtId(fallbackRtId);
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
        if (clean.equals("-")) return "";
        try {
            return String.valueOf(Integer.parseInt(clean));
        } catch (NumberFormatException e) {
            return clean.toLowerCase(Locale.US);
        }
    }

    public static String normalizeRt(String rt) {
        if (rt == null) return "";
        String clean = rt.trim().replaceAll("(?i)^rt\\s*", "").trim();
        if (clean.equals("-")) return "";
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

    public static final java.util.Comparator<String> RT_COMPARATOR = new java.util.Comparator<String>() {
        @Override
        public int compare(String rt1, String rt2) {
            if (rt1 == null && rt2 == null) return 0;
            if (rt1 == null) return -1;
            if (rt2 == null) return 1;
            if (rt1.equals(rt2)) return 0;
            
            int num1 = extractRtNumber(rt1);
            int num2 = extractRtNumber(rt2);
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
            return rt1.compareTo(rt2);
        }
        
        private int extractRtNumber(String rt) {
            if (rt == null) return 0;
            String clean = rt.replaceAll("\\D+", "");
            if (clean.isEmpty()) return 9999;
            try {
                return Integer.parseInt(clean);
            } catch (NumberFormatException e) {
                return 9999;
            }
        }
    };

    public static java.util.List<String> getRegisteredRtList(java.util.List<com.example.ecosnap.model.User> users, String rwId) {
        java.util.Set<String> rtSet = new java.util.TreeSet<>(RT_COMPARATOR);
        if (users != null) {
            for (com.example.ecosnap.model.User user : users) {
                if (user == null) continue;
                if (!"user".equalsIgnoreCase(user.getRole())) continue;
                if (!isMatchingRw(user.getRwId(), rwId)) continue;
                String rt = formatRtId(user.getRtId());
                if (!rt.isEmpty()) {
                    rtSet.add(rt);
                }
            }
        }
        return new java.util.ArrayList<>(rtSet);
    }

    public static java.util.List<String> getScanRtList(java.util.List<com.example.ecosnap.ScanHistory> scans, String rwId) {
        java.util.Set<String> rtSet = new java.util.TreeSet<>(RT_COMPARATOR);
        if (scans != null) {
            for (com.example.ecosnap.ScanHistory s : scans) {
                if (s == null) continue;
                if (rwId != null && !rwId.isEmpty() && !isMatchingRw(s.getRwId(), rwId)) continue;
                String rt = formatRtId(s.getRtId());
                if (!rt.isEmpty()) {
                    rtSet.add(rt);
                }
            }
        }
        return new java.util.ArrayList<>(rtSet);
    }
}
