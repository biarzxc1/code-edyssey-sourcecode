package com.odyssey.game;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Neon bottom navigation for Code Odyssey (Home / Admin / Profile).
 * A glowing planet with an orbiting moon slides to the selected tab; icons lift and glow.
 *
 * XML (put at the bottom of your root layout):
 *   <com.odyssey.game.NeonBottomNav
 *       android:id="@+id/bottomNav"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       android:layout_gravity="bottom" />
 *
 * Java:
 *   nav.setSelected(NeonBottomNav.HOME, false);
 *   nav.setAdminVisible(isAdmin);
 *   nav.setListener((id, changed) -> { if (id == NeonBottomNav.PROFILE) ... });
 *   nav.setBottomInset(systemBarsBottomPx);   // optional, for gesture bar / navigation bar
 */
public class NeonBottomNav extends View {

    public static final int HOME = 0, ADMIN = 1, PROFILE = 2;

    public interface Listener { void onSelected(int id, boolean changed); }

    private static final int INK = 0xFF150F8F;
    private static final String[] LABELS = {"Home", "Admin", "Profile"};
    private static final int[][] COL = { // light, mid, dark
            {0xFFA5F3FC, 0xFF22D3EE, 0xFF1D4ED8},
            {0xFFFEF3C7, 0xFFFBBF24, 0xFFD97706},
            {0xFFFBCFE8, 0xFFD946EF, 0xFF6D28D9}};

    private final float dp;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF(), r2 = new RectF();
    private final Path path = new Path();
    private final Path[] icons = new Path[3];

    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private boolean adminVisible = true;
    private int selected = HOME, pressed = -1, bottomInset;
    private Listener listener;
    private float pos, idle, pressT, gt, barH, corner;
    private Shader rimSh, glassSh;
    private ValueAnimator posA, idleA, pressA;

    public NeonBottomNav(Context c) { this(c, null); }

    public NeonBottomNav(Context c, AttributeSet a) { this(c, a, 0); }

    public NeonBottomNav(Context c, AttributeSet a, int s) {
        super(c, a, s);
        dp = c.getResources().getDisplayMetrics().density;
        setClickable(true);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setStrokeJoin(Paint.Join.ROUND);

        Path home = new Path();
        home.moveTo(-1f, 0.1f); home.lineTo(0f, -0.92f); home.lineTo(1f, 0.1f); home.lineTo(0.78f, 0.1f);
        home.lineTo(0.78f, 0.92f); home.lineTo(0.22f, 0.92f); home.lineTo(0.22f, 0.36f); home.lineTo(-0.22f, 0.36f);
        home.lineTo(-0.22f, 0.92f); home.lineTo(-0.78f, 0.92f); home.lineTo(-0.78f, 0.1f); home.close();
        icons[HOME] = home;

        Path shield = new Path();
        shield.moveTo(0f, -0.98f); shield.lineTo(0.86f, -0.62f); shield.lineTo(0.86f, 0.1f);
        shield.cubicTo(0.86f, 0.66f, 0.42f, 0.9f, 0f, 1.04f);
        shield.cubicTo(-0.42f, 0.9f, -0.86f, 0.66f, -0.86f, 0.1f);
        shield.lineTo(-0.86f, -0.62f); shield.close();
        icons[ADMIN] = shield;

        Path user = new Path();
        user.addCircle(0f, -0.46f, 0.38f, Path.Direction.CW);
        user.moveTo(-0.86f, 0.96f);
        user.cubicTo(-0.86f, 0.4f, -0.46f, 0.12f, 0f, 0.12f);
        user.cubicTo(0.46f, 0.12f, 0.86f, 0.4f, 0.86f, 0.96f);
        user.close();
        icons[PROFILE] = user;
    }

    // ------------------------------------------------------------ public API

    public void setListener(Listener l) { listener = l; }

    public void setNeonTypeface(Typeface t) { if (t != null) { tf = t; invalidate(); } }

    public void setBottomInset(int px) { bottomInset = Math.max(0, px); requestLayout(); }

    public int getSelected() { return selected; }

    public void setAdminVisible(boolean v) {
        adminVisible = v;
        if (!v && selected == ADMIN) selected = HOME;
        pos = indexOf(selected);
        requestLayout();
        invalidate();
    }

    /** Select a tab without calling the listener. */
    public void setSelected(int id, boolean animate) {
        selected = id;
        float target = indexOf(id);
        if (posA != null) posA.cancel();
        if (animate && isAttachedToWindow() && Math.abs(target - pos) > 0.01f) {
            posA = anim(pos, target, 420, new OvershootInterpolator(1.15f), a -> { pos = (float) a.getAnimatedValue(); invalidate(); });
        } else {
            pos = target;
            invalidate();
        }
    }

