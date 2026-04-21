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

    private final List<TFLiteHelper.Result> results = new ArrayList<>();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        boxPaint.setColor(Color.YELLOW);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
    }

    public void setResults(List<TFLiteHelper.Result> list) {
        results.clear();
        if (list != null) results.addAll(list);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (TFLiteHelper.Result r : results) {

            if (r.rect == null) continue;

            canvas.drawRect(r.rect, boxPaint);

            String text = r.label + " (" + String.format("%.1f", r.confidence) + "%)";
            canvas.drawText(text, r.rect.left, Math.max(40, r.rect.top - 10), textPaint);
        }
    }
}