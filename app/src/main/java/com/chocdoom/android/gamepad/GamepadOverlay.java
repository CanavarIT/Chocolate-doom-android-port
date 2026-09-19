package com.chocdoom.android.gamepad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import org.libsdl.app.SDLActivity;

/**
 * Virtual gamepad + true Mouse Lock.
 *
 * - Joystick  = move + strafe only
 * - Buttons   = fire / use / run / ...
 * - Free finger on empty space = ONLY turn (horizontal mouse)
 *
 * Special case for FIRE: the same finger that holds Fire can also
 * drag left/right to turn the camera (like classic mobile Doom ports).
 * Look is processed in dispatchTouchEvent for every pointer,
 * so it works while holding stick or any button.
 */
public class GamepadOverlay extends FrameLayout {

    // Higher = faster turn
    private static final float LOOK_SENSITIVITY = 3.5f;

    private int lookPointerId = -1;
    private float lastLookX;
    private float lastLookY;

    /** Fire button — touch on it may also drive look (hold + drag). */
    private View fireButton;

    public GamepadOverlay(Context context) {
        super(context);
        init(context);
    }

    public GamepadOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GamepadOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        setWillNotDraw(true);
        setClickable(true);
        setFocusable(false);

        float d = ctx.getResources().getDisplayMetrics().density;
        int stickSize  = (int)(130 * d);
        int fireSize   = (int)(100 * d);
        int medSize    = (int)(70  * d);
        int smallSize  = (int)(55  * d);
        int edgeMargin = (int)(10  * d);
        int gap        = (int)(6   * d);

        // ESC
        GamepadButton esc = new GamepadButton(ctx);
        esc.setLabel("ESC");
        esc.setKeycode(111);
        esc.setAccent(0xFF616161);
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(smallSize, smallSize);
        elp.gravity = Gravity.TOP | Gravity.START;
        elp.leftMargin = edgeMargin;
        elp.topMargin = edgeMargin;
        esc.setLayoutParams(elp);
        addView(esc);

        // ENT
        GamepadButton ent = new GamepadButton(ctx);
        ent.setLabel("ENT");
        ent.setKeycode(66);
        ent.setTemporary(true);
        ent.setAccent(0xFF7B1FA2);
        FrameLayout.LayoutParams enlp = new FrameLayout.LayoutParams(smallSize, smallSize);
        enlp.gravity = Gravity.TOP | Gravity.START;
        enlp.leftMargin = edgeMargin + smallSize + gap;
        enlp.topMargin = edgeMargin;
        ent.setLayoutParams(enlp);
        addView(ent);

        // MAP
        GamepadButton map = new GamepadButton(ctx);
        map.setLabel("MAP");
        map.setKeycode(61);
        map.setAccent(0xFF616161);
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(smallSize, smallSize);
        mlp.gravity = Gravity.TOP | Gravity.END;
        mlp.rightMargin = edgeMargin;
        mlp.topMargin = edgeMargin;
        map.setLayoutParams(mlp);
        addView(map);

        // Joystick
        JoystickView joystick = new JoystickView(ctx);
        FrameLayout.LayoutParams jlp = new FrameLayout.LayoutParams(stickSize, stickSize);
        jlp.gravity = Gravity.BOTTOM | Gravity.START;
        jlp.leftMargin = edgeMargin;
        jlp.bottomMargin = edgeMargin;
        joystick.setLayoutParams(jlp);
        addView(joystick);

        // FIRE — keep reference so look can start on the same finger
        GamepadButton fire = new GamepadButton(ctx);
        fire.setLabel("FIRE");
        fire.setKeycode(113);
        fire.setAccent(0xFFD32F2F);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(fireSize, fireSize);
        flp.gravity = Gravity.BOTTOM | Gravity.END;
        flp.rightMargin = edgeMargin;
        flp.bottomMargin = edgeMargin;
        fire.setLayoutParams(flp);
        addView(fire);
        fireButton = fire;

