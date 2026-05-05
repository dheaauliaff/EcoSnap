package com.example.ecosnap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SharedPrototypeData {

    private static SharedPrototypeData instance;

    private Map<String, Map<String, Integer>> wasteData;
    private String selectedRT = "RT 01";
    
    // Latest Scan Data
    private String latestScanJenis = "Plastik";
    private String latestScanKategori = "Anorganik";
    private String latestScanWaktu = "Hari ini, 20.20";

    public static final String[] CATEGORIES = {"Organik", "Kardus", "Kaca", "Logam", "Kertas", "Plastik"};

    private SharedPrototypeData() {
        wasteData = new HashMap<>();
        
        // Mock Data
        Map<String, Integer> rt01 = new HashMap<>();
        rt01.put("Organik", 45); rt01.put("Kardus", 12); rt01.put("Kaca", 8); rt01.put("Logam", 5); rt01.put("Kertas", 20); rt01.put("Plastik", 30);
        wasteData.put("RT 01", rt01);

        Map<String, Integer> rt02 = new HashMap<>();
        rt02.put("Organik", 30); rt02.put("Kardus", 25); rt02.put("Kaca", 10); rt02.put("Logam", 2); rt02.put("Kertas", 15); rt02.put("Plastik", 40);
        wasteData.put("RT 02", rt02);

        Map<String, Integer> rt03 = new HashMap<>();
        rt03.put("Organik", 60); rt03.put("Kardus", 10); rt03.put("Kaca", 5); rt03.put("Logam", 15); rt03.put("Kertas", 25); rt03.put("Plastik", 20);
        wasteData.put("RT 03", rt03);
        
        Map<String, Integer> rt04 = new HashMap<>();
        rt04.put("Organik", 20); rt04.put("Kardus", 40); rt04.put("Kaca", 15); rt04.put("Logam", 8); rt04.put("Kertas", 30); rt04.put("Plastik", 50);
        wasteData.put("RT 04", rt04);
        
        Map<String, Integer> rt05 = new HashMap<>();
        rt05.put("Organik", 55); rt05.put("Kardus", 20); rt05.put("Kaca", 12); rt05.put("Logam", 10); rt05.put("Kertas", 18); rt05.put("Plastik", 35);
        wasteData.put("RT 05", rt05);
        
        Map<String, Integer> rt06 = new HashMap<>();
        rt06.put("Organik", 15); rt06.put("Kardus", 25); rt06.put("Kaca", 20); rt06.put("Logam", 5); rt06.put("Kertas", 15); rt06.put("Plastik", 20);
        wasteData.put("RT 06", rt06);
        
        Map<String, Integer> rt07 = new HashMap<>();
        rt07.put("Organik", 40); rt07.put("Kardus", 15); rt07.put("Kaca", 10); rt07.put("Logam", 20); rt07.put("Kertas", 10); rt07.put("Plastik", 25);
        wasteData.put("RT 07", rt07);
        
        Map<String, Integer> rt08 = new HashMap<>();
        rt08.put("Organik", 35); rt08.put("Kardus", 30); rt08.put("Kaca", 5); rt08.put("Logam", 5); rt08.put("Kertas", 25); rt08.put("Plastik", 15);
        wasteData.put("RT 08", rt08);
    }

    public static synchronized SharedPrototypeData getInstance() {
        if (instance == null) {
            instance = new SharedPrototypeData();
        }
        return instance;
    }

    public Map<String, Map<String, Integer>> getWasteData() {
        return wasteData;
    }

    public void setSelectedRT(String rt) {
        this.selectedRT = rt;
    }

    public String getSelectedRT() {
        return selectedRT;
    }
    
    public Map<String, Integer> getSelectedRTData() {
        return wasteData.getOrDefault(selectedRT, new HashMap<>());
    }
    
    public void addScan(String rt, String jenis, String kategori, String waktu) {
        if (!wasteData.containsKey(rt)) {
            wasteData.put(rt, new HashMap<>());
        }
        Map<String, Integer> rtData = wasteData.get(rt);
        String mappedCategory = normalizeCategory(jenis, kategori);
        rtData.put(mappedCategory, rtData.getOrDefault(mappedCategory, 0) + 1);
        
        this.latestScanJenis = jenis;
        this.latestScanKategori = kategori;
        this.latestScanWaktu = waktu;
    }
    
    private String normalizeCategory(String jenis, String kategori) {
        String source = (jenis != null ? jenis : "") + " " + (kategori != null ? kategori : "");
        String normalized = source.toLowerCase(java.util.Locale.US);
        if (normalized.contains("organik")) return "Organik";
        if (normalized.contains("kardus")) return "Kardus";
        if (normalized.contains("kaca")) return "Kaca";
        if (normalized.contains("logam")) return "Logam";
        if (normalized.contains("kertas")) return "Kertas";
        if (normalized.contains("plastik")) return "Plastik";
        return "Organik";
    }

    public String getLatestScanJenis() { return latestScanJenis; }
    public String getLatestScanKategori() { return latestScanKategori; }
    public String getLatestScanWaktu() { return latestScanWaktu; }

    public int getTotalReportsForRT(String rt) {
        Map<String, Integer> data = wasteData.get(rt);
        if (data == null) return 0;
        int sum = 0;
        for (int v : data.values()) sum += v;
        return sum;
    }

    public String getDominantCategoryForRT(String rt) {
        Map<String, Integer> data = wasteData.get(rt);
        if (data == null) return "-";
        return calculateDominant(data);
    }

    public int getGlobalTotalReports() {
        int sum = 0;
        for (String rt : wasteData.keySet()) {
            sum += getTotalReportsForRT(rt);
        }
        return sum;
    }

    public LinkedHashMap<String, Integer> getGlobalCategoryTotals() {
        LinkedHashMap<String, Integer> totals = new LinkedHashMap<>();
        for (String cat : CATEGORIES) totals.put(cat, 0);
        
        for (Map<String, Integer> data : wasteData.values()) {
            for (String cat : CATEGORIES) {
                totals.put(cat, totals.get(cat) + data.getOrDefault(cat, 0));
            }
        }
        return totals;
    }

    public String getGlobalDominantCategory() {
        return calculateDominant(getGlobalCategoryTotals());
    }

    public int getGlobalPercentageForCategory(String category) {
        int total = getGlobalTotalReports();
        if (total == 0) return 0;
        int catTotal = getGlobalCategoryTotals().getOrDefault(category, 0);
        return Math.round((catTotal * 100f) / total);
    }

    private String calculateDominant(Map<String, Integer> data) {
        String best = "-";
        int max = 0;
        for (String cat : CATEGORIES) {
            int val = data.getOrDefault(cat, 0);
            if (val > max) {
                max = val;
                best = cat;
            }
        }
        return best;
    }
    
    public List<RTStat> getRTRanking() {
        List<RTStat> list = new ArrayList<>();
        for (String rt : wasteData.keySet()) {
            int total = getTotalReportsForRT(rt);
            list.add(new RTStat(rt, total));
        }
        Collections.sort(list, (a, b) -> Integer.compare(b.total, a.total));
        return list;
    }

    public static class RTStat {
        public String rtName;
        public int total;
        public RTStat(String rtName, int total) { this.rtName = rtName; this.total = total; }
    }
}
