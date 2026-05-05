package com.example.ecosnap;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsFragment extends Fragment {

    private final String[] categoryOrder = SharedPrototypeData.CATEGORIES;
    private CimahiMapView mapView;
    private LinearLayout legendRow;
    private LinearLayout topRegionRow;
    private View mapsRoot;
    private Map<String, CimahiMapView.RegionStat> latestRegionStats = new HashMap<>();
    private LinkedHashMap<String, Integer> latestCategoryTotals = new LinkedHashMap<>();
    private int totalReports = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_maps, container, false);
        mapsRoot = view.findViewById(R.id.mapsRoot);
        mapView = view.findViewById(R.id.cimahiMapView);
        legendRow = view.findViewById(R.id.legendRow);
        topRegionRow = view.findViewById(R.id.topRegionRow);

        setupStaticUi(view);
        setupInteractions(view);
        animatePage(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupStaticUi(View root) {
        bindMetric(root.findViewById(R.id.metricKelurahan), R.drawable.ic_user_outline, "0", "RT");
        bindMetric(root.findViewById(R.id.metricLaporan), R.drawable.ic_document_outline, "0", "Total Laporan");
        bindMetric(root.findViewById(R.id.metricDominan), R.drawable.ic_leaf, "-", "Dominan");
        bindMetric(root.findViewById(R.id.metricPersentase), R.drawable.ic_pie_chart, "0%", "Persentase");
        buildLegend();
        buildTopRegions(new ArrayList<>());
    }

    private void setupInteractions(View root) {
        if (mapView != null) {
            mapView.setOnRegionClickListener(this::showRegionDetail);
        }

        View locate = root.findViewById(R.id.btnLocate);
        TextView zoomIn = root.findViewById(R.id.btnZoomIn);
        TextView zoomOut = root.findViewById(R.id.btnZoomOut);

        if (locate != null) locate.setOnClickListener(v -> {
            CimahiMapView.RegionInfo selected = mapView != null ? mapView.getSelectedRegion() : null;
            if (selected != null) showRegionDetail(selected);
        });
        if (zoomIn != null) zoomIn.setOnClickListener(v -> animateMapScale(1.04f));
        if (zoomOut != null) zoomOut.setOnClickListener(v -> animateMapScale(1f));
    }

    private void animatePage(View root) {
        root.setAlpha(0f);
        root.setTranslationY(dp(12f));
        root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateMapScale(float scale) {
        if (mapView == null) return;
        mapView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(200L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void loadData() {
        SharedPrototypeData data = SharedPrototypeData.getInstance();
        latestCategoryTotals = data.getGlobalCategoryTotals();
        totalReports = data.getGlobalTotalReports();
        latestRegionStats = buildRegionStatsFromPrototype(data);

        if (mapView != null) {
            mapView.setRegionStats(latestRegionStats);
        }

        String dominant = data.getGlobalDominantCategory();
        int dominantCount = latestCategoryTotals.getOrDefault(dominant, 0);
        int percentage = totalReports == 0 ? 0 : Math.round((dominantCount * 100f) / totalReports);

        View root = getView();
        if (root != null) {
            int totalRt = mapView != null ? mapView.getRegions().size() : 0;
            bindMetric(root.findViewById(R.id.metricKelurahan), R.drawable.ic_user_outline, String.valueOf(totalRt), "RT");
            bindMetric(root.findViewById(R.id.metricLaporan), R.drawable.ic_document_outline, String.valueOf(totalReports), "Total Laporan");
            bindMetric(root.findViewById(R.id.metricDominan), R.drawable.ic_leaf, dominant, "Dominan");
            bindMetric(root.findViewById(R.id.metricPersentase), R.drawable.ic_pie_chart, percentage + "%", "Persentase");
        }

        buildTopRegions(sortedRegions());
    }

    private Map<String, CimahiMapView.RegionStat> buildRegionStatsFromPrototype(SharedPrototypeData data) {
        Map<String, CimahiMapView.RegionStat> result = new HashMap<>();
        List<CimahiMapView.RegionInfo> regions = mapView != null ? mapView.getRegions() : new ArrayList<>();
        
        Map<String, Map<String, Integer>> allWaste = data.getWasteData();

        for (CimahiMapView.RegionInfo region : regions) {
            CimahiMapView.RegionStat stat = new CimahiMapView.RegionStat();
            
            // Region name is usually like "RT 01". Let's match it to mock data.
            Map<String, Integer> counts = allWaste.getOrDefault(region.name, new HashMap<>());
            
            stat.categoryCounts.putAll(counts);
            for (int v : counts.values()) {
                stat.totalReports += v;
            }
            
            stat.dominantCategory = data.getDominantCategoryForRT(region.name);
            int dominantCount = counts.getOrDefault(stat.dominantCategory, 0);
            stat.percentage = stat.totalReports == 0 ? 0 : Math.round((dominantCount * 100f) / stat.totalReports);
            stat.dominantColor = CategoryDonutView.colorFor(stat.dominantCategory);
            
            result.put(region.name, stat);
        }
        return result;
    }

    private List<CimahiMapView.RegionInfo> sortedRegions() {
        List<CimahiMapView.RegionInfo> regions = mapView != null ? mapView.getRegions() : new ArrayList<>();
        Collections.sort(regions, (a, b) -> Integer.compare(b.stat.totalReports, a.stat.totalReports));
        return regions;
    }

    private void bindMetric(View metric, int iconRes, String value, String label) {
        if (metric == null) return;
        ImageView icon = metric.findViewById(R.id.ivMetricIcon);
        TextView valueView = metric.findViewById(R.id.tvMetricValue);
        TextView labelView = metric.findViewById(R.id.tvMetricLabel);
        if (icon != null) icon.setImageResource(iconRes);
        if (valueView != null) valueView.setText(value);
        if (labelView != null) labelView.setText(label);
    }

    private void buildLegend() {
        if (legendRow == null || getContext() == null) return;
        legendRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String category : categoryOrder) {
            View item = inflater.inflate(R.layout.item_legend_category, legendRow, false);
            FrameLayout circle = item.findViewById(R.id.legendCircle);
            ImageView icon = item.findViewById(R.id.ivLegendIcon);
            TextView label = item.findViewById(R.id.tvLegendLabel);
            int color = CategoryDonutView.colorFor(category);
            if (circle != null) circle.setBackgroundTintList(ColorStateList.valueOf(softColor(color)));
            if (icon != null) {
                icon.setImageResource(iconFor(category));
                icon.setImageTintList(ColorStateList.valueOf(color));
            }
            if (label != null) label.setText(category);
            legendRow.addView(item);
        }
    }

    private void buildTopRegions(List<CimahiMapView.RegionInfo> regions) {
        if (topRegionRow == null || getContext() == null) return;
        topRegionRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < 3; i++) {
            View item = inflater.inflate(R.layout.item_top_kelurahan, topRegionRow, false);
            TextView rank = item.findViewById(R.id.tvRank);
            TextView name = item.findViewById(R.id.tvTopName);
            TextView total = item.findViewById(R.id.tvTopTotal);

            if (rank != null) {
                rank.setText(String.valueOf(i + 1));
                rank.setBackgroundResource(i == 0 ? R.drawable.bg_rank_badge_green : i == 1 ? R.drawable.bg_rank_badge_blue : R.drawable.bg_rank_badge_orange);
            }
            if (i < regions.size()) {
                CimahiMapView.RegionInfo region = regions.get(i);
                if (name != null) name.setText(region.name);
                if (total != null) total.setText(String.valueOf(region.stat.totalReports));
                item.setOnClickListener(v -> showRegionDetail(region));
            } else {
                if (name != null) name.setText("-");
                if (total != null) total.setText("0");
            }
            topRegionRow.addView(item);
        }
    }

    private void showRegionDetail(CimahiMapView.RegionInfo region) {
        if (!isAdded() || getContext() == null || region == null) return;
        
        SharedPrototypeData.getInstance().setSelectedRT(region.name);
        
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_kelurahan_detail, null, false);
        dialog.setContentView(sheet);

        ImageView back = sheet.findViewById(R.id.btnDetailBack);
        CimahiMapView detailMap = sheet.findViewById(R.id.detailMapView);

        if (back != null) back.setOnClickListener(v -> dialog.dismiss());
        if (detailMap != null) {
            detailMap.setRegionStats(latestRegionStats);
            detailMap.selectRegion(region.name);
            
            detailMap.setOnRegionClickListener(clickedRegion -> {
                SharedPrototypeData.getInstance().setSelectedRT(clickedRegion.name);
                updateDetailUi(sheet, clickedRegion);
            });
        }

        updateDetailUi(sheet, region);

        dialog.setOnShowListener(d -> {
            View parent = (View) sheet.getParent();
            if (parent != null) {
                parent.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                parent.requestLayout();
            }
        });
        dialog.show();
    }

    private void updateDetailUi(View sheet, CimahiMapView.RegionInfo region) {
        if (region == null) return;
        
        // RE-FETCH data specific to this RT to ensure it's not holding old state
        SharedPrototypeData data = SharedPrototypeData.getInstance();
        Map<String, Integer> counts = data.getWasteData().getOrDefault(region.name, new HashMap<>());
        int totalReports = data.getTotalReportsForRT(region.name);
        String dominantCategory = data.getDominantCategoryForRT(region.name);
        int dominantCount = counts.getOrDefault(dominantCategory, 0);
        int percentage = totalReports == 0 ? 0 : Math.round((dominantCount * 100f) / totalReports);
        
        TextView badge = sheet.findViewById(R.id.tvDetailBadge);
        TextView name = sheet.findViewById(R.id.tvDetailName);
        TextView dominant = sheet.findViewById(R.id.tvDetailDominan);
        CategoryDonutView donut = sheet.findViewById(R.id.detailDonut);
        LinearLayout breakdown = sheet.findViewById(R.id.categoryBreakdown);
        
        if (badge != null) badge.setText(region.number);
        if (name != null) name.setText(region.name);
        if (dominant != null) dominant.setText(dominantCategory);
        if (donut != null) donut.setValues(counts);

        bindDetailMetric(sheet.findViewById(R.id.detailTotal), "Total Laporan", totalReports > 0 ? String.valueOf(totalReports) : "-", totalReports > 0 ? "laporan" : "");
        bindDetailMetric(sheet.findViewById(R.id.detailPercent), "Persentase", totalReports > 0 ? percentage + "%" : "-", "");
        bindDetailMetric(sheet.findViewById(R.id.detailArea), "RW", "-", "");
        bindDetailMetric(sheet.findViewById(R.id.detailResidents), "Status Data", totalReports > 0 ? "Aktif" : "-", "");
        
        buildBreakdown(breakdown, counts, totalReports);
    }

    private void bindDetailMetric(View metric, String label, String value, String suffix) {
        if (metric == null) return;
        TextView labelView = metric.findViewById(R.id.tvDetailMetricLabel);
        TextView valueView = metric.findViewById(R.id.tvDetailMetricValue);
        TextView suffixView = metric.findViewById(R.id.tvDetailMetricSuffix);
        if (labelView != null) labelView.setText(label);
        if (valueView != null) valueView.setText(value);
        if (suffixView != null) {
            if (suffix == null || suffix.isEmpty()) {
                suffixView.setVisibility(View.GONE);
            } else {
                suffixView.setVisibility(View.VISIBLE);
                suffixView.setText(suffix);
            }
        }
    }

    private void buildBreakdown(LinearLayout parent, Map<String, Integer> counts, int localTotal) {
        if (parent == null || getContext() == null) return;
        parent.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (String category : categoryOrder) {
            int value = counts.containsKey(category) ? counts.get(category) : 0;
            int percentage = localTotal == 0 ? 0 : Math.round((value * 100f) / localTotal);
            int color = CategoryDonutView.colorFor(category);
            View row = inflater.inflate(R.layout.item_category_progress, parent, false);
            View dot = row.findViewById(R.id.categoryDot);
            TextView name = row.findViewById(R.id.tvCategoryName);
            TextView count = row.findViewById(R.id.tvCategoryValue);
            View fill = row.findViewById(R.id.progressFill);
            if (dot != null) dot.setBackgroundTintList(ColorStateList.valueOf(color));
            if (name != null) name.setText(category);
            if (count != null) count.setText(value + " (" + percentage + "%)");
            if (fill != null) {
                fill.setBackgroundTintList(ColorStateList.valueOf(color));
                fill.post(() -> {
                    View parentTrack = (View) fill.getParent();
                    ViewGroup.LayoutParams params = fill.getLayoutParams();
                    params.width = Math.max(dp(4f), Math.round(parentTrack.getWidth() * (percentage / 100f)));
                    fill.setLayoutParams(params);
                    ObjectAnimator animator = ObjectAnimator.ofFloat(fill, "alpha", 0.2f, 1f);
                    animator.setDuration(260L);
                    animator.start();
                });
            }
            parent.addView(row);
        }
    }

    private int iconFor(String category) {
        if ("Kardus".equals(category)) return R.drawable.ic_box_outline;
        if ("Kaca".equals(category)) return R.drawable.ic_bottle_outline;
        if ("Logam".equals(category)) return R.drawable.ic_can_outline;
        if ("Kertas".equals(category)) return R.drawable.ic_document_outline;
        if ("Plastik".equals(category)) return R.drawable.ic_plastic_bottle_outline;
        return R.drawable.ic_leaf;
    }

    private int softColor(int color) {
        int r = Math.min(255, (int) (Color.red(color) * 0.18f + 230));
        int g = Math.min(255, (int) (Color.green(color) * 0.18f + 230));
        int b = Math.min(255, (int) (Color.blue(color) * 0.18f + 230));
        return Color.rgb(r, g, b);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
