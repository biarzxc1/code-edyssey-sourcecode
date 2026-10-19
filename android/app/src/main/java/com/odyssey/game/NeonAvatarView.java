package com.odyssey.game;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Profile avatar for Code Odyssey: photo inside a rotating neon ring, wrapped by a tilted orbit
 * with a moon, a twinkling sparkle and a glossy "+" badge.
 *
 * XML:
 *   <com.odyssey.game.NeonAvatarView
 *       android:id="@+id/avatar"
 *       android:layout_width="200dp"
 *       android:layout_height="200dp"
 *       android:layout_gravity="center_horizontal" />
 *
 * Java:
 *   avatar.setInitial(username);                 // shown when there is no photo
 *   avatar.setImageBitmap(bitmapOrNull);         // null = back to the initial; a new photo plays a burst
 *   avatar.setOnBadgeClickListener(v -> pickPhoto());
 *   avatar.setOnClickListener(v -> { ... });     // tap on the photo itself
 */
public class NeonAvatarView extends View {

    private static final int INK = 0xFF150F8F;

    private final float dp;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path path = new Path();
    private final Matrix mx = new Matrix();
    private final Random rnd = new Random();
    private final ArrayList<float[]> parts = new ArrayList<>();

    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private Bitmap bmp;
    private BitmapShader bmpShader;
    private SweepGradient ringSh;
    private Shader orbitSh, fallbackSh;
    private String initial = "?";
    private OnClickListener badgeListener;

    private float cx, cy, R, ringW, rx, ry, br, bx, by;
    private float idle, pop = 1f, flash = 1f, press, badgePress;
    private boolean downOnBadge;
    private ValueAnimator idleA, popA, flashA, pressA;
    private long lastNs;

    public NeonAvatarView(Context c) { this(c, null); }

    public NeonAvatarView(Context c, AttributeSet a) { this(c, a, 0); }

