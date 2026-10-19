package com.odyssey.game;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

/**
 * Neon "START QUIZ" button that matches the Code Odyssey logo:
 * gradient body, dark-blue outline + cyan rim, gloss, glowing outlined gradient text,
 * orbit ring with glossy orb and sparkle, moving shine, squash press and tap ripple.
 *
 * Usage in XML:
 *   <com.odyssey.game.NeonQuizButton
 *       android:id="@+id/btnStartQuiz"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       android:text="START QUIZ" />
 * Use setOnClickListener(...) like any other button. Height is automatic and scales with width.
 */
public class NeonQuizButton extends View {

    private static final int INK = 0xFF150F8F, EDGE = 0xFF2A0F8F, RIM = 0xFF67E8F9;

    private String text = "START QUIZ";
    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private final float dp;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF(), tmp = new RectF();
    private final Path path = new Path(), star = new Path();
    private final Matrix mx = new Matrix();

    private Shader bodyShader, glossShader, textShader, orbitShader, orbShader, shineShader;
    private float padX, padY, bodyH, radius, edge, ts, orbR, ox, oy, shineW;
    private float idle, press, ripple = -1f, rpx, rpy;
    private ValueAnimator idleAnim, pressAnim, rippleAnim;

    public NeonQuizButton(Context c) { this(c, null); }

    public NeonQuizButton(Context c, AttributeSet a) { this(c, a, 0); }

    public NeonQuizButton(Context c, AttributeSet a, int s) {
        super(c, a, s);
        dp = c.getResources().getDisplayMetrics().density;
        setClickable(true);
        setFocusable(true);
        if (a != null) {
            TypedArray ta = c.obtainStyledAttributes(a, new int[]{android.R.attr.text});
            CharSequence t = ta.getText(0);
            if (t != null) text = t.toString();
            ta.recycle();
        }
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription(text);
    }

    // ------------------------------------------------------------ public API

    public void setText(String t) {
        text = t;
        setContentDescription(t);
        if (getWidth() > 0) { build(getWidth()); }
        invalidate();
    }

    /** Optional: call with a bold wide font, e.g. ResourcesCompat.getFont(ctx, R.font.unbounded_extrabold). */
    public void setNeonTypeface(Typeface t) {
        if (t == null) return;
        tf = t;
        if (getWidth() > 0) { build(getWidth()); }
        invalidate();
    }

    @Override public void setEnabled(boolean e) {
        super.setEnabled(e);
        setAlpha(e ? 1f : 0.5f);
    }

    // ------------------------------------------------------------ layout

    private void metrics(int w) {
        padX = Math.max(10 * dp, w * 0.035f);
        float avail = w - 2 * padX;
        bodyH = Math.max(54 * dp, Math.min(76 * dp, avail * 0.19f));
        padY = bodyH * 0.32f;
        radius = bodyH * 0.3f;
        edge = bodyH * 0.07f;
    }

    @Override protected void onMeasure(int ws, int hs) {
        int w = resolveSize((int) (320 * dp), ws);
        metrics(w);
        setMeasuredDimension(w, (int) Math.ceil(bodyH + 2 * padY + edge + 2 * dp));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        build(w);
    }

