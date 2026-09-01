package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public final class SearchIconView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int green = Color.rgb(49, 88, 71);

    public SearchIconView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w * 0.48f;
        float cy = h * 0.48f;
        float buttonR = Math.min(w, h) * 0.39f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(235, 243, 238));
        canvas.drawCircle(cx, cy, buttonR, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(3f, Math.min(w, h) * 0.065f));
        paint.setColor(green);
        float glassR = buttonR * 0.43f;
        float gx = cx - buttonR * 0.10f;
        float gy = cy - buttonR * 0.10f;
        canvas.drawCircle(gx, gy, glassR, paint);
        float k = glassR * 0.72f;
        canvas.drawLine(gx + k, gy + k, gx + glassR * 1.58f, gy + glassR * 1.58f, paint);
    }
}
