package com.chocdoom.android.gamepad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

public class GamepadOverlay extends FrameLayout {

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

        float d = ctx.getResources().getDisplayMetrics().density;
        int stickSize  = (int)(130 * d);  // джойстик
        int fireSize   = (int)(100 * d);  // fire — крупная
        int medSize    = (int)(70  * d);  // use, run
        int smallSize  = (int)(55  * d);  // esc, map, weapon
        int edgeMargin = (int)(10  * d);  // прижатие к краям
        int gap        = (int)(6   * d);  // между кнопками

        // === ESC — верхний левый (мелкая, в самом углу) ===
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

        // === MAP — верхний правый (мелкая, в самом углу) ===
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

        // === Джойстик — нижний левый ===
        JoystickView joystick = new JoystickView(ctx);
        FrameLayout.LayoutParams jlp = new FrameLayout.LayoutParams(stickSize, stickSize);
        jlp.gravity = Gravity.BOTTOM | Gravity.START;
        jlp.leftMargin = edgeMargin;
        jlp.bottomMargin = edgeMargin;
        joystick.setLayoutParams(jlp);
        addView(joystick);

        // === FIRE — нижний правый (большая, под большим пальцем) ===
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

        // === USE — над FIRE ===
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

        // === RUN — слева от USE ===
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

        // === W+ — слева от FIRE ===
        GamepadButton wNext = new GamepadButton(ctx);
        wNext.setLabel("W+");
        wNext.setKeycode(122); // KEYCODE_HOME
        wNext.setTemporary(true);
        wNext.setAccent(0xFF1976D2);
        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(smallSize, smallSize);
        wlp.gravity = Gravity.BOTTOM | Gravity.END;
        wlp.rightMargin = edgeMargin + fireSize + gap;
        wlp.bottomMargin = edgeMargin;
        wNext.setLayoutParams(wlp);
        addView(wNext);

        // === W- — над W+ ===
        GamepadButton wPrev = new GamepadButton(ctx);
        wPrev.setLabel("W-");
        wPrev.setKeycode(123); // KEYCODE_END
        wPrev.setTemporary(true);
        wPrev.setAccent(0xFF1976D2);
        FrameLayout.LayoutParams wplp = new FrameLayout.LayoutParams(smallSize, smallSize);
        wplp.gravity = Gravity.BOTTOM | Gravity.END;
        wplp.rightMargin = edgeMargin + fireSize + gap;
        wplp.bottomMargin = edgeMargin + smallSize + gap;
        wPrev.setLayoutParams(wplp);
        addView(wPrev);
    }
}
