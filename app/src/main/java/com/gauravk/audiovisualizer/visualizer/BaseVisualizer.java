package com.gauravk.audiovisualizer.base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public abstract class BaseVisualizer extends View {

    protected byte[] bytes;
    protected Paint paint;

    public BaseVisualizer(Context context) {
        super(context);
        init();
    }

    public BaseVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected abstract void init();

    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null || paint == null) return;
        // Görselleştirme çizimi burada yapılır.
    }
}