    private void build(int w) {
        metrics(w);
        body.set(padX, padY, w - padX, padY + bodyH);

        // fit text to the body width
        float avail = body.width() - bodyH * 0.7f;
        ts = bodyH * 0.40f;
        tp.setTypeface(tf);
        tp.setLetterSpacing(0.05f);
        tp.setTextSize(ts);
        while (tp.measureText(text) > avail && ts > 9 * dp) { ts -= dp * 0.5f; tp.setTextSize(ts); }

        float cy = body.centerY();
        bodyShader = new LinearGradient(0, body.top, 0, body.bottom,
                new int[]{0xFF22D3EE, 0xFF2563EB, 0xFF5B21B6, 0xFFD946EF}, new float[]{0f, 0.3f, 0.68f, 1f}, Shader.TileMode.CLAMP);
        glossShader = new LinearGradient(0, body.top, 0, body.top + bodyH * 0.5f,
                0xBFFFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP);
        textShader = new LinearGradient(0, cy - ts * 0.55f, 0, cy + ts * 0.55f,
                new int[]{0xFFFFFFFF, 0xFFCFFAFE, 0xFFC4B5FD, 0xFFF5D0FE}, new float[]{0f, 0.42f, 0.72f, 1f}, Shader.TileMode.CLAMP);
        orbitShader = new LinearGradient(0, 0, w, 0,
                new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7}, null, Shader.TileMode.CLAMP);
        orbR = Math.max(11 * dp, bodyH * 0.22f);
        ox = w - padX - orbR * 0.4f;
        oy = padY * 0.55f;
        orbShader = new RadialGradient(ox - orbR * 0.35f, oy - orbR * 0.4f, orbR * 1.5f,
                new int[]{0xFFFFFFFF, 0xFF67E8F9, 0xFF2563EB, 0xFF6D28D9}, new float[]{0f, 0.18f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        shineW = bodyH * 1.1f;
        shineShader = new LinearGradient(0, 0, shineW, 0,
                new int[]{0x00FFFFFF, 0x99FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------ lifecycle / animation

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (idleAnim == null) {
            idleAnim = ValueAnimator.ofFloat(0f, 1f);
            idleAnim.setDuration(3400);
            idleAnim.setRepeatCount(ValueAnimator.INFINITE);
            idleAnim.setInterpolator(null);
            idleAnim.addUpdateListener(a -> { idle = (float) a.getAnimatedValue(); invalidate(); });
        }
        if (!idleAnim.isStarted()) idleAnim.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (idleAnim != null) idleAnim.cancel();
        if (pressAnim != null) pressAnim.cancel();
        if (rippleAnim != null) rippleAnim.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        if (idleAnim == null) return;
        if (v == VISIBLE) { if (idleAnim.isPaused()) idleAnim.resume(); }
        else if (idleAnim.isRunning()) idleAnim.pause();
    }

    private void animatePress(float to, long ms, TimeInterpolator ip) {
        if (pressAnim != null) pressAnim.cancel();
        pressAnim = ValueAnimator.ofFloat(press, to);
        pressAnim.setDuration(ms);
        pressAnim.setInterpolator(ip);
        pressAnim.addUpdateListener(a -> { press = (float) a.getAnimatedValue(); invalidate(); });
        pressAnim.start();
    }

    private void startRipple(float x, float y) {
        rpx = x; rpy = y;
        if (rippleAnim != null) rippleAnim.cancel();
        rippleAnim = ValueAnimator.ofFloat(0f, 1f);
        rippleAnim.setDuration(650);
        rippleAnim.addUpdateListener(a -> { ripple = (float) a.getAnimatedValue(); invalidate(); });
        rippleAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator an) { ripple = -1f; invalidate(); }
        });
        rippleAnim.start();
    }

