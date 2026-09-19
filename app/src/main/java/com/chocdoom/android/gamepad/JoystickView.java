package com.chocdoom.android.gamepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

public class JoystickView extends View {

    // Android keycodes для стрелок
    private static final int KEY_UP    = 19;
    private static final int KEY_DOWN  = 20;
    private static final int KEY_LEFT  = 21;
    private static final int KEY_RIGHT = 22;

    private Paint bgPaint, borderPaint, knobPaint;
    private float cx, cy, radius;
    private float knobX, knobY;

    private boolean upHeld, downHeld, leftHeld, rightHeld;

    public JoystickView(Context c) { super(c); init(); }
    public JoystickView(Context c, AttributeSet a) { super(c, a); init(); }
    public JoystickView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.argb(80, 0, 0, 0));
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.argb(160, 255, 255, 255));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        knobPaint.setColor(Color.argb(180, 255, 255, 255));
        knobPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        cx = w / 2f;
        cy = h / 2f;
        radius = Math.min(w, h) / 2f - 8f;
        knobX = cx;
        knobY = cy;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(cx, cy, radius, bgPaint);
        canvas.drawCircle(cx, cy, radius, borderPaint);
        canvas.drawCircle(knobX, knobY, radius * 0.35f, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateKnob(x, y);
                updateKeys();
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                knobX = cx;
                knobY = cy;
                clearAllKeys();
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void updateKnob(float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        float maxDist = radius * 0.6f;
        if (dist > maxDist) {
            dx = dx / dist * maxDist;
            dy = dy / dist * maxDist;
        }
        knobX = cx + dx;
        knobY = cy + dy;
    }

    private void updateKeys() {
        float deadzone = radius * 0.25f;
        float dx = knobX - cx;
        float dy = knobY - cy;

        if (dy < -deadzone) {
            if (!upHeld) { SDLActivity.onNativeKeyDown(KEY_UP); upHeld = true; }
        } else {
            if (upHeld) { SDLActivity.onNativeKeyUp(KEY_UP); upHeld = false; }
        }
        if (dy > deadzone) {
            if (!downHeld) { SDLActivity.onNativeKeyDown(KEY_DOWN); downHeld = true; }
        } else {
            if (downHeld) { SDLActivity.onNativeKeyUp(KEY_DOWN); downHeld = false; }
        }
        if (dx < -deadzone) {
            if (!leftHeld) { SDLActivity.onNativeKeyDown(KEY_LEFT); leftHeld = true; }
        } else {
            if (leftHeld) { SDLActivity.onNativeKeyUp(KEY_LEFT); leftHeld = false; }
        }
        if (dx > deadzone) {
            if (!rightHeld) { SDLActivity.onNativeKeyDown(KEY_RIGHT); rightHeld = true; }
        } else {
            if (rightHeld) { SDLActivity.onNativeKeyUp(KEY_RIGHT); rightHeld = false; }
        }
    }

    private void clearAllKeys() {
        if (upHeld)    { SDLActivity.onNativeKeyUp(KEY_UP);    upHeld = false; }
        if (downHeld)  { SDLActivity.onNativeKeyUp(KEY_DOWN);  downHeld = false; }
        if (leftHeld)  { SDLActivity.onNativeKeyUp(KEY_LEFT);  leftHeld = false; }
        if (rightHeld) { SDLActivity.onNativeKeyUp(KEY_RIGHT); rightHeld = false; }
    }
}
