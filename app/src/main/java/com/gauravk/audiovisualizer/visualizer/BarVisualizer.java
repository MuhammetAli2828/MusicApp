package com.gauravk.audiovisualizer.visualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.gauravk.audiovisualizer.base.BaseVisualizer;

public class BarVisualizer extends BaseVisualizer {

    private float density = 50; // Görselleştirme yoğunluğu

    public BarVisualizer(Context context) {
        super(context);
    }

    public BarVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(5f);
        paint.setColor(0xFF00FF00); // Varsayılan renk (Yeşil)
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bytes == null) return;

        float barWidth = getWidth() / (float) bytes.length;
        float barHeight;
        for (int i = 0; i < bytes.length; i++) {
            barHeight = (bytes[i] + 128) * density / 256;
            canvas.drawRect(
                    i * barWidth,
                    getHeight() - barHeight,
                    (i + 1) * barWidth,
                    getHeight(),
                    paint
            );
        }
    }
}