    private int[] ids() { return adminVisible ? new int[]{HOME, ADMIN, PROFILE} : new int[]{HOME, PROFILE}; }

    private int indexOf(int id) {
        int[] a = ids();
        for (int i = 0; i < a.length; i++) if (a[i] == id) return i;
        return 0;
    }

    // ------------------------------------------------------------ layout

    private void metrics(int w) {
        barH = Math.max(64 * dp, Math.min(84 * dp, w * 0.2f));
        gt = 10 * dp;
        corner = 26 * dp;
    }

    @Override protected void onMeasure(int ws, int hs) {
        int w = resolveSize((int) (360 * dp), ws);
        metrics(w);
        setMeasuredDimension(w, (int) Math.ceil(gt + barH + bottomInset));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        metrics(w);
        rimSh = new LinearGradient(0, 0, w, 0, new int[]{0xFF22D3EE, 0xFF7C3AED, 0xFFD946EF, 0xFF22D3EE}, null, Shader.TileMode.CLAMP);
        glassSh = new LinearGradient(0, gt, 0, h, 0xF71B1250, 0xFA07030F, Shader.TileMode.CLAMP);
        pos = indexOf(selected);
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
        if (posA != null) posA.cancel();
        if (pressA != null) pressA.cancel();
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

    private int hit(float x, float y) {
        if (y < gt) return -1;
        int n = ids().length;
        return Math.max(0, Math.min(n - 1, (int) (x / (getWidth() / (float) n))));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(e.getX(), e.getY());
                if (pressed < 0) return false;
                if (pressA != null) pressA.cancel();
                pressA = anim(pressT, 1f, 80, new DecelerateInterpolator(), a -> { pressT = (float) a.getAnimatedValue(); invalidate(); });
                return true;
            case MotionEvent.ACTION_UP:
                int idx = hit(e.getX(), e.getY());
                int was = pressed;
                pressed = -1;
                if (pressA != null) pressA.cancel();
                pressA = anim(pressT, 0f, 160, new OvershootInterpolator(2f), a -> { pressT = (float) a.getAnimatedValue(); invalidate(); });
                if (idx >= 0 && idx == was) {
                    int id = ids()[idx];
                    boolean changed = id != selected;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (changed) setSelected(id, true);
                    if (listener != null) listener.onSelected(id, changed);
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                if (pressA != null) pressA.cancel();
                pressA = anim(pressT, 0f, 140, new DecelerateInterpolator(), a -> { pressT = (float) a.getAnimatedValue(); invalidate(); });
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override public boolean performClick() { return super.performClick(); }

    // ------------------------------------------------------------ drawing

    private static int lerp(int a, int b, float f) {
        int aa = (a >>> 24), ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24), br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * f) << 24) | ((int) (ar + (br - ar) * f) << 16) | ((int) (ag + (bg - ag) * f) << 8) | (int) (ab + (bb - ab) * f);
    }

    private int blended(int k) {
        int[] a = ids();
        int i0 = Math.max(0, Math.min(a.length - 1, (int) Math.floor(pos)));
        int i1 = Math.min(a.length - 1, i0 + 1);
        float f = Math.max(0f, Math.min(1f, pos - i0));
        return lerp(COL[a[i0]][k], COL[a[i1]][k], f);
    }