        // USE
        GamepadButton use = new GamepadButton(ctx);
        use.setLabel("USE");
        use.setKeycode(62);
        use.setAccent(0xFF388E3C);
        FrameLayout.LayoutParams ulp = new FrameLayout.LayoutParams(medSize, medSize);
        ulp.gravity = Gravity.BOTTOM | Gravity.END;
        ulp.rightMargin = edgeMargin;
        ulp.bottomMargin = edgeMargin + fireSize + gap;
        use.setLayoutParams(ulp);
        addView(use);

        // RUN
        GamepadButton run = new GamepadButton(ctx);
        run.setLabel("RUN");
        run.setKeycode(59);
        run.setAccent(0xFFF57C00);
        run.setToggle(true);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(medSize, medSize);
        rlp.gravity = Gravity.BOTTOM | Gravity.END;
        rlp.rightMargin = edgeMargin + medSize + gap;
        rlp.bottomMargin = edgeMargin + fireSize + gap;
        run.setLayoutParams(rlp);
        addView(run);

        // W+
        GamepadButton wNext = new GamepadButton(ctx);
        wNext.setLabel("W+");
        wNext.setKeycode(122);
        wNext.setTemporary(true);
        wNext.setAccent(0xFF1976D2);
        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(smallSize, smallSize);
        wlp.gravity = Gravity.BOTTOM | Gravity.END;
        wlp.rightMargin = edgeMargin + fireSize + gap;
        wlp.bottomMargin = edgeMargin;
        wNext.setLayoutParams(wlp);
        addView(wNext);

        // W-
        GamepadButton wPrev = new GamepadButton(ctx);
        wPrev.setLabel("W-");
        wPrev.setKeycode(123);
        wPrev.setTemporary(true);
        wPrev.setAccent(0xFF1976D2);
        FrameLayout.LayoutParams wplp = new FrameLayout.LayoutParams(smallSize, smallSize);
        wplp.gravity = Gravity.BOTTOM | Gravity.END;
        wplp.rightMargin = edgeMargin + fireSize + gap;
        wplp.bottomMargin = edgeMargin + smallSize + gap;
        wPrev.setLayoutParams(wplp);
        addView(wPrev);
    }

    /**
     * True if the point is over any control except the Fire button.
     * Fire is deliberately excluded so the same finger can hold Fire and look.
     */
    private boolean isOverBlockingChild(float x, float y) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            // Allow look to start on the Fire button itself
            if (child == fireButton) continue;
            if (x >= child.getLeft() && x < child.getRight()
                    && y >= child.getTop() && y < child.getBottom()) {
                return true;
            }
        }
        return false;
    }

    private boolean handleLookPointer(MotionEvent event, int pointerIndex) {
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                // Start look on empty space OR on the Fire button
                // (so you can hold Fire and drag to turn, like classic mobile Doom ports)
                if (lookPointerId == -1 && !isOverBlockingChild(x, y)) {
                    lookPointerId = pointerId;
                    lastLookX = x;
                    lastLookY = y;
                    return true;
                }
                return pointerId == lookPointerId;
            }

            case MotionEvent.ACTION_MOVE: {
                if (pointerId != lookPointerId) return false;

                float dx = (x - lastLookX) * LOOK_SENSITIVITY;
                // ONLY horizontal — Vanilla Doom mouse Y = walk forward/back
                float dy = 0f;

                if (Math.abs(dx) > 0.05f) {
                    SDLActivity.onNativeMouse(
                            0,
                            MotionEvent.ACTION_MOVE,
                            dx,
                            dy,
                            true
                    );
                }

                lastLookX = x;
                lastLookY = y;
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pointerId == lookPointerId) {
                    lookPointerId = -1;
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                handleLookPointer(event, i);
            }
        } else {
            handleLookPointer(event, actionIndex);
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}