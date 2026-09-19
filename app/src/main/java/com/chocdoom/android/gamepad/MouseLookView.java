package com.chocdoom.android.gamepad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

/**
 * Invisible overlay view that captures swipe gestures and translates them
 * into incremental mouse movement for SDL2.
 *
 * This is the Android equivalent of "mouse lock" / mouse look:
 * absolute finger position does not matter — only the delta between
 * consecutive MotionEvents is used.
 */
public class MouseLookView extends View {

    // SDL mouse button/action constants (see SDL_mouse.h)
    private static final int SDL_BUTTON_LEFT = 1;
    private static final int SDL_PRESSED = 1;
    private static final int SDL_RELEASED = 0;

    // Relative movement scale. Tune this if turning feels too slow/fast.
    // Higher = faster turn for the same finger movement.
    private static final float SENSITIVITY = 1.0f;

    private float lastX;
    private float lastY;

    public MouseLookView(Context context) {
        super(context);
        init();
    }

    public MouseLookView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MouseLookView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Transparent, but still catches touch events.
        setBackgroundColor(0x00000000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                float dx = (x - lastX) * SENSITIVITY;
                float dy = (y - lastY) * SENSITIVITY;
                lastX = x;
                lastY = y;

                // Send relative motion to SDL. Doom only uses x (left/right turn),
                // but we forward both so nothing breaks if a mod uses y.
                if (dx != 0.0f || dy != 0.0f) {
                    SDLActivity.onNativeMouse(SDL_BUTTON_LEFT, SDL_RELEASED,
                                              dx, dy, true);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return true;
        }
        return super.onTouchEvent(event);
    }
}