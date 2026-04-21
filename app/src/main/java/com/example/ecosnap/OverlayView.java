package com.example.ecosnap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final List<DetectionBox> boxes = new ArrayList<>();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint.setColor(Color.parseColor("#FFEB3B"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void setResults(List<DetectionBox> results) {
        boxes.clear();
        if (results != null) boxes.addAll(results);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (DetectionBox box : boxes) {
            canvas.drawRect(box.rect, boxPaint);
            canvas.drawText(box.label, box.rect.left, Math.max(40, box.rect.top - 10), textPaint);
        }
    }

    public static class DetectionBox {
        public RectF rect;
        public String label;

        public DetectionBox(RectF rect, String label) {
            this.rect = rect;
            this.label = label;
        }
    }
}