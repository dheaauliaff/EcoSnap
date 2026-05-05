package com.example.ecosnap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class CategoryDonutView extends View {

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint separatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF chartBounds = new RectF();
    private final LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
    private int total = 0;

    public CategoryDonutView(Context context) {
        super(context);
        init();
    }

    public CategoryDonutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryDonutView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        segmentPaint.setStyle(Paint.Style.STROKE);
        segmentPaint.setStrokeCap(Paint.Cap.BUTT);
        separatorPaint.setStyle(Paint.Style.STROKE);
        separatorPaint.setStrokeCap(Paint.Cap.BUTT);
        separatorPaint.setColor(0xFFFFFFFF);
        separatorPaint.setStrokeWidth(dp(1f));
        centerPaint.setColor(0xFFFFFFFF);
        centerPaint.setShadowLayer(dp(3f), 0, dp(1f), 0x18000000);
        valuePaint.setColor(0xFF212121);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
        labelPaint.setColor(0xFF757575);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setValues(Map<String, Integer> categoryValues) {
        values.clear();
        total = 0;
        String[] order = {"Organik", "Kardus", "Kaca", "Logam", "Kertas", "Plastik"};
        if (categoryValues != null) {
            for (String key : order) {
                int value = categoryValues.containsKey(key) ? categoryValues.get(key) : 0;
                values.put(key, value);
                total += value;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.20f;
        segmentPaint.setStrokeWidth(stroke);
        float pad = stroke / 2f + dp(4f);
        chartBounds.set(pad, pad, getWidth() - pad, getHeight() - pad);

        if (total == 0) {
            segmentPaint.setColor(0xFFE8F5E9);
            canvas.drawArc(chartBounds, -90f, 360f, false, segmentPaint);
        } else {
            float start = -90f;
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                if (entry.getValue() <= 0) continue;
                float sweep = (entry.getValue() * 360f) / total;
                segmentPaint.setColor(colorFor(entry.getKey()));
                canvas.drawArc(chartBounds, start, sweep, false, segmentPaint);
                start += sweep;
            }

            start = -90f;
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                if (entry.getValue() <= 0) continue;
                float sweep = (entry.getValue() * 360f) / total;
                canvas.drawArc(chartBounds, start, 0.7f, false, separatorPaint);
                start += sweep;
            }
        }

        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size * 0.24f, centerPaint);
        valuePaint.setTextSize(sp(28f));
        labelPaint.setTextSize(sp(12f));
        canvas.drawText(String.valueOf(total), getWidth() / 2f, getHeight() / 2f - dp(2f), valuePaint);
        canvas.drawText("Total", getWidth() / 2f, getHeight() / 2f + dp(18f), labelPaint);
    }

    public static int colorFor(String category) {
        if (category == null) return 0xFF4CAF50;
        String normalized = category.toLowerCase();
        if (normalized.contains("organik")) return 0xFF4CAF50;
        if (normalized.contains("kardus")) return 0xFF2196F3;
        if (normalized.contains("kaca")) return 0xFF00BCD4;
        if (normalized.contains("logam")) return 0xFF9C27B0;
        if (normalized.contains("kertas")) return 0xFFFFC107;
        if (normalized.contains("plastik")) return 0xFFFF9800;
        return 0xFF4CAF50;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
