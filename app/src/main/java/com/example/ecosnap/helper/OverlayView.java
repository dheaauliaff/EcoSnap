package com.example.ecosnap.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();
    private final Paint roiPaint = new Paint();
    private final Paint panelPaint = new Paint();
    private final Paint glowPaint = new Paint();
    private final Paint bitmapPaint = new Paint();

    private final List<TFLiteHelper.Result> results = new ArrayList<>();

    private int sourceWidth = 0;
    private int sourceHeight = 0;
    private int analyzerFps = 0;
    private boolean scannerChrome = false;
    private Bitmap offscreenBuffer;
    private Canvas offscreenCanvas;

    private final float cornerRadius;
    private final float strokeWidth;
    private final float labelRadius;
    private final float hudRadius;
    private final float hudPadding;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        cornerRadius = dp(18);
        strokeWidth = dp(5);
        labelRadius = dp(18);
        hudRadius = dp(16);
        hudPadding = dp(12);

        boxPaint.setAntiAlias(true);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(strokeWidth);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(13));
        textPaint.setFakeBoldText(true);

        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);

        roiPaint.setAntiAlias(true);
        roiPaint.setColor(Color.argb(210, 255, 255, 255));
        roiPaint.setStyle(Paint.Style.STROKE);
        roiPaint.setStrokeWidth(dp(3));
        roiPaint.setStrokeCap(Paint.Cap.ROUND);

        panelPaint.setAntiAlias(true);
        panelPaint.setColor(Color.argb(150, 16, 20, 24));
        panelPaint.setStyle(Paint.Style.FILL);

        glowPaint.setAntiAlias(true);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(9));

        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
    }

    public void setResults(List<TFLiteHelper.Result> list) {
        updateBoxes(list);
    }

    public void updateBoxes(List<TFLiteHelper.Result> list) {
        results.clear();
        if (list != null) results.addAll(list);
        invalidate();
    }

    public void setFrameInfo(int width, int height, int fps) {
        sourceWidth = width;
        sourceHeight = height;
        analyzerFps = fps;
        scannerChrome = true;
    }

    public void setImageSource(int width, int height) {
        sourceWidth = width;
        sourceHeight = height;
        scannerChrome = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        ensureBuffer(width, height);
        offscreenBuffer.eraseColor(Color.TRANSPARENT);

        boolean scannerMode = scannerChrome && sourceWidth > 0 && sourceHeight > 0;
        if (scannerMode) {
            drawScannerGuide(offscreenCanvas, width, height);
        }
        DrawSummary summary = drawDetections(offscreenCanvas, width, height, scannerMode);
        if (scannerMode) {
            drawHud(offscreenCanvas, summary, width);
            drawStatusChips(offscreenCanvas, summary, width, height);
            drawSummaryChip(offscreenCanvas, summary, width);
        }

        canvas.drawBitmap(offscreenBuffer, 0, 0, bitmapPaint);

        if (summary.hasLocked) {
            postInvalidateDelayed(120);
        }
    }

    private DrawSummary drawDetections(Canvas canvas, int width, int height, boolean scannerMode) {
        DrawSummary summary = new DrawSummary();
        float hudBottom = scannerMode ? dp(82) : 0f;
        RectF hudRect = scannerMode ? getHudRect(width) : new RectF();
        Map<String, Integer> classCounts = new HashMap<>();
        List<RectF> occupiedLabels = new ArrayList<>();

        for (TFLiteHelper.Result r : results) {
            if (r.rect == null) continue;
            RectF displayRect = constrainRect(shrinkDisplayRect(mapToView(r.rect, width, height)), width, height);
            int classColor = getClassColor(r.classId, r.label);

            String dominantLabel = displayLabel(r);
            classCounts.put(dominantLabel, classCounts.getOrDefault(dominantLabel, 0) + 1);
            if (classCounts.get(dominantLabel) > summary.maxCount) {
                summary.maxCount = classCounts.get(dominantLabel);
                summary.dominantClass = dominantLabel;
            }

            summary.detected++;
            summary.maxConfidence = Math.max(summary.maxConfidence, r.confidence);
            if (r.isLocked || r.confidence > 85f) summary.hasLocked = true;
            if (r.stableFrames > 6) summary.hasStable = true;
            if (r.stableFrames > 12) summary.readyCapture = true;
            if (r.isLowConfidence || r.confidence < 70f) summary.hasLowConfidence = true;

            drawDetectionBox(canvas, displayRect, r, classColor);
            RectF labelRect = drawDetectionLabel(canvas, displayRect, r, classColor, width, height, hudBottom, hudRect, occupiedLabels);
            occupiedLabels.add(labelRect);
        }

        summary.objectSummary = buildObjectSummary(classCounts);
        return summary;
    }

    private void drawDetectionBox(Canvas canvas, RectF rect, TFLiteHelper.Result result, int color) {
        int alpha = result.confidence >= 90f ? 235 : 210;
        if (result.isLocked) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 130.0);
            glowPaint.setColor(applyAlpha(color, (int) (95 * pulse)));
            glowPaint.setPathEffect(null);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint);
        } else {
            glowPaint.setColor(applyAlpha(color, 42));
            glowPaint.setPathEffect(null);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint);
        }

        boxPaint.setColor(applyAlpha(color, alpha));
        boxPaint.setStrokeWidth(result.confidence >= 90f ? dp(6) : strokeWidth);
        if (result.isLowConfidence || result.confidence < 75f) {
            boxPaint.setPathEffect(new DashPathEffect(new float[]{dp(12), dp(8)}, 0));
        } else {
            boxPaint.setPathEffect(null);
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, boxPaint);
        boxPaint.setPathEffect(null);
    }

    private RectF drawDetectionLabel(Canvas canvas, RectF box, TFLiteHelper.Result result, int color,
                                     int width, int height, float hudBottom, RectF hudRect,
                                     List<RectF> occupiedLabels) {
        String label = buildLabel(result);
        textPaint.setTextSize(dp(13));
        textPaint.setFakeBoldText(true);
        float textWidth = textPaint.measureText(label);
        float labelHeight = dp(34);
        float labelWidth = Math.min(width - dp(16), textWidth + dp(24));
        RectF labelRect = chooseLabelRect(box, labelWidth, labelHeight, width, height, hudBottom, hudRect, occupiedLabels);
        bgPaint.setColor(applyAlpha(color, 220));
        canvas.drawRoundRect(labelRect, labelRadius, labelRadius, bgPaint);

        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, labelRect.left + dp(12), labelRect.top + dp(22), textPaint);
        return labelRect;
    }

    private void drawHud(Canvas canvas, DrawSummary summary, int width) {
        RectF hud = getHudRect(width);

        panelPaint.setColor(Color.argb(145, 15, 18, 24));
        canvas.drawRoundRect(hud, hudRadius, hudRadius, panelPaint);

        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dp(13));
        textPaint.setColor(Color.WHITE);
        canvas.drawText("YOLOv8 \u2022 " + Math.min(analyzerFps, 60) + " FPS",
                hud.left + hudPadding, hud.top + dp(25), textPaint);

        textPaint.setFakeBoldText(false);
        textPaint.setTextSize(dp(12));
        textPaint.setColor(Color.argb(225, 255, 255, 255));
        canvas.drawText("Objects: " + summary.detected, hud.left + hudPadding, hud.top + dp(48), textPaint);
        canvas.drawText(summary.dominantClass, hud.left + dp(112), hud.top + dp(48), textPaint);
    }

    private void drawStatusChips(Canvas canvas, DrawSummary summary, int width, int height) {
        float x = dp(16);
        float y = height - dp(52);
        x = drawChip(canvas, x, y, "Tracking ON", Color.rgb(46, 125, 50));
        if (summary.hasLocked) {
            x = drawChip(canvas, x, y, "LOCKED", Color.rgb(76, 175, 80));
        } else if (summary.hasStable) {
            x = drawChip(canvas, x, y, "Stable", Color.rgb(33, 150, 243));
        }
        if (summary.hasLowConfidence) {
            x = drawChip(canvas, x, y, "Low Confidence", Color.rgb(255, 152, 0));
        }
        if (summary.readyCapture) {
            drawChip(canvas, x, y, "Ready Capture", Color.rgb(0, 150, 136));
        }
    }

    private void drawSummaryChip(Canvas canvas, DrawSummary summary, int width) {
        if (summary.detected <= 1 || summary.objectSummary.isEmpty()) {
            return;
        }
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dp(11));
        float chipWidth = Math.min(width - dp(32), textPaint.measureText(summary.objectSummary) + dp(24));
        RectF chip = new RectF(dp(16), dp(98), dp(16) + chipWidth, dp(126));
        bgPaint.setColor(Color.argb(150, 15, 18, 24));
        canvas.drawRoundRect(chip, dp(14), dp(14), bgPaint);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(summary.objectSummary, chip.left + dp(12), chip.top + dp(19), textPaint);
    }

    private float drawChip(Canvas canvas, float x, float y, String text, int color) {
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(dp(11));
        float width = textPaint.measureText(text) + dp(22);
        RectF chip = new RectF(x, y, x + width, y + dp(28));
        bgPaint.setColor(applyAlpha(color, 215));
        canvas.drawRoundRect(chip, dp(14), dp(14), bgPaint);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(text, chip.left + dp(11), chip.top + dp(19), textPaint);
        return chip.right + dp(8);
    }

    private void drawScannerGuide(Canvas canvas, int width, int height) {
        float left = width * 0.17f;
        float top = height * 0.19f;
        float right = width * 0.83f;
        float bottom = height * 0.81f;
        float len = dp(38);

        canvas.drawLine(left, top, left + len, top, roiPaint);
        canvas.drawLine(left, top, left, top + len, roiPaint);
        canvas.drawLine(right, top, right - len, top, roiPaint);
        canvas.drawLine(right, top, right, top + len, roiPaint);
        canvas.drawLine(left, bottom, left + len, bottom, roiPaint);
        canvas.drawLine(left, bottom, left, bottom - len, roiPaint);
        canvas.drawLine(right, bottom, right - len, bottom, roiPaint);
        canvas.drawLine(right, bottom, right, bottom - len, roiPaint);
    }

    private void ensureBuffer(int width, int height) {
        if (offscreenBuffer == null || offscreenBuffer.getWidth() != width || offscreenBuffer.getHeight() != height) {
            if (offscreenBuffer != null) {
                offscreenBuffer.recycle();
            }
            offscreenBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            offscreenCanvas = new Canvas(offscreenBuffer);
        }
    }

    private RectF getHudRect(int width) {
        float left = dp(16);
        float top = dp(16);
        float right = Math.min(width - dp(16), left + dp(220));
        float bottom = top + dp(74);
        return new RectF(left, top, right, bottom);
    }

    private RectF constrainRect(RectF rect, int width, int height) {
        return new RectF(
                Math.max(0, Math.min(width, rect.left)),
                Math.max(0, Math.min(height, rect.top)),
                Math.max(0, Math.min(width, rect.right)),
                Math.max(0, Math.min(height, rect.bottom))
        );
    }

    private RectF shrinkDisplayRect(RectF rect) {
        float shrink = 0.91f;
        float centerX = rect.centerX();
        float centerY = rect.centerY();
        float halfWidth = rect.width() * shrink / 2f;
        float halfHeight = rect.height() * shrink / 2f;
        return new RectF(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
    }

    private RectF chooseLabelRect(RectF box, float labelWidth, float labelHeight, int width, int height,
                                  float hudBottom, RectF hudRect, List<RectF> occupiedLabels) {
        float margin = dp(8);
        RectF[] candidates = new RectF[]{
                new RectF(box.left, box.top - labelHeight - margin, box.left + labelWidth, box.top - margin),
                new RectF(box.left, box.bottom + margin, box.left + labelWidth, box.bottom + labelHeight + margin),
                new RectF(box.right - labelWidth, box.top - labelHeight - margin, box.right, box.top - margin),
                new RectF(box.right - labelWidth, box.bottom + margin, box.right, box.bottom + labelHeight + margin)
        };

        for (RectF candidate : candidates) {
            RectF clamped = clampLabel(candidate, width, height);
            boolean hitsHud = hudBottom > 0f && RectF.intersects(clamped, hudRect);
            if (!hitsHud && !collidesWithLabels(clamped, occupiedLabels)
                    && clamped.top >= margin && clamped.bottom <= height - margin) {
                return clamped;
            }
        }

        RectF stacked = clampLabel(candidates[1], width, height);
        for (int attempt = 0; attempt < 8 && collidesWithLabels(stacked, occupiedLabels); attempt++) {
            stacked.offset(0, labelHeight + margin);
            stacked = clampLabel(stacked, width, height);
            if (stacked.bottom >= height - margin) {
                stacked.offset(0, -(labelHeight + margin) * (attempt + 1));
                stacked = clampLabel(stacked, width, height);
            }
        }
        return stacked;
    }

    private boolean collidesWithLabels(RectF rect, List<RectF> labels) {
        RectF padded = new RectF(rect.left - dp(4), rect.top - dp(4), rect.right + dp(4), rect.bottom + dp(4));
        for (RectF existing : labels) {
            if (RectF.intersects(padded, existing)) {
                return true;
            }
        }
        return false;
    }

    private RectF clampLabel(RectF rect, int width, int height) {
        float margin = dp(8);
        float labelWidth = rect.width();
        float labelHeight = rect.height();
        float left = Math.max(margin, Math.min(width - labelWidth - margin, rect.left));
        float top = Math.max(margin, Math.min(height - labelHeight - margin, rect.top));
        return new RectF(left, top, left + labelWidth, top + labelHeight);
    }

    private String buildObjectSummary(Map<String, Integer> classCounts) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(entry.getKey()).append(" x").append(entry.getValue());
        }
        return builder.toString();
    }

    private String buildLabel(TFLiteHelper.Result result) {
        if (result.classId == 6 || "Bukan Sampah".equalsIgnoreCase(result.label)) {
            return "Ignored Object (" + String.format("%.0f", result.confidence) + "%)";
        }
        return displayLabel(result) + " #" + result.trackingId + " (" + String.format("%.0f", result.confidence) + "%)";
    }

    private String displayLabel(TFLiteHelper.Result result) {
        if (result.label == null || result.label.trim().isEmpty()) {
            return "Object";
        }
        if ("Bukan Sampah".equalsIgnoreCase(result.label)) {
            return "Ignored";
        }
        return result.label;
    }

    private int getClassColor(int classId, String label) {
        switch (classId) {
            case 0: return Color.GREEN;
            case 1: return 0xFFFF9800;
            case 2: return 0xFF00BCD4;
            case 3: return Color.GRAY;
            case 4: return Color.BLUE;
            case 5: return Color.YELLOW;
            case 6: return Color.RED;
            default:
                if (label == null) return Color.YELLOW;
                switch (label.toLowerCase()) {
                    case "organik": return Color.GREEN;
                    case "kardus": return 0xFFFF9800;
                    case "kaca": return 0xFF00BCD4;
                    case "logam": return Color.GRAY;
                    case "kertas": return Color.BLUE;
                    case "plastik": return Color.YELLOW;
                    case "bukan sampah": return Color.RED;
                    default: return Color.YELLOW;
                }
        }
    }

    private int applyAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private RectF mapToView(RectF sourceRect, int viewWidth, int viewHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return new RectF(sourceRect);
        }

        float scale = Math.max((float) viewWidth / sourceWidth, (float) viewHeight / sourceHeight);
        float dx = (viewWidth - sourceWidth * scale) / 2f;
        float dy = (viewHeight - sourceHeight * scale) / 2f;

        return new RectF(
                sourceRect.left * scale + dx,
                sourceRect.top * scale + dy,
                sourceRect.right * scale + dx,
                sourceRect.bottom * scale + dy
        );
    }

    private static class DrawSummary {
        int detected = 0;
        int maxCount = 0;
        boolean hasLocked = false;
        boolean hasStable = false;
        boolean hasLowConfidence = false;
        boolean readyCapture = false;
        float maxConfidence = 0f;
        String dominantClass = "None";
        String objectSummary = "";
    }
}
