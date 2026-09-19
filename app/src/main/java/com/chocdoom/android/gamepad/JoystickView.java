package com.chocdoom.android.gamepad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import org.libsdl.app.SDLActivity;

/**
 * Analog joystick for Mouse Lock style controls.
 *
 * Up/Down  → forward / back
 * Left/Right → strafe left / strafe right
 *
 * Turning is done ONLY by finger swipe (relative mouse) in GamepadOverlay.
 * This stick never sends turn keys.
 */
public class JoystickView extends View {

    // Forward / Back — DPAD (works out of the box)
    private static final int KEY_FORWARD = 19; // KEYCODE_DPAD_UP
    private static final int KEY_BACK    = 20; // KEYCODE_DPAD_DOWN

    // Strafe — A / D (bound in default.cfg to key_strafeleft / key_straferight)
    private static final int KEY_STRAFE_LEFT  = 29; // KEYCODE_A
    private static final int KEY_STRAFE_RIGHT = 32; // KEYCODE_D

    private Paint bgPaint, borderPaint, knobPaint;
    private float cx, cy, radius;
    private float knobX, knobY;

    private boolean forwardHeld, backHeld, strafeLeftHeld, strafeRightHeld;

    // Multi-touch: which finger owns this joystick (-1 = free)
    private int activePointerId = -1;

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
        int action = e.getActionMasked();
        int actionIndex = e.getActionIndex();
        int pointerId = e.getPointerId(actionIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (activePointerId == -1) {
                    activePointerId = pointerId;
                    // Prevent parent from stealing this pointer
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    float x = e.getX(actionIndex);
                    float y = e.getY(actionIndex);
                    updateKnob(x, y);
                    updateKeys();
                    invalidate();
                    return true;
                }
                return false;
            }

            case MotionEvent.ACTION_MOVE: {
                if (activePointerId == -1) return false;

                int idx = e.findPointerIndex(activePointerId);
                if (idx < 0) {
                    releaseStick();
                    return true;
                }

                float x = e.getX(idx);
                float y = e.getY(idx);
                updateKnob(x, y);
                updateKeys();
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pointerId == activePointerId) {
                    releaseStick();
                    return true;
                }
                return false;
            }
        }
        return super.onTouchEvent(e);
    }

    private void releaseStick() {
        activePointerId = -1;
        knobX = cx;
        knobY = cy;
        clearAllKeys();
        invalidate();

        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
    }

    private void updateKnob(float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
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

        // Forward / Back
        if (dy < -deadzone) {
            if (!forwardHeld) { SDLActivity.onNativeKeyDown(KEY_FORWARD); forwardHeld = true; }
        } else {
            if (forwardHeld) { SDLActivity.onNativeKeyUp(KEY_FORWARD); forwardHeld = false; }
        }
        if (dy > deadzone) {
            if (!backHeld) { SDLActivity.onNativeKeyDown(KEY_BACK); backHeld = true; }
        } else {
            if (backHeld) { SDLActivity.onNativeKeyUp(KEY_BACK); backHeld = false; }
        }

        // Strafe Left / Right (NO turning!)
        if (dx < -deadzone) {
            if (!strafeLeftHeld) { SDLActivity.onNativeKeyDown(KEY_STRAFE_LEFT); strafeLeftHeld = true; }
        } else {
            if (strafeLeftHeld) { SDLActivity.onNativeKeyUp(KEY_STRAFE_LEFT); strafeLeftHeld = false; }
        }
        if (dx > deadzone) {
            if (!strafeRightHeld) { SDLActivity.onNativeKeyDown(KEY_STRAFE_RIGHT); strafeRightHeld = true; }
        } else {
            if (strafeRightHeld) { SDLActivity.onNativeKeyUp(KEY_STRAFE_RIGHT); strafeRightHeld = false; }
        }
    }

    private void clearAllKeys() {
        if (forwardHeld)     { SDLActivity.onNativeKeyUp(KEY_FORWARD);     forwardHeld = false; }
        if (backHeld)        { SDLActivity.onNativeKeyUp(KEY_BACK);        backHeld = false; }
        if (strafeLeftHeld)  { SDLActivity.onNativeKeyUp(KEY_STRAFE_LEFT); strafeLeftHeld = false; }
        if (strafeRightHeld) { SDLActivity.onNativeKeyUp(KEY_STRAFE_RIGHT); strafeRightHeld = false; }
    }
}