    @Override protected void onDraw(Canvas c) {
        if (rimSh == null) return;
        int[] ids = ids();
        int n = ids.length;
        float w = getWidth(), h = getHeight(), itemW = w / n;
        float ox = (pos + 0.5f) * itemW, oy = gt + barH * 0.38f, R = barH * 0.27f;
        int mid = blended(1);

        r.set(0, gt, w, h + corner * 2);

        // glow along the top edge
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 6; i++) {
            float ex = 2.5f * dp + i * 1.4f * dp;
            p.setColor(((int) (58 * (1f - i / 7f)) << 24) | (mid & 0xFFFFFF));
            r2.set(r); r2.inset(-ex, -ex);
            c.drawRoundRect(r2, corner + ex, corner + ex, p);
        }
        // rim + glass
        p.reset(); p.setAntiAlias(true); p.setShader(rimSh);
        c.drawRoundRect(r, corner, corner, p);
        r2.set(r); r2.inset(2.5f * dp, 2.5f * dp);
        p.reset(); p.setAntiAlias(true); p.setShader(glassSh);
        c.drawRoundRect(r2, corner - 2.5f * dp, corner - 2.5f * dp, p);
        // soft top gloss
        r2.set(corner, gt + 3 * dp, w - corner, gt + barH * 0.3f);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, r2.top, 0, r2.bottom, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r2, 16 * dp, 16 * dp, p);

        // glowing light notch on the top edge above the selected tab
        float nw = itemW * 0.6f;
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(ox - nw / 2, 0, ox + nw / 2, 0,
                new int[]{0x00FFFFFF, 0xFFFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        r2.set(ox - nw / 2, gt - 0.5f * dp, ox + nw / 2, gt + 3 * dp);
        c.drawRoundRect(r2, 2 * dp, 2 * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(ox, gt, nw * 0.6f, (mid & 0xFFFFFF) | 0x66000000, mid & 0xFFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(ox, gt, nw * 0.6f, p);

        drawOrb(c, ox, oy, R, blended(0), mid, blended(2));

        // tabs
        for (int i = 0; i < n; i++) {
            int id = ids[i];
            float s = Math.max(0f, Math.min(1f, 1f - Math.abs(pos - i)));
            float cx = (i + 0.5f) * itemW;
            float press = (pressed == i) ? pressT : 0f;
            float u = barH * 0.15f * (1f + 0.2f * s) * (1f - 0.1f * press);
            float cy = oy - s * 1.5f * dp + (1f - s) * barH * 0.03f;

            c.save();
            c.translate(cx, cy);
            c.scale(u, u);
            if (s > 0.02f) { // selected look: white glossy glyph with dark outline
                p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.FILL_AND_STROKE);
                p.setStrokeJoin(Paint.Join.ROUND); p.setStrokeWidth(2.4f * dp / u);
                p.setColor(((int) (255 * s) << 24) | (INK & 0xFFFFFF));
                c.drawPath(icons[id], p);
                p.reset(); p.setAntiAlias(true); p.setColor(((int) (255 * s) << 24) | 0xFFFFFF);
                c.drawPath(icons[id], p);
            }
            if (s < 0.98f) {  // idle look: dim lilac glyph
                p.reset(); p.setAntiAlias(true); p.setColor(((int) (255 * (1f - s)) << 24) | 0x9C97D8);
                c.drawPath(icons[id], p);
            }
            c.restore();

            // label
            float ty = gt + barH * 0.86f;
            tp.setTypeface(tf); tp.setLetterSpacing(0.04f);
            tp.setTextSize(Math.max(11 * dp, barH * 0.16f));
            if (s > 0.02f) {
                tp.setStyle(Paint.Style.STROKE); tp.setStrokeWidth(tp.getTextSize() * 0.3f);
                tp.setColor(((int) (0x66 * s) << 24) | (COL[id][1] & 0xFFFFFF));
                c.drawText(LABELS[id], cx, ty, tp);
            }
            tp.setStyle(Paint.Style.FILL);
            tp.setColor(lerp(0xFFA5A0D8, 0xFFFFFFFF, s));
            c.drawText(LABELS[id], cx, ty, tp);
        }
    }

    /** Glossy planet with tilted orbit ring and moon, plus twinkling sparkle. */
    private void drawOrb(Canvas c, float ox, float oy, float R, int light, int mid, int dark) {
        c.save();
        c.rotate(-22f, ox, oy);
        r2.set(ox - R * 1.75f, oy - R * 0.62f, ox + R * 1.75f, oy + R * 0.62f);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.4f * dp); p.setColor(INK);
        c.drawOval(r2, p);
        p.setStrokeWidth(1.9f * dp); p.setColor(mid);
        c.drawOval(r2, p);
        c.restore();

        p.reset(); p.setAntiAlias(true); p.setColor(0x66000000 | (mid & 0xFFFFFF));
        c.drawCircle(ox, oy, R * 1.4f, p);
        p.reset(); p.setAntiAlias(true); p.setColor(INK);
        c.drawCircle(ox, oy, R + 2 * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(ox - R * 0.3f, oy - R * 0.4f, R * 1.5f,
                new int[]{0xFFFFFFFF, light, mid, dark}, new float[]{0f, 0.15f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(ox, oy, R, p);

        // moon
        double ang = idle * Math.PI * 2, rot = Math.toRadians(-22);
        float mxp = (float) Math.cos(ang) * R * 1.75f, myp = (float) Math.sin(ang) * R * 0.62f;
        float mx = ox + (float) (mxp * Math.cos(rot) - myp * Math.sin(rot));
        float my = oy + (float) (mxp * Math.sin(rot) + myp * Math.cos(rot));
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFFFFFF);
        c.drawCircle(mx, my, 2.4f * dp, p);

        // sparkle
        float tw = 0.75f + 0.25f * (float) Math.sin(idle * 12.566f);
        float sr = R * 0.34f * tw, sx = ox + R * 1.05f, sy = oy - R * 1.05f;
        path.reset();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45);
            float rr = (i % 2 == 0) ? sr : sr * 0.22f;
            float x = sx + (float) Math.cos(a) * rr, y = sy + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFBCFE8);
        c.drawPath(path, p);
    }
}
