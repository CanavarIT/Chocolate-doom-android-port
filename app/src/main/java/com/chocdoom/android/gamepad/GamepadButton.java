package com.chocdoom.android.gamepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

public class GamepadButton extends View {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private boolean pressed = false;
    private boolean toggle = false;
    private boolean temporary = false;
    private boolean toggledOn = false;

    private String label = "";
    private int keycode = 0;

    // Cyclic mode
    private int[] cycleKeys = null;
    private int cycleIndex = 0;

    private int accentColor = 0xFF888888;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingKeyUp = null;

    public GamepadButton(Context context) { super(context); init(); }
    public GamepadButton(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public GamepadButton(Context context, AttributeSet attrs, int def) { super(context, attrs, def); init(); }

    public void setLabel(String l) { this.label = l; invalidate(); }
    public void setKeycode(int k) { this.keycode = k; }
    public void setAccent(int c) { this.accentColor = c; invalidate(); }
    public void setToggle(boolean t) { this.toggle = t; }
    public void setTemporary(boolean t) { this.temporary = t; }
    public void setCycleKeys(int[] keys) { this.cycleKeys = keys; this.cycleIndex = 0; }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.argb(200, 255, 255, 255));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(34f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - 6f;

        int alpha = (pressed || toggledOn) ? 180 : 60;
        int color = (pressed || toggledOn) ? accentColor : Color.argb(alpha, 0, 0, 0);

        bgPaint.setColor(color);
        canvas.drawCircle(cx, cy, radius, bgPaint);
        canvas.drawCircle(cx, cy, radius, borderPaint);

        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, cx, textY, textPaint);
    }

    private void cancelPendingKeyUp() {
        if (pendingKeyUp != null) {
            handler.removeCallbacks(pendingKeyUp);
            pendingKeyUp = null;
        }
    }

    private void sendTemporaryKey(int k) {
        cancelPendingKeyUp();
        SDLActivity.onNativeKeyDown(k);
        final int keyToRelease = k;
        pendingKeyUp = new Runnable() {
            @Override
            public void run() {
                SDLActivity.onNativeKeyUp(keyToRelease);
                pendingKeyUp = null;
            }
        };
        handler.postDelayed(pendingKeyUp, 100);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (cycleKeys != null && cycleKeys.length > 0) {
                    int k = cycleKeys[cycleIndex];
                    android.util.Log.d("ChocDoomGP", "Cycle key: " + k);
                    sendTemporaryKey(k);
                    cycleIndex = (cycleIndex + 1) % cycleKeys.length;
                    pressed = true;
                } else if (temporary) {
                    android.util.Log.d("ChocDoomGP", "Temp key: " + keycode);
                    sendTemporaryKey(keycode);
                    pressed = true;
                } else if (toggle) {
                    toggledOn = !toggledOn;
                    if (toggledOn) {
                        SDLActivity.onNativeKeyDown(keycode);
                    } else {
                        SDLActivity.onNativeKeyUp(keycode);
                    }
                } else {
                    pressed = true;
                    SDLActivity.onNativeKeyDown(keycode);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (cycleKeys != null) {
                    pressed = false;
                } else if (temporary) {
                    pressed = false;
                    // keyUp уже отправлен через postDelayed
                } else if (!toggle) {
                    pressed = false;
                    SDLActivity.onNativeKeyUp(keycode);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