    // ------------------------------------------------------------ touch

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                animatePress(1f, 80, new DecelerateInterpolator());
                startRipple(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isPressed() && (e.getX() < 0 || e.getY() < 0 || e.getX() > getWidth() || e.getY() > getHeight())) {
                    setPressed(false);
                    animatePress(0f, 140, new DecelerateInterpolator());
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean was = isPressed();
                setPressed(false);
                animatePress(0f, 180, new OvershootInterpolator(2f));
                if (was) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                animatePress(0f, 140, new DecelerateInterpolator());
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override public boolean performClick() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        NeonSound.play(getContext(), NeonSound.Sfx.CLICK);
        return super.performClick();
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (bodyShader == null) return;
        float w = getWidth();
        float pulse = 0.5f + 0.5f * (float) Math.sin(idle * 6.2832f);

        drawOrbit(c, w);
        drawGlow(c, pulse);

        // 3D bottom edge (stays fixed while the button sinks)
        tmp.set(body); tmp.offset(0, edge); tmp.inset(-4.5f * dp, -4.5f * dp);
        fill(c, tmp, radius + 4.5f * dp, EDGE, null);

        c.save();
        c.translate(0, press * edge);

        tmp.set(body); tmp.inset(-4.5f * dp, -4.5f * dp);
        fill(c, tmp, radius + 4.5f * dp, RIM, null);
        tmp.set(body); tmp.inset(-3f * dp, -3f * dp);
        fill(c, tmp, radius + 3f * dp, INK, null);
        fill(c, body, radius, 0, bodyShader);

        // gloss
        tmp.set(body.left + bodyH * 0.2f, body.top + bodyH * 0.06f, body.right - bodyH * 0.2f, body.top + bodyH * 0.5f);
        fill(c, tmp, bodyH * 0.2f, 0, glossShader);

        // shine + ripple, clipped to the body
        path.reset();
        path.addRoundRect(body, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(path);
        float st = Math.max(0f, Math.min(1f, (idle - 0.55f) / 0.45f));
        if (st > 0f && st < 1f) {
            float x = -shineW + st * (w + 2 * shineW);
            mx.setTranslate(x, 0);
            shineShader.setLocalMatrix(mx);
            p.reset(); p.setAntiAlias(true); p.setShader(shineShader);
            Path band = new Path();
            float sk = bodyH * 0.36f;
            band.moveTo(x, body.top); band.lineTo(x + shineW, body.top);
            band.lineTo(x + shineW - sk, body.bottom); band.lineTo(x - sk, body.bottom); band.close();
            c.drawPath(band, p);
        }
        if (ripple >= 0f) {
            float r = bodyH * (0.3f + 2.2f * ripple);
            int a = (int) (255 * (1f - ripple));
            p.reset(); p.setAntiAlias(true);
            p.setShader(new RadialGradient(rpx, rpy, r,
                    new int[]{(a * 0xDD / 255) << 24 | 0xFFFFFF, (a * 0x66 / 255) << 24 | 0x67E8F9, 0x0067E8F9},
                    new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(rpx, rpy, r, p);
        }
        c.restore();

        drawText(c);
        c.restore();

        drawOrbAndSpark(c, pulse);
    }

    private void fill(Canvas c, RectF r, float rad, int color, Shader sh) {
        p.reset(); p.setAntiAlias(true);
        if (sh != null) p.setShader(sh); else p.setColor(color);
        c.drawRoundRect(r, rad, rad, p);
    }

    private void drawGlow(Canvas c, float pulse) {
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 6; i++) {
            float ex = 4.5f * dp + i * 1.6f * dp;
            int a = (int) (70 * (1f - i / 7f) * (0.8f + 0.2f * pulse));
            p.setColor((a << 24) | (i % 2 == 0 ? 0xA855F7 : 0x60A5FA));
            tmp.set(body); tmp.inset(-ex, -ex);
            c.drawRoundRect(tmp, radius + ex, radius + ex, p);
        }
    }

    private void drawOrbit(Canvas c, float w) {
        float cyo = padY + bodyH / 2f;
        float ry = bodyH / 2f + padY * 0.78f;
        tmp.set(3 * dp, cyo - ry, w - 3 * dp, cyo + ry);
        c.save();
        c.rotate(-4f, w / 2f, cyo);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(6 * dp); p.setColor(0xFF1D1A9E);
        c.drawOval(tmp, p);
        p.setStrokeWidth(3.5f * dp); p.setShader(orbitShader);
        c.drawOval(tmp, p);
        p.setShader(null); p.setStrokeWidth(1.2f * dp); p.setColor(0x8CFFFFFF);
        p.setPathEffect(new DashPathEffect(new float[]{26 * dp, 170 * dp}, -idle * 196 * dp));
        c.drawOval(tmp, p);
        c.restore();
    }

    private void drawText(Canvas c) {
        float cx = body.centerX();
        float base = body.centerY() - (tp.ascent() + tp.descent()) / 2f;
        tp.setShader(null);
        tp.setStyle(Paint.Style.STROKE);
        tp.setStrokeWidth(ts * 0.44f); tp.setColor(0x5567E8F9);          // neon glow
        c.drawText(text, cx, base, tp);
        tp.setStrokeWidth(ts * 0.24f); tp.setColor(INK);                  // dark outline + drop
        c.drawText(text, cx, base + ts * 0.06f, tp);
        c.drawText(text, cx, base, tp);
        tp.setStyle(Paint.Style.FILL);
        tp.setShader(textShader);                                         // gradient fill
        c.drawText(text, cx, base, tp);
        tp.setShader(null);
    }

    private void drawOrbAndSpark(Canvas c, float pulse) {
        // orb
        p.reset(); p.setAntiAlias(true); p.setColor(0x6638BDF8);
        c.drawCircle(ox, oy, orbR * 1.5f, p);
        p.reset(); p.setAntiAlias(true); p.setShader(orbShader);
        c.drawCircle(ox, oy, orbR, p);

        // sparkle (twinkles twice per loop)
        float tw = 0.75f + 0.25f * (float) Math.sin(idle * 12.566f);
        float R = bodyH * 0.24f * tw;
        float sx = getWidth() * 0.78f, sy = padY * 0.34f;
        p.reset(); p.setAntiAlias(true); p.setColor(0x66F0ABFC);
        c.drawCircle(sx, sy, R * 1.3f, p);
        star.reset();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45) + idle * 0.4f * (pulse - 0.5f);
            float rr = (i % 2 == 0) ? R : R * 0.22f;
            float x = sx + (float) Math.cos(a) * rr, y = sy + (float) Math.sin(a) * rr;
            if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
        }
        star.close();
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFBCFE8);
        c.drawPath(star, p);
    }
}
