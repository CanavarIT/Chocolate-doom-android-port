package com.chocdoom.android.gamepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

public class FireButton extends View {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private boolean pressed = false;
    private String label = "FIRE";
    private int scancode = 224; // SDL_SCANCODE_LCTRL

    public FireButton(Context context) {
        super(context);
        init();
    }

    public FireButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FireButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    public void setScancode(int scancode) {
        this.scancode = scancode;
    }

    private void init() {
        bgPaint = new Paint();
        bgPaint.setColor(Color.argb(120, 0, 0, 0));
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.argb(200, 255, 255, 255));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float radius = Math.min(w, h) / 2f - 6f;

        if (pressed) {
            bgPaint.setColor(Color.argb(180, 200, 50, 50));
        } else {
            bgPaint.setColor(Color.argb(120, 0, 0, 0));
        }

        canvas.drawCircle(w / 2f, h / 2f, radius, bgPaint);
        canvas.drawCircle(w / 2f, h / 2f, radius, borderPaint);

        float textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, w / 2f, textY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                SDLActivity.onNativeKeyDown(scancode);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                SDLActivity.onNativeKeyUp(scancode);
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}