    public NeonAvatarView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        dp = c.getResources().getDisplayMetrics().density;
        setClickable(true);
        setFocusable(true);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription("Profile photo");
    }

    // ------------------------------------------------------------ public API

    public void setInitial(String name) {
        initial = (name == null || name.trim().isEmpty()) ? "?" : name.trim().substring(0, 1).toUpperCase();
        invalidate();
    }

    public void setImageBitmap(Bitmap b) {
        boolean changed = b != bmp;
        bmp = b;
        buildBitmapShader();
        if (changed && b != null && isAttachedToWindow()) celebrate();
        invalidate();
    }

    public void setOnBadgeClickListener(OnClickListener l) { badgeListener = l; }

    public void setNeonTypeface(Typeface t) { if (t != null) { tf = t; invalidate(); } }

    /** Pop + ring flash + particle burst (called automatically when a new photo is set). */
    public void celebrate() {
        if (popA != null) popA.cancel();
        popA = anim(0f, 1f, 520, new AccelerateDecelerateInterpolator(), a -> { pop = (float) a.getAnimatedValue(); invalidate(); });
        if (flashA != null) flashA.cancel();
        flashA = anim(0f, 1f, 900, null, a -> { flash = (float) a.getAnimatedValue(); invalidate(); });
        for (int i = 0; i < 26; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            float sp = (70f + rnd.nextFloat() * 190f) * dp;
            parts.add(new float[]{cx + (float) Math.cos(ang) * R, cy + (float) Math.sin(ang) * R,
                    (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp, 1f, rnd.nextBoolean() ? 0xFFFFFF : 0x67E8F9});
        }
    }

    // ------------------------------------------------------------ layout

    @Override protected void onMeasure(int ws, int hs) {
        int w = resolveSize((int) (200 * dp), ws);
        int h = resolveSize(w, hs);
        int s = Math.min(w, h);
        setMeasuredDimension(s, s);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float size = Math.min(w, h);
        cx = w / 2f; cy = h / 2f;
        R = size * 0.30f;
        ringW = size * 0.035f;
        rx = size * 0.47f; ry = size * 0.15f;
        br = size * 0.085f;
        bx = cx + R * 0.74f; by = cy + R * 0.74f;
        ringSh = new SweepGradient(cx, cy,
                new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7, 0xFFD946EF, 0xFF22D3EE}, new float[]{0f, 0.25f, 0.5f, 0.75f, 1f});
        orbitSh = new LinearGradient(cx - rx, 0, cx + rx, 0,
                new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7}, null, Shader.TileMode.CLAMP);
        fallbackSh = new LinearGradient(0, cy - R, 0, cy + R,
                new int[]{0xFF22D3EE, 0xFF4F46E5, 0xFFC026D3}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        buildBitmapShader();
    }

    private void buildBitmapShader() {
        if (bmp == null || R <= 0) { bmpShader = null; return; }
        bmpShader = new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float s = Math.max(2 * R / bmp.getWidth(), 2 * R / bmp.getHeight());
        mx.setScale(s, s);
        mx.postTranslate(cx - bmp.getWidth() * s / 2f, cy - bmp.getHeight() * s / 2f);
        bmpShader.setLocalMatrix(mx);
    }

    // ------------------------------------------------------------ lifecycle & touch

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (idleA == null) {
            idleA = ValueAnimator.ofFloat(0f, 1f);
            idleA.setDuration(6800);
            idleA.setRepeatCount(ValueAnimator.INFINITE);
            idleA.setInterpolator(null);
            idleA.addUpdateListener(a -> { idle = (float) a.getAnimatedValue(); invalidate(); });
        }
        if (!idleA.isStarted()) idleA.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (idleA != null) idleA.cancel();
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

    private boolean onBadge(float x, float y) {
        float dx = x - bx, dy = y - by, r = br * 1.8f; // generous touch area
        return dx * dx + dy * dy <= r * r;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downOnBadge = onBadge(e.getX(), e.getY());
                if (pressA != null) pressA.cancel();
                pressA = anim(0f, 1f, 80, new DecelerateInterpolator(), a -> {
                    float v = (float) a.getAnimatedValue();
                    if (downOnBadge) badgePress = v; else press = v;
                    invalidate();
                });
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean up = e.getActionMasked() == MotionEvent.ACTION_UP;
                boolean wasBadge = downOnBadge;
                if (pressA != null) pressA.cancel();
                final float p0 = wasBadge ? badgePress : press;
                pressA = anim(p0, 0f, 160, new DecelerateInterpolator(), a -> {
                    float v = (float) a.getAnimatedValue();
                    if (wasBadge) badgePress = v; else press = v;
                    invalidate();
                });
                if (up) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (wasBadge && onBadge(e.getX(), e.getY())) { if (badgeListener != null) badgeListener.onClick(this); }
                    else if (!wasBadge) performClick();
                }
                return true;
        }
        return super.onTouchEvent(e);
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (ringSh == null) return;
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;

        float sc = (1f + 0.12f * (float) Math.sin(pop * Math.PI)) * (1f - 0.04f * press);
        c.save();
        c.scale(sc, sc, cx, cy);

        float outer = R + ringW;
        // glow
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 7; i++) {
            int a = (int) (64 * (1f - i / 8f) * (1f + 1.2f * (1f - flash)));
            p.setColor((Math.min(255, a) << 24) | (i % 2 == 0 ? 0xA855F7 : 0x38BDF8));
            c.drawCircle(cx, cy, outer + i * 2f * dp, p);
        }

        drawOrbit(c, false);

        // ring
        p.reset(); p.setAntiAlias(true); p.setColor(INK);
        c.drawCircle(cx, cy, outer + 1.8f * dp, p);
        mx.setRotate(idle * 360f, cx, cy);
        ringSh.setLocalMatrix(mx);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(ringW); p.setShader(ringSh);
        c.drawCircle(cx, cy, R + ringW / 2f, p);
        if (flash < 1f) {
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(ringW * 1.2f);
            p.setColor(((int) (255 * (1f - flash)) << 24) | 0xFFFFFF);
            c.drawCircle(cx, cy, R + ringW / 2f, p);
        }

        // photo or initial
        p.reset(); p.setAntiAlias(true);
        if (bmpShader != null) {
            p.setShader(bmpShader);
            c.drawCircle(cx, cy, R, p);
        } else {
            p.setShader(fallbackSh);
            c.drawCircle(cx, cy, R, p);
            tp.setTypeface(tf); tp.setShader(null); tp.setLetterSpacing(0f); tp.setTextSize(R * 1.1f);
            float base = cy - (tp.ascent() + tp.descent()) / 2f;
            tp.setStyle(Paint.Style.STROKE); tp.setStrokeWidth(R * 0.16f); tp.setColor(INK);
            c.drawText(initial, cx, base, tp);
            tp.setStyle(Paint.Style.FILL); tp.setColor(0xFFFFFFFF);
            c.drawText(initial, cx, base, tp);
        }
        // glass gloss on top of the photo
        oval.set(cx - R * 0.78f, cy - R * 0.95f, cx + R * 0.78f, cy - R * 0.1f);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, oval.top, 0, oval.bottom, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawOval(oval, p);

        drawOrbit(c, true);
        c.restore();

        drawSparkle(c);
        drawBadge(c);
        drawParticles(c, dt);
    }

    /** Tilted orbit ring. front=true draws only the lower half so it wraps in front of the photo. */
    private void drawOrbit(Canvas c, boolean front) {
        c.save();
        c.rotate(-18f, cx, cy);
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(5.5f * dp); p.setColor(INK);
        if (front) c.drawArc(oval, 8, 164, false, p); else c.drawOval(oval, p);
        p.setStrokeWidth(3.2f * dp); p.setShader(orbitSh);
        if (front) c.drawArc(oval, 8, 164, false, p); else c.drawOval(oval, p);
        if (front) {
            // moon travelling along the orbit
            double a = idle * Math.PI * 2;
            float mx2 = cx + (float) Math.cos(a) * rx, my2 = cy + (float) Math.sin(a) * ry;
            p.reset(); p.setAntiAlias(true); p.setColor(0x6638BDF8);
            c.drawCircle(mx2, my2, 6.5f * dp, p);
            p.reset(); p.setAntiAlias(true); p.setColor(INK);
            c.drawCircle(mx2, my2, 4.2f * dp, p);
            p.reset(); p.setAntiAlias(true);
            p.setShader(new RadialGradient(mx2 - 1.2f * dp, my2 - 1.4f * dp, 5f * dp,
                    new int[]{0xFFFFFFFF, 0xFF67E8F9, 0xFF2563EB}, new float[]{0f, 0.3f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(mx2, my2, 3.4f * dp, p);
        }
        c.restore();
    }

    private void drawSparkle(Canvas c) {
        float tw = 0.75f + 0.25f * (float) Math.sin(idle * 25.13f);
        float s = R * 0.2f * tw, sx = cx + R * 0.98f, sy = cy - R * 1.0f;
        p.reset(); p.setAntiAlias(true); p.setColor(0x66F0ABFC);
        c.drawCircle(sx, sy, s * 1.3f, p);
        path.reset();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45);
            float rr = (i % 2 == 0) ? s : s * 0.22f;
            float x = sx + (float) Math.cos(a) * rr, y = sy + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFBCFE8);
        c.drawPath(path, p);
    }

    private void drawBadge(Canvas c) {
        float s = 1f - 0.12f * badgePress;
        c.save();
        c.scale(s, s, bx, by);
        p.reset(); p.setAntiAlias(true); p.setColor(0x6614B8A6);
        c.drawCircle(bx, by, br * 1.5f, p);
        p.reset(); p.setAntiAlias(true); p.setColor(INK);
        c.drawCircle(bx, by, br + 2.2f * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(bx - br * 0.3f, by - br * 0.4f, br * 1.5f,
                new int[]{0xFFFFFFFF, 0xFF99F6E4, 0xFF14B8A6, 0xFF0F766E}, new float[]{0f, 0.15f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(bx, by, br, p);
        // plus sign
        float t = br * 0.5f;
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(br * 0.5f); p.setColor(INK);
        c.drawLine(bx - t, by, bx + t, by, p);
        c.drawLine(bx, by - t, bx, by + t, p);
        p.setStrokeWidth(br * 0.3f); p.setColor(0xFFFFFFFF);
        c.drawLine(bx - t, by, bx + t, by, p);
        c.drawLine(bx, by - t, bx, by + t, p);
        c.restore();
    }

    private void drawParticles(Canvas c, float dt) {
        Iterator<float[]> it = parts.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            float drag = (float) Math.exp(-dt * 2.2f);
            q[2] *= drag; q[3] *= drag;
            q[0] += q[2] * dt; q[1] += q[3] * dt;
            q[4] -= dt * 1.5f;
            if (q[4] <= 0f) { it.remove(); continue; }
            p.reset(); p.setAntiAlias(true);
            p.setColor(((int) (255 * q[4]) << 24) | ((int) q[5] & 0xFFFFFF));
            c.drawCircle(q[0], q[1], 2.4f * dp * q[4] + dp * 0.6f, p);
        }
    }
}
