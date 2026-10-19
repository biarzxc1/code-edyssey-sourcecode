package com.odyssey.game;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
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
 * Secondary neon button for Code Odyssey ("TUTORIAL" with 5 hearts).
 * Dark glass pill, glowing gradient border, mini orbit-planet icon, outlined gradient text and
 * a row of glossy hearts that you can update (lives). Has a gold variant for the Admin button.
 *
 * XML:
 *   <com.odyssey.game.NeonTutorialButton
 *       android:id="@+id/btnTutorial"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       android:text="TUTORIAL" />
 *
 * Java:
 *   btn.setHearts(5);        // 0..5 filled hearts (call again when the player loses/gains a heart)
 *   btn.hideHearts();        // no hearts row
 *   btn.setVariant(NeonTutorialButton.Variant.GOLD); btn.setGlyph("\u2605");  // admin style
 */
public class NeonTutorialButton extends View {

    public enum Variant {
        CYAN(new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7}, 0x22D3EE,
                0xFFA5F3FC, 0xFF22D3EE, 0xFF1D4ED8,
                new int[]{0xFFFFFFFF, 0xFFCFFAFE, 0xFF93C5FD}),
        GOLD(new int[]{0xFFFDE047, 0xFFF59E0B, 0xFFEC4899}, 0xFBBF24,
                0xFFFEF3C7, 0xFFFBBF24, 0xFFD97706,
                new int[]{0xFFFFFFFF, 0xFFFEF3C7, 0xFFFCD34D}),
        RED(new int[]{0xFFFB7185, 0xFFE11D48, 0xFFBE123C}, 0xF43F5E,
                0xFFFECDD3, 0xFFFB7185, 0xFFBE123C,
                new int[]{0xFFFFFFFF, 0xFFFFE4E6, 0xFFFDA4AF});

        final int[] border; final int glow, orbLight, orbMid, orbDark; final int[] text;

