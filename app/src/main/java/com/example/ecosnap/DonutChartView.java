package com.example.ecosnap;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class DonutChartView extends View {
    private Paint paint;
    private Paint bgPaint;
    private RectF rectF;
    private float[] values = {25f, 23f, 20f, 15f, 10f, 5f};
    private int[] colors = {
        0xFF4CAF50, // Organik (Green)
        0xFFFF9800, // Plastik (Orange)
        0xFFFFC107, // Kertas (Yellow)
        0xFF00BCD4, // Kaca (Cyan)
        0xFF2196F3, // Kardus (Blue)
        0xFF9C27B0  // Logam (Purple)
    };
    
    public void setValues(float[] newValues) {
        if (newValues != null && newValues.length == 6) {
            this.values = newValues;
            this.sweepProgress = 0f;
            initAnim();
        }
    }
    
    private void initAnim() {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            sweepProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }
    
    private float sweepProgress = 0f;
    private float strokeWidth = 35f; 

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT); 
        paint.setStrokeWidth(strokeWidth);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setColor(Color.parseColor("#F5F5F5"));
        bgPaint.setStrokeWidth(strokeWidth);

        rectF = new RectF();
        
        // Smooth animation on load (ease-out)
        initAnim();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = strokeWidth / 2f + 2f; 
        rectF.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw background track
        canvas.drawArc(rectF, 0, 360, false, bgPaint);

        float total = 0f;
        for (float v : values) total += v;

        if (total <= 0f) {
            return;
        }
        
        float currentAngle = -90f; // Start at top
        
        for (int i = 0; i < values.length; i++) {
            float sweepAngle = (values[i] / total) * 360f;
            paint.setColor(colors[i]);
            
            // 4px subtle gap
            float gap = 4f; 
            float drawnSweep = (sweepAngle * sweepProgress) - gap;
            if (drawnSweep < 0) drawnSweep = 0;
            
            canvas.drawArc(rectF, currentAngle, drawnSweep, false, paint);
            currentAngle += sweepAngle * sweepProgress;
        }
    }
}
