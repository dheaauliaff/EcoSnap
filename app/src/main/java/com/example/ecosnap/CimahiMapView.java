package com.example.ecosnap;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CimahiMapView extends View {

    public interface OnRegionClickListener {
        void onRegionClick(RegionInfo region);
    }

    public static class RegionInfo {
        public final String number;
        public final String name;
        public final int baseColor;
        public final Path normalizedPath;
        public final PointF labelPoint;
        public RegionStat stat;
        public float animProgress = 0f;

        RegionInfo(String number, String name, int baseColor, Path normalizedPath, PointF labelPoint) {
            this.number = number;
            this.name = name;
            this.baseColor = baseColor;
            this.normalizedPath = normalizedPath;
            this.labelPoint = labelPoint;
            this.stat = new RegionStat();
        }
    }

    public static class RegionStat {
        public int totalReports;
        public String dominantCategory = "-";
        public int dominantColor = 0xFF4CAF50;
        public int percentage;
        public final Map<String, Integer> categoryCounts = new HashMap<>();
    }

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
    private final List<RegionInfo> regions = new ArrayList<>();
    private final Map<RegionInfo, Region> hitRegions = new HashMap<>();
    private final Matrix drawMatrix = new Matrix();
    private final RectF mapBounds = new RectF();

    private OnRegionClickListener clickListener;
    private String selectedName = "RT 05";
    private float appearScale = 0.97f;
    private float appearAlpha = 0f;

    public CimahiMapView(Context context) {
        super(context);
        init();
    }

    public CimahiMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CimahiMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        roadPaint.setColor(0x11000000); // Very low contrast lines
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setStrokeWidth(dp(4f));
        roadPaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);
        
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1.5f)); // ~3px separator
        borderPaint.setColor(0xFFFFFFFF);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(0x1A000000);
        shadowPaint.setMaskFilter(new BlurMaskFilter(dp(6f), BlurMaskFilter.Blur.NORMAL));

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(6f));
        glowPaint.setColor(0xFFFFCA28); // Yellow glow
        glowPaint.setMaskFilter(new BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.OUTER));

        badgePaint.setColor(0xFFFFFFFF);
        badgePaint.setStyle(Paint.Style.FILL);

        numberPaint.setColor(0xFF111111);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setFakeBoldText(true);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);

        buildRtBlocks();
        startIntroAnimation();
    }

    private void startIntroAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            appearAlpha = value;
            appearScale = 0.97f + (0.03f * value);
            invalidate();
        });
        animator.start();
    }

    private void buildRtBlocks() {
        regions.clear();
        
        // Exact Coordinate Positions requested by user
        PointF[] centers = new PointF[15];
        centers[0] = new PointF(0.50f, 0.15f); // 01
        centers[1] = new PointF(0.72f, 0.22f); // 02
        centers[2] = new PointF(0.50f, 0.78f); // 03
        centers[3] = new PointF(0.72f, 0.40f); // 04
        centers[4] = new PointF(0.50f, 0.52f); // 05 (Center)
        centers[5] = new PointF(0.50f, 0.32f); // 06
        centers[6] = new PointF(0.28f, 0.22f); // 07
        centers[7] = new PointF(0.38f, 0.38f); // 08
        centers[8] = new PointF(0.62f, 0.40f); // 09
        centers[9] = new PointF(0.25f, 0.50f); // 10
        centers[10] = new PointF(0.85f, 0.65f); // 11
        centers[11] = new PointF(0.68f, 0.58f); // 12
        centers[12] = new PointF(0.88f, 0.45f); // 13
        centers[13] = new PointF(0.30f, 0.72f); // 14
        centers[14] = new PointF(0.75f, 0.78f); // 15

        // Dummy points to create an organic outer boundary (faceted)
        PointF[] dummies = {
            new PointF(0.50f, -0.15f), new PointF(0.50f, 1.15f),
            new PointF(-0.15f, 0.50f), new PointF(1.15f, 0.50f),
            new PointF(0.00f, 0.00f),  new PointF(1.00f, 0.00f),
            new PointF(0.00f, 1.00f),  new PointF(1.00f, 1.00f)
        };

        String[] numbers = { "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15" };
        
        // Exact Material Palette
        int[] baseColors = {
            0xFFEF5350, // 01 Red
            0xFF66BB6A, // 02 Green
            0xFF42A5F5, // 03 Blue
            0xFF42A5F5, // 04 Blue
            0xFFFFA726, // 05 Orange
            0xFFAB47BC, // 06 Purple
            0xFF42A5F5, // 07 Blue
            0xFFFFCA28, // 08 Yellow
            0xFF66BB6A, // 09 Green
            0xFFEF5350, // 10 Red
            0xFFFFCA28, // 11 Yellow
            0xFFEF5350, // 12 Red
            0xFFFFCA28, // 13 Yellow
            0xFF66BB6A, // 14 Green
            0xFFFFA726  // 15 Orange
        };

        // Mathematical Voronoi Generation for Zero Gaps
        for (int i = 0; i < 15; i++) {
            List<PointF> poly = new ArrayList<>();
            poly.add(new PointF(-2f, -2f));
            poly.add(new PointF(3f, -2f));
            poly.add(new PointF(3f, 3f));
            poly.add(new PointF(-2f, 3f));

            for (int j = 0; j < 15; j++) {
                if (i == j) continue;
                clipWith(poly, centers[i], centers[j]);
            }
            for (PointF dummy : dummies) {
                clipWith(poly, centers[i], dummy);
            }

            Path path = new Path();
            if (!poly.isEmpty()) {
                path.moveTo(poly.get(0).x, poly.get(0).y);
                for (int j = 0; j < poly.size(); j++) {
                    PointF p1 = poly.get(j);
                    PointF p2 = poly.get((j + 1) % poly.size());
                    addOrganicEdge(path, p1, p2);
                }
                path.close();
            }

            RegionInfo info = new RegionInfo(
                    numbers[i],
                    "RT " + numbers[i],
                    baseColors[i],
                    path,
                    centers[i]
            );
            
            if (info.name.equalsIgnoreCase(selectedName)) {
                info.animProgress = 1f;
            }
            
            regions.add(info);
        }
    }

    private void clipWith(List<PointF> poly, PointF c1, PointF c2) {
        float dx = c2.x - c1.x;
        float dy = c2.y - c1.y;
        float mx = (c1.x + c2.x) / 2f;
        float my = (c1.y + c2.y) / 2f;
        float a = dx;
        float b = dy;
        float c = -(dx * mx + dy * my);
        
        List<PointF> out = new ArrayList<>();
        if (poly.isEmpty()) return;
        
        PointF prev = poly.get(poly.size() - 1);
        float prevVal = a * prev.x + b * prev.y + c;
        
        for (PointF curr : poly) {
            float currVal = a * curr.x + b * curr.y + c;
            
            if (currVal <= 0) {
                if (prevVal > 0) out.add(intersect(prev, curr, prevVal, currVal));
                out.add(curr);
            } else {
                if (prevVal <= 0) out.add(intersect(prev, curr, prevVal, currVal));
            }
            prev = curr;
            prevVal = currVal;
        }

        // Filter duplicates
        List<PointF> filtered = new ArrayList<>();
        for (PointF pt : out) {
            if (filtered.isEmpty()) {
                filtered.add(pt);
            } else {
                PointF last = filtered.get(filtered.size() - 1);
                if (Math.hypot(pt.x - last.x, pt.y - last.y) > 1e-4f) {
                    filtered.add(pt);
                }
            }
        }
        if (filtered.size() > 1) {
            PointF first = filtered.get(0);
            PointF last = filtered.get(filtered.size() - 1);
            if (Math.hypot(first.x - last.x, first.y - last.y) < 1e-4f) {
                filtered.remove(filtered.size() - 1);
            }
        }
        
        poly.clear();
        poly.addAll(filtered);
    }

    private PointF intersect(PointF p1, PointF p2, float v1, float v2) {
        float diff = v2 - v1;
        if (Math.abs(diff) < 1e-6f) return new PointF(p1.x, p1.y);
        float t = -v1 / diff;
        return new PointF(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y));
    }

    private void addOrganicEdge(Path p, PointF p1, PointF p2) {
        if (Math.hypot(p2.x - p1.x, p2.y - p1.y) < 1e-4f) return;
        
        long x1 = Math.round(p1.x * 1000f);
        long y1 = Math.round(p1.y * 1000f);
        long x2 = Math.round(p2.x * 1000f);
        long y2 = Math.round(p2.y * 1000f);

        boolean forward = true;
        if (x1 > x2 || (x1 == x2 && y1 > y2)) {
            forward = false;
        }
        
        long pxA = forward ? x1 : x2;
        long pyA = forward ? y1 : y2;
        long pxB = forward ? x2 : x1;
        long pyB = forward ? y2 : y1;

        float fxA = pxA / 1000f;
        float fyA = pyA / 1000f;
        float fxB = pxB / 1000f;
        float fyB = pyB / 1000f;

        float dx = fxB - fxA;
        float dy = fyB - fyA;

        int hash = (int) (pxA * 73856093 ^ pyA * 19349663 ^ pxB * 83492791 ^ pyB * 2394871);
        
        float rand1 = (((hash * 137) % 1000) / 1000f - 0.5f) * 0.2f;
        float rand2 = (((hash * 277) % 1000) / 1000f - 0.5f) * 0.2f;
        
        float nx = -dy;
        float ny = dx;

        float cp1x = fxA + dx * 0.33f + nx * rand1;
        float cp1y = fyA + dy * 0.33f + ny * rand1;
        float cp2x = fxA + dx * 0.66f + nx * rand2;
        float cp2y = fyA + dy * 0.66f + ny * rand2;

        if (forward) {
            p.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y);
        } else {
            p.cubicTo(cp2x, cp2y, cp1x, cp1y, p2.x, p2.y);
        }
    }

    public void setOnRegionClickListener(OnRegionClickListener listener) {
        this.clickListener = listener;
    }

    public List<RegionInfo> getRegions() {
        return new ArrayList<>(regions);
    }

    public RegionInfo getSelectedRegion() {
        for (RegionInfo region : regions) {
            if (region.name.equalsIgnoreCase(selectedName)) return region;
        }
        return null;
    }

    public void setRegionStats(Map<String, RegionStat> stats) {
        for (RegionInfo region : regions) {
            RegionStat stat = findStat(stats, region.name);
            region.stat = stat != null ? stat : new RegionStat();
        }
        invalidate();
    }

    public void selectRegion(String name) {
        String targetName = normalizeRtName(name);
        boolean changed = false;
        for (RegionInfo r : regions) {
            boolean isTarget = r.name.equalsIgnoreCase(targetName);
            if (isTarget && r.animProgress < 1f) {
                animateRegion(r, r.animProgress, 1f);
                changed = true;
            } else if (!isTarget && r.animProgress > 0f) {
                animateRegion(r, r.animProgress, 0f);
                changed = true;
            }
        }
        this.selectedName = targetName;
        if (!changed) invalidate();
    }

    private void animateRegion(RegionInfo r, float from, float to) {
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(250L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            r.animProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private RegionStat findStat(Map<String, RegionStat> stats, String regionName) {
        if (stats == null) return null;
        String target = normalizeRtName(regionName);
        for (Map.Entry<String, RegionStat> entry : stats.entrySet()) {
            if (normalizeRtName(entry.getKey()).equals(target)) return entry.getValue();
        }
        return null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(appearScale, appearScale, getWidth() / 2f, getHeight() / 2f);
        canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(), Math.round(appearAlpha * 255));
        
        drawBackground(canvas);
        prepareMatrix();
        hitRegions.clear();
        
        RegionInfo selected = null;
        for (RegionInfo region : regions) {
            if (region.name.equalsIgnoreCase(selectedName)) {
                selected = region;
            } else {
                drawRtBlock(canvas, region);
            }
        }
        if (selected != null) {
            drawRtBlock(canvas, selected);
        }
        
        canvas.restore();
        canvas.restore();
    }

    private void drawBackground(Canvas canvas) {
        backgroundPaint.setColor(0xFFF5F5F5);
        canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()), dp(20f), dp(20f), backgroundPaint);

        roadPaint.setColor(0x11000000);
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setStrokeWidth(dp(5f));
        roadPaint.setStrokeCap(Paint.Cap.ROUND);

        for (int i = 1; i <= 4; i++) {
            Path road = new Path();
            float y = getHeight() * (i / 5f);
            road.moveTo(0, y + (float)(Math.sin(i)*dp(30f)));
            road.cubicTo(getWidth()*0.33f, y - dp(40f), getWidth()*0.66f, y + dp(40f), getWidth(), y - (float)(Math.cos(i)*dp(30f)));
            canvas.drawPath(road, roadPaint);
        }
        for (int i = 1; i <= 3; i++) {
            Path road = new Path();
            float x = getWidth() * (i / 4f);
            road.moveTo(x + (float)(Math.sin(i)*dp(30f)), 0);
            road.cubicTo(x - dp(40f), getHeight()*0.33f, x + dp(40f), getHeight()*0.66f, x - (float)(Math.cos(i)*dp(30f)), getHeight());
            canvas.drawPath(road, roadPaint);
        }
    }

    private void prepareMatrix() {
        float padding = dp(24f);
        RectF rawBounds = new RectF(padding, padding, getWidth() - padding, getHeight() - padding);
        drawMatrix.reset();
        drawMatrix.setRectToRect(new RectF(0f, 0f, 1f, 1f), rawBounds, Matrix.ScaleToFit.CENTER);
        
        drawMatrix.mapRect(mapBounds, new RectF(0f, 0f, 1f, 1f));
    }

    private void drawRtBlock(Canvas canvas, RegionInfo region) {
        Path path = new Path(region.normalizedPath);
        path.transform(drawMatrix);

        float p = region.animProgress;
        float centerXPt = region.labelPoint.x * mapBounds.width() + mapBounds.left;
        float centerYPt = region.labelPoint.y * mapBounds.height() + mapBounds.top;

        Matrix scale = new Matrix();
        float s = 1f + 0.05f * p;
        scale.setScale(s, s, centerXPt, centerYPt);
        path.transform(scale);

        Path shadowPath = new Path(path);
        shadowPath.offset(0, dp(2f + 4f * p));
        shadowPaint.setColor(Color.argb((int)(0x1A + 0x22 * p), 0, 0, 0));
        canvas.drawPath(shadowPath, shadowPaint);

        int blockColor = region.stat != null && region.stat.totalReports > 0
                ? region.stat.dominantColor
                : region.baseColor;
                
        int brightColor = p > 0 ? blendColors(blockColor, 0xFFFFFFFF, p * 0.25f) : blockColor;
        int lightColor = lighten(brightColor);

        float maxProgress = 0f;
        for (RegionInfo r : regions) maxProgress = Math.max(maxProgress, r.animProgress);
        float dimAmount = Math.max(0f, maxProgress - region.animProgress);
        
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        
        fillPaint.setAlpha((int) (255 * (1f - 0.15f * dimAmount)));
        fillPaint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom, lightColor, brightColor, Shader.TileMode.CLAMP));
        canvas.drawPath(path, fillPaint);
        fillPaint.setShader(null);
        fillPaint.setAlpha(255);

        if (p > 0) {
            glowPaint.setAlpha((int) (255 * p));
            canvas.drawPath(path, glowPaint);
        }

        borderPaint.setAlpha((int) (255 * (1f - 0.15f * dimAmount)));
        canvas.drawPath(path, borderPaint);
        borderPaint.setAlpha(255);

        Region hit = new Region();
        hit.setPath(path, new Region((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom));
        hitRegions.put(region, hit);

        drawLabel(canvas, region, centerXPt, centerYPt, p, dimAmount);
    }

    private void drawLabel(Canvas canvas, RegionInfo region, float cx, float cy, float p, float dimAmount) {
        float badgeRadius = dp(14f) + dp(1f) * p;
        
        badgePaint.setAlpha((int) (255 * (1f - 0.15f * dimAmount)));
        canvas.drawCircle(cx, cy - dp(9f), badgeRadius, badgePaint);
        badgePaint.setAlpha(255);

        numberPaint.setTextSize(sp(12f) + sp(0.5f) * p);
        numberPaint.setAlpha((int) (255 * (1f - 0.15f * dimAmount)));
        canvas.drawText(region.number, cx, cy - dp(4f) + dp(0.5f)*p, numberPaint);
        numberPaint.setAlpha(255);

        int textColor = shouldUseDarkText(region) ? 0xFF222222 : 0xFFFFFFFF;
        labelPaint.setColor(textColor);
        labelPaint.setTextSize(sp(11f) + sp(0.5f) * p);
        labelPaint.setAlpha((int) (255 * (1f - 0.15f * dimAmount)));
        canvas.drawText(region.name, cx, cy + dp(16f), labelPaint);
        labelPaint.setAlpha(255);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        int x = (int) event.getX();
        int y = (int) event.getY();

        RegionInfo selected = getSelectedRegion();
        if (selected != null && hitRegions.containsKey(selected) && hitRegions.get(selected).contains(x, y)) {
            selectRegion("");
            if (clickListener != null) clickListener.onRegionClick(selected); 
            return true;
        }

        for (RegionInfo region : regions) {
            if (region == selected) continue;
            Region hit = hitRegions.get(region);
            if (hit != null && hit.contains(x, y)) {
                selectRegion(region.name);
                if (clickListener != null) clickListener.onRegionClick(region);
                return true;
            }
        }
        
        selectRegion("");
        return true;
    }

    private int blendColors(int color1, int color2, float ratio) {
        float inverseRatio = 1f - ratio;
        float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private boolean shouldUseDarkText(RegionInfo region) {
        int color = region.stat != null && region.stat.totalReports > 0 ? region.stat.dominantColor : region.baseColor;
        return color == 0xFFFFCA28; 
    }

    private String normalizeRtName(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return text.trim().toUpperCase(Locale.US);
        try {
            return "RT " + String.format(Locale.US, "%02d", Integer.parseInt(digits));
        } catch (Exception e) {
            return text.trim().toUpperCase(Locale.US);
        }
    }

    private int lighten(int color) {
        int r = Math.min(255, (int) (Color.red(color) * 1.12f + 14));
        int g = Math.min(255, (int) (Color.green(color) * 1.12f + 14));
        int b = Math.min(255, (int) (Color.blue(color) * 1.12f + 14));
        return Color.rgb(r, g, b);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