        Variant(int[] border, int glow, int l, int m, int d, int[] text) {
            this.border = border; this.glow = glow; this.orbLight = l; this.orbMid = m; this.orbDark = d; this.text = text;
        }
    }

    private static final int INK = 0xFF150F8F;
    private static final int TOTAL_HEARTS = 5;

    private String text = "TUTORIAL", glyph = "?";
    private Variant variant = Variant.CYAN;
    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private int hearts = TOTAL_HEARTS;
    private boolean showHearts = true;

    private final float dp;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF(), tmp = new RectF();
    private final Path path = new Path(), heart = new Path();

    private Shader rimSh, glassSh, glossSh, textSh, shineSh, heartSh;
    private float padX, padY, bodyH, radius, edge, ts;
    private float iconX, iconY, iconR, textCx, heartsX, hs, hSpacing;
    private float idle, press, ripple = -1f, rpx, rpy, pulseT = 1f;
    private int pulseIdx = -1;
    private ValueAnimator idleA, pressA, rippleA, pulseA;

    public NeonTutorialButton(Context c) { this(c, null); }

    public NeonTutorialButton(Context c, AttributeSet a) { this(c, a, 0); }

    public NeonTutorialButton(Context c, AttributeSet a, int s) {
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
        tp.setStrokeJoin(Paint.Join.ROUND);
        heart.moveTo(0f, 0.9f);
        heart.cubicTo(-1.35f, 0.05f, -1.0f, -1.0f, 0f, -0.42f);
        heart.cubicTo(1.0f, -1.0f, 1.35f, 0.05f, 0f, 0.9f);
        heart.close();
        updateDescription();
    }

    // ------------------------------------------------------------ public API

    public void setText(String t) { text = t; updateDescription(); rebuild(); }

    public void setGlyph(String g) { glyph = g; invalidate(); }

    public void setVariant(Variant v) { variant = v; rebuild(); }

    public void setNeonTypeface(Typeface t) { if (t != null) { tf = t; rebuild(); } }

    /** Number of filled hearts (0..5). Animates the heart that was lost or gained. */
    public void setHearts(int filled) {
        filled = Math.max(0, Math.min(TOTAL_HEARTS, filled));
        if (filled == hearts && showHearts) return;
        int old = hearts;
        hearts = filled;
        showHearts = true;
        if (filled != old && isAttachedToWindow()) {
            pulseIdx = filled < old ? filled : filled - 1;
            if (pulseA != null) pulseA.cancel();
            pulseA = anim(0f, 1f, 520, new DecelerateInterpolator(), a -> { pulseT = (float) a.getAnimatedValue(); invalidate(); });
        }
        updateDescription();
        rebuild();
    }

    public int getHearts() { return hearts; }

    public void hideHearts() { showHearts = false; updateDescription(); rebuild(); }

    @Override public void setEnabled(boolean e) { super.setEnabled(e); setAlpha(e ? 1f : 0.5f); }

    private void updateDescription() {
        setContentDescription(text + (showHearts ? ". " + hearts + " of " + TOTAL_HEARTS + " hearts." : ""));
    }

    private void rebuild() {
        if (getWidth() > 0) build(getWidth());
        requestLayout();
        invalidate();
    }

    // ------------------------------------------------------------ layout

    private void metrics(int w) {
        padX = Math.max(10 * dp, w * 0.035f);
        float avail = w - 2 * padX;
        bodyH = Math.max(46 * dp, Math.min(60 * dp, avail * 0.15f));
        padY = bodyH * 0.22f;
        radius = bodyH / 2f;
        edge = bodyH * 0.06f;
    }

    @Override protected void onMeasure(int ws, int hs2) {
        int w = resolveSize((int) (320 * dp), ws);
        metrics(w);
        setMeasuredDimension(w, (int) Math.ceil(bodyH + 2 * padY + edge + 2 * dp));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { build(w); }

    private void build(int w) {
        metrics(w);
        body.set(padX, padY, w - padX, padY + bodyH);
        float cy = body.centerY();

        iconR = bodyH * 0.27f;
        iconX = body.left + bodyH * 0.55f;
        iconY = cy;

        // hearts block (right)
        hs = bodyH * 0.21f;
        float heartsW = 0f;
        if (showHearts) {
            float wh = hs * 2f;
            hSpacing = wh * 0.16f;
            heartsW = TOTAL_HEARTS * wh + (TOTAL_HEARTS - 1) * hSpacing;
        }
        float rightEdge = body.right - bodyH * 0.45f;
        heartsX = rightEdge - heartsW;

        // text area between icon and hearts
        float textLeft = iconX + iconR * 1.75f + bodyH * 0.12f;
        float textRight = showHearts ? heartsX - bodyH * 0.14f : body.right - bodyH * 0.4f;
        textCx = (textLeft + textRight) / 2f;
        float avail = Math.max(20 * dp, textRight - textLeft);
        ts = bodyH * 0.34f;
        tp.setTypeface(tf);
        tp.setLetterSpacing(0.05f);
        tp.setTextSize(ts);
        while (tp.measureText(text) > avail && ts > 9 * dp) { ts -= dp * 0.5f; tp.setTextSize(ts); }

        int[] b = variant.border;
        rimSh = new LinearGradient(body.left, 0, body.right, 0, b, null, Shader.TileMode.CLAMP);
        glassSh = new LinearGradient(0, body.top, 0, body.bottom, 0xF01D1253, 0xF00A0524, Shader.TileMode.CLAMP);
        glossSh = new LinearGradient(0, body.top, 0, body.top + bodyH * 0.45f, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        textSh = new LinearGradient(0, cy - ts * 0.55f, 0, cy + ts * 0.55f, variant.text, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        shineSh = new LinearGradient(0, 0, bodyH, 0, new int[]{0x00FFFFFF, 0x80FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
        heartSh = new LinearGradient(0, -1f, 0, 1f,
                new int[]{0xFFFDA4AF, 0xFFFB3C6E, 0xFFBE123C}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------ lifecycle & touch

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (idleA == null) {
            idleA = ValueAnimator.ofFloat(0f, 1f);
            idleA.setDuration(3400);
            idleA.setRepeatCount(ValueAnimator.INFINITE);
            idleA.setInterpolator(null);
            idleA.addUpdateListener(a -> { idle = (float) a.getAnimatedValue(); invalidate(); });
        }
        if (!idleA.isStarted()) idleA.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (idleA != null) idleA.cancel();
        if (pressA != null) pressA.cancel();
        if (rippleA != null) rippleA.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        if (idleA == null) return;
        if (v == VISIBLE) { if (idleA.isPaused()) idleA.resume(); }
        else if (idleA.isRunning()) idleA.pause();
    }

    private ValueAnimator anim(float a, float b, long ms, TimeInterpolator ip, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator v = ValueAnimator.ofFloat(a, b);
        v.setDuration(ms);
        if (ip != null) v.setInterpolator(ip);
        v.addUpdateListener(l);
        v.start();
        return v;
    }

    private void animatePress(float to, long ms, TimeInterpolator ip) {
        if (pressA != null) pressA.cancel();
        pressA = anim(press, to, ms, ip, a -> { press = (float) a.getAnimatedValue(); invalidate(); });
    }

    private void startRipple(float x, float y) {
        rpx = x; rpy = y;
        if (rippleA != null) rippleA.cancel();
        rippleA = anim(0f, 1f, 600, null, a -> { ripple = (float) a.getAnimatedValue(); invalidate(); });
        rippleA.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator an) { ripple = -1f; invalidate(); }
        });
    }

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
        return super.performClick();
    }

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (rimSh == null) return;
        float w = getWidth();
        int glow = variant.glow;

        // neon glow
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        float pulse = 0.5f + 0.5f * (float) Math.sin(idle * 6.2832f);
        for (int i = 1; i <= 6; i++) {
            float ex = 2.5f * dp + i * 1.5f * dp;
            p.setColor(((int) (62 * (1f - i / 7f) * (0.8f + 0.2f * pulse)) << 24) | glow);
            tmp.set(body); tmp.inset(-ex, -ex);
            c.drawRoundRect(tmp, radius + ex, radius + ex, p);
        }

        // 3D edge (stays put)
        tmp.set(body); tmp.offset(0, edge); tmp.inset(-2.5f * dp, -2.5f * dp);
        fill(c, tmp, radius + 2.5f * dp, 0xFF08061E, null);

        c.save();
        c.translate(0, press * edge);

        tmp.set(body); tmp.inset(-2.5f * dp, -2.5f * dp);
        fill(c, tmp, radius + 2.5f * dp, 0, rimSh);
        fill(c, body, radius, 0, glassSh);
        tmp.set(body.left + bodyH * 0.35f, body.top + 2 * dp, body.right - bodyH * 0.35f, body.top + bodyH * 0.45f);
        fill(c, tmp, bodyH * 0.2f, 0, glossSh);

        // shine + ripple clipped to the pill
        path.reset();
        path.addRoundRect(body, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(path);
        float st = Math.max(0f, Math.min(1f, (idle - 0.6f) / 0.4f));
        if (st > 0f && st < 1f) {
            float x = -bodyH + st * (w + 2 * bodyH);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setTranslate(x, 0);
            shineSh.setLocalMatrix(m);
            p.reset(); p.setAntiAlias(true); p.setShader(shineSh);
            Path band = new Path();
            float sk = bodyH * 0.36f;
            band.moveTo(x, body.top); band.lineTo(x + bodyH, body.top);
            band.lineTo(x + bodyH - sk, body.bottom); band.lineTo(x - sk, body.bottom); band.close();
            c.drawPath(band, p);
        }
        if (ripple >= 0f) {
            float r = bodyH * (0.3f + 2.4f * ripple);
            int a = (int) (255 * (1f - ripple));
            p.reset(); p.setAntiAlias(true);
            p.setShader(new RadialGradient(rpx, rpy, r,
                    new int[]{((a * 0xCC / 255) << 24) | 0xFFFFFF, ((a * 0x55 / 255) << 24) | glow, glow & 0xFFFFFF},
                    new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(rpx, rpy, r, p);
        }
        c.restore();

        drawIcon(c);
        drawText(c);
        if (showHearts) drawHearts(c);
        c.restore();
    }

    private void fill(Canvas c, RectF r, float rad, int color, Shader sh) {
        p.reset(); p.setAntiAlias(true);
        if (sh != null) p.setShader(sh); else p.setColor(color);
        c.drawRoundRect(r, rad, rad, p);
    }

    /** Mini planet with a tilted orbit ring and a tiny moon, like the logo. */
    private void drawIcon(Canvas c) {
        float R = iconR;
        // orbit ring (behind)
        c.save();
        c.rotate(-24f, iconX, iconY);
        tmp.set(iconX - R * 1.7f, iconY - R * 0.62f, iconX + R * 1.7f, iconY + R * 0.62f);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.2f * dp); p.setColor(INK);
        c.drawOval(tmp, p);
        p.setStrokeWidth(1.8f * dp); p.setColor(variant.orbMid);
        c.drawOval(tmp, p);
        c.restore();

        // planet
        p.reset(); p.setAntiAlias(true); p.setColor(0x66000000 | variant.glow);
        c.drawCircle(iconX, iconY, R * 1.35f, p);
        p.reset(); p.setAntiAlias(true); p.setColor(INK);
        c.drawCircle(iconX, iconY, R + 1.8f * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(iconX - R * 0.3f, iconY - R * 0.4f, R * 1.5f,
                new int[]{0xFFFFFFFF, variant.orbLight, variant.orbMid, variant.orbDark},
                new float[]{0f, 0.15f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(iconX, iconY, R, p);

        // glyph on the planet
        tp.setTypeface(tf); tp.setShader(null); tp.setLetterSpacing(0f);
        tp.setTextAlign(Paint.Align.CENTER); tp.setTextSize(R * 1.25f);
        float base = iconY - (tp.ascent() + tp.descent()) / 2f;
        tp.setStyle(Paint.Style.STROKE); tp.setStrokeWidth(R * 0.28f); tp.setColor(INK);
        c.drawText(glyph, iconX, base, tp);
        tp.setStyle(Paint.Style.FILL); tp.setColor(0xFFFFFFFF);
        c.drawText(glyph, iconX, base, tp);

        // moon travelling on the orbit
        double ang = idle * Math.PI * 2;
        float mxp = (float) Math.cos(ang) * R * 1.7f, myp = (float) Math.sin(ang) * R * 0.62f;
        double rot = Math.toRadians(-24);
        float mx = iconX + (float) (mxp * Math.cos(rot) - myp * Math.sin(rot));
        float my = iconY + (float) (mxp * Math.sin(rot) + myp * Math.cos(rot));
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFFFFFF);
        c.drawCircle(mx, my, 2.4f * dp, p);
    }

    private void drawText(Canvas c) {
        float base = body.centerY() - (tp.ascent() + tp.descent()) / 2f;
        tp.setTypeface(tf); tp.setTextAlign(Paint.Align.CENTER); tp.setLetterSpacing(0.05f); tp.setTextSize(ts);
        tp.setShader(null); tp.setStyle(Paint.Style.STROKE);
        tp.setStrokeWidth(ts * 0.44f); tp.setColor(0x55000000 | variant.glow);
        c.drawText(text, textCx, base, tp);
        tp.setStrokeWidth(ts * 0.24f); tp.setColor(INK);
        c.drawText(text, textCx, base + ts * 0.05f, tp);
        c.drawText(text, textCx, base, tp);
        tp.setStyle(Paint.Style.FILL); tp.setShader(textSh);
        c.drawText(text, textCx, base, tp);
        tp.setShader(null);
    }

    private void drawHearts(Canvas c) {
        float wh = hs * 2f, cy = body.centerY();
        for (int i = 0; i < TOTAL_HEARTS; i++) {
            float cx = heartsX + wh / 2f + i * (wh + hSpacing);
            boolean on = i < hearts;
            float wave = on ? 0.1f * Math.max(0f, (float) Math.sin(idle * 6.2832f - i * 0.7f)) : 0f;
            float sc = hs * (1f + wave);
            if (i == pulseIdx && pulseT < 1f) sc *= 1f + 0.6f * (1f - pulseT) * (float) Math.sin(pulseT * Math.PI * 2 + 1.2f);

            c.save();
            c.translate(cx, cy + hs * 0.05f);
            c.scale(sc, sc);

            // dark outline
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.FILL_AND_STROKE);
            p.setStrokeJoin(Paint.Join.ROUND); p.setStrokeWidth(2.6f * dp / sc); p.setColor(INK);
            c.drawPath(heart, p);

            p.reset(); p.setAntiAlias(true);
            if (on) {
                p.setShader(heartSh);
                c.drawPath(heart, p);
                // glossy highlight
                c.save();
                c.rotate(-28f, -0.45f, -0.5f);
                tmp.set(-0.72f, -0.62f, -0.16f, -0.4f);
                p.reset(); p.setAntiAlias(true); p.setColor(0xCCFFFFFF);
                c.drawOval(tmp, p);
                c.restore();
                // soft glow
                p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.2f * dp / sc); p.setColor(0x99FDA4AF);
                c.drawPath(heart, p);
            } else {
                p.setColor(0xFF1B1250);
                c.drawPath(heart, p);
                p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.4f * dp / sc); p.setColor(0xFF5B3FA8);
                c.drawPath(heart, p);
            }
            c.restore();
        }
    }
}
