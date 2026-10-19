package com.odyssey.game;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * "Select Difficulty Mode" popup for Code Odyssey, in the neon logo style.
 * EASY = teal, MEDIUM = violet, HARD = red. Locked rows shake and flash red when tapped.
 * Plays sounds through NeonSound (WHOOSH on open, SUCCESS on pick, ERROR on locked, CLICK on cancel).
 *
 * Usage:
 *   NeonDifficultyPicker.show(this, new boolean[]{true, true, hardUnlocked},
 *       "Clear / Perfect Medium quiz to unlock",
 *       new NeonDifficultyPicker.Listener() {
 *           public void onPick(int level) { // 0 = EASY, 1 = MEDIUM, 2 = HARD
 *               startQuiz(level); }
 *           public void onCancel() { } });
 */
public class NeonDifficultyPicker extends View {

    public interface Listener {
        void onPick(int level);
        void onCancel();
    }

    public static final int EASY = 0, MEDIUM = 1, HARD = 2;

    private static final String[] NAMES = {"EASY", "MEDIUM", "HARD"};
    private static final int[][] ACC = { // light, mid, dark, glow rgb
            {0xFF99F6E4, 0xFF2DD4BF, 0xFF0D9488, 0x2DD4BF},
            {0xFFF0ABFC, 0xFFC084FC, 0xFF7C3AED, 0xA855F7},
            {0xFFFDA4AF, 0xFFF43F5E, 0xFFBE123C, 0xF43F5E}};
    private static final int INK = 0xFF150F8F;

    private final float dp, sd;
    private final Random rnd = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF(), panel = new RectF(), cancelR = new RectF();
    private final RectF[] rows = {new RectF(), new RectF(), new RectF()};
    private final Path path = new Path();
    private final ArrayList<float[]> parts = new ArrayList<>(); // x,y,vx,vy,life,color,size
    private final ArrayList<float[]> rings = new ArrayList<>(); // x,y,r,a,color

    private boolean[] unlocked = {true, true, false};
    private String hint = "Clear / Perfect Medium quiz to unlock";
    private Listener listener;
    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private float W, H, panelW, panelH, pad, rowH, gap, cancelH, labelBase, titleBase, labelSize, titleSize;
    private final float[] rowTop = new float[3], rowHt = new float[3];
    private float cancelTop;

    private float open, closeT, clock, pressS, shakeT = 1f, rippleT = 1f, launchT, rpx, rpy, denyK;
    private int pressIdx = -1, shakeIdx = -1, rippleIdx = -1, launchIdx = -1, denyIdx = -1, pickedLevel = -1;
    private boolean closing, finished, downOutside;
    private long lastNs;

    public NeonDifficultyPicker(Context c) { this(c, null); }

    public NeonDifficultyPicker(Context c, AttributeSet a) {
        super(c, a);
        dp = c.getResources().getDisplayMetrics().density;
        sd = c.getResources().getDisplayMetrics().scaledDensity;
        setClickable(true);
        setFocusableInTouchMode(true);
        tp.setStrokeJoin(Paint.Join.ROUND);
    }

    // ------------------------------------------------------------ public API

    /** unlocked = {easy, medium, hard}. hardHint is shown under HARD while it is locked (null hides it). */
    public static NeonDifficultyPicker show(Activity act, boolean[] unlocked, String hardHint, Listener l) {
        ViewGroup root = (ViewGroup) act.getWindow().getDecorView();
        NeonDifficultyPicker v = new NeonDifficultyPicker(act);
        if (unlocked != null && unlocked.length >= 3) v.unlocked = unlocked.clone();
        v.hint = hardHint;
        v.listener = l;
        root.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return v;
    }

    public void setNeonTypeface(Typeface t) { if (t != null) { tf = t; build(); } }

    // ------------------------------------------------------------ layout

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastNs = 0;
        requestFocus();
        NeonSound.play(getContext(), NeonSound.Sfx.WHOOSH);
        postInvalidateOnAnimation();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { W = w; H = h; build(); }

    private void build() {
        if (W <= 0) return;
        panelW = Math.min(W - 32 * dp, 420 * dp);
        pad = 20 * dp; rowH = 64 * dp; gap = 12 * dp; cancelH = 44 * dp;
        labelSize = 12 * sd; titleSize = 22 * sd;
        float y = pad;
        labelBase = y + labelSize; y = labelBase + 12 * dp;
        titleBase = y + titleSize; y = titleBase + 20 * dp;
        for (int i = 0; i < 3; i++) {
            rowTop[i] = y;
            rowHt[i] = rowH + ((!unlocked[i] && hint != null) ? 16 * dp : 0f);
            y += rowHt[i] + gap;
        }
        y += 4 * dp;
        cancelTop = y; y += cancelH + pad;
        panelH = y;
        float px = (W - panelW) / 2f, py = (H - panelH) / 2f - 8 * dp;
        panel.set(px, py, px + panelW, py + panelH);
        for (int i = 0; i < 3; i++) rows[i].set(px + pad, py + rowTop[i], px + panelW - pad, py + rowTop[i] + rowHt[i]);
        cancelR.set(px + pad, py + cancelTop, px + panelW - pad, py + cancelTop + cancelH);
    }

    // ------------------------------------------------------------ effects & input

    private void burst(float x, float y, int n, int rgb) {
        for (int i = 0; i < n && parts.size() < 300; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float sp = (80f + rnd.nextFloat() * 300f) * dp, life = 0.5f + rnd.nextFloat() * 0.7f;
            parts.add(new float[]{x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, life, rnd.nextBoolean() ? 0xFFFFFF : rgb, (1.2f + rnd.nextFloat() * 2.4f) * dp});
        }
        rings.add(new float[]{x, y, 0f, 1f, rgb});
    }

    private int hit(float x, float y) {
        for (int i = 0; i < 3; i++) if (rows[i].contains(x, y)) return i;
        if (cancelR.contains(x, y)) return 3;
        return -1;
    }

    private void pick(int i) {
        launchIdx = i; launchT = 0f; pickedLevel = i;
        burst(rows[i].centerX(), rows[i].centerY(), 44, ACC[i][3]);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        NeonSound.play(NeonSound.Sfx.SUCCESS);
    }

    private void deny(int i) {
        shakeIdx = i; shakeT = 0f; denyIdx = i; denyK = 1f;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        NeonSound.play(NeonSound.Sfx.ERROR);
    }

    private void cancel() {
        if (closing) return;
        NeonSound.play(NeonSound.Sfx.CLICK);
        startClose(-1);
    }

    private void startClose(int level) { closing = true; closeT = 0f; pickedLevel = level; }

    @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { cancel(); return true; }
        return super.onKeyDown(keyCode, e);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (closing || launchIdx >= 0) return true;
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downOutside = !panel.contains(x, y);
                pressIdx = hit(x, y);
                if (pressIdx >= 0 && pressIdx < 3) { rippleIdx = pressIdx; rpx = x; rpy = y; rippleT = 0f; }
                if (pressIdx >= 0) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressIdx >= 0 && hit(x, y) != pressIdx) pressIdx = -1;
                return true;
            case MotionEvent.ACTION_UP:
                int h = hit(x, y), was = pressIdx;
                pressIdx = -1;
                if (downOutside && !panel.contains(x, y)) { cancel(); return true; }
                if (was >= 0 && h == was) {
                    if (was == 3) cancel();
                    else if (unlocked[was]) pick(was);
                    else deny(was);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressIdx = -1;
                return true;
        }
        return true;
    }

    // ------------------------------------------------------------ update

    private static float ease(float x) { x = x < 0 ? 0 : (x > 1 ? 1 : x); return x * x * (3 - 2 * x); }

    private static float clamp(float x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }

    private void update(float dt) {
        clock += dt;
        open = Math.min(1f, open + dt / 0.5f);
        pressS += ((pressIdx >= 0 ? 1f : 0f) - pressS) * (1f - (float) Math.exp(-dt * 20f));
        shakeT = Math.min(1f, shakeT + dt / 0.45f);
        rippleT = Math.min(1f, rippleT + dt / 0.6f);
        denyK *= (float) Math.exp(-dt * 3.2f);
        if (launchIdx >= 0) { launchT += dt; if (launchT >= 0.4f && !closing) startClose(launchIdx); }
        if (closing) {
            closeT += dt / 0.26f;
            if (closeT >= 1f && !finished) {
                finished = true;
                if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
                if (listener != null) { if (pickedLevel >= 0) listener.onPick(pickedLevel); else listener.onCancel(); }
            }
        }
        Iterator<float[]> it = parts.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            float d = (float) Math.exp(-dt * 1.8f);
            q[2] *= d; q[3] *= d; q[0] += q[2] * dt; q[1] += q[3] * dt; q[4] -= dt;
            if (q[4] <= 0f) it.remove();
        }
        it = rings.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            q[2] += 560 * dp * dt; q[3] -= dt * 1.4f;
            if (q[3] <= 0f) it.remove();
        }
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (W <= 0 || panelH <= 0) return;
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        update(dt);

        float alpha = ease(open) * (1f - ease(closeT));
        p.reset(); p.setColor(((int) (190 * alpha) << 24) | 0x050212);
        c.drawRect(0, 0, W, H, p);

        float sc = (0.92f + 0.08f * ease(open)) * (1f - 0.05f * ease(closeT));
        c.saveLayerAlpha(0, 0, W, H, (int) (255 * alpha));
        c.scale(sc, sc, panel.centerX(), panel.centerY());
        drawPanel(c);
        for (int i = 0; i < 3; i++) drawRow(c, i);
        drawCancel(c);
        c.restore();

        for (float[] q : rings) {
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(9 * dp);
            p.setColor(((int) (80 * Math.max(0f, q[3])) << 24) | ((int) q[4] & 0xFFFFFF));
            c.drawCircle(q[0], q[1], q[2], p);
            p.setStrokeWidth(2.2f * dp); p.setColor(((int) (255 * Math.max(0f, q[3])) << 24) | 0xFFFFFF);
            c.drawCircle(q[0], q[1], q[2], p);
        }
        p.reset(); p.setAntiAlias(true);
        for (float[] q : parts) {
            float al = clamp(q[4]);
            p.setColor(((int) (255 * al) << 24) | ((int) q[5] & 0xFFFFFF));
            c.drawCircle(q[0], q[1], q[6] * (0.4f + 0.6f * al), p);
        }
        postInvalidateOnAnimation();
    }

    private void drawPanel(Canvas c) {
        float radius = 26 * dp;
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 7; i++) {
            float ex = 2.5f * dp + i * 1.8f * dp;
            p.setColor(((int) (60 * (1f - i / 8f)) << 24) | (i % 2 == 0 ? 0x22D3EE : 0xA855F7));
            r.set(panel); r.inset(-ex, -ex);
            c.drawRoundRect(r, radius + ex, radius + ex, p);
        }
        r.set(panel); r.inset(-2.5f * dp, -2.5f * dp);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(panel.left, panel.top, panel.right, panel.bottom,
                new int[]{0xFF22D3EE, 0xFFA855F7, 0xFFD946EF, 0xFF22D3EE}, null, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, radius + 2.5f * dp, radius + 2.5f * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, panel.top, 0, panel.bottom, 0xFA1A1050, 0xFA09041F, Shader.TileMode.CLAMP));
        c.drawRoundRect(panel, radius, radius, p);
        r.set(panel.left + radius, panel.top + 3 * dp, panel.right - radius, panel.top + panelH * 0.22f);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, radius * 0.6f, radius * 0.6f, p);

        // header
        float x = panel.left + pad, top = panel.top;
        tp.setTypeface(tf); tp.setShader(null); tp.setStyle(Paint.Style.FILL); tp.setTextAlign(Paint.Align.LEFT);
        tp.setLetterSpacing(0.16f); tp.setTextSize(labelSize); tp.setColor(0xFF2DD4BF);
        c.drawText("CHALLENGE DIFFICULTY", x, top + labelBase, tp);
        tp.setLetterSpacing(0.01f);
        outlined(c, "Select Difficulty Mode", x, top + titleBase, titleSize, Paint.Align.LEFT,
                new LinearGradient(0, top + titleBase - titleSize, 0, top + titleBase, 0xFFFFFFFF, 0xFFDDD6FE, Shader.TileMode.CLAMP), 1f, 0xA855F7);

        // sparkle on the corner
        float tw = 0.75f + 0.25f * (float) Math.sin(clock * 4f), sr = 11 * dp * tw, sx = panel.right - 26 * dp, sy = panel.top + 22 * dp;
        starPath(sx, sy, sr);
        p.reset(); p.setAntiAlias(true); p.setColor(0x66F0ABFC); c.drawCircle(sx, sy, sr * 1.4f, p);
        p.setColor(0xFFFBCFE8); c.drawPath(path, p);
    }

    private void drawRow(Canvas c, int i) {
        float in = ease((open - 0.12f * i - 0.18f) / 0.5f);
        if (in <= 0f) return;
        RectF rr = rows[i];
        boolean ok = unlocked[i];
        int[] a = ACC[i];
        float press = (pressIdx == i) ? pressS : 0f;
        float shake = (shakeIdx == i && shakeT < 1f) ? (float) Math.sin(shakeT * Math.PI * 7) * (1f - shakeT) * 9 * dp : 0f;
        float flashK = (launchIdx == i) ? Math.max(0f, 1f - launchT / 0.4f) : 0f;
        float pulse = (launchIdx == i) ? (float) Math.sin(Math.min(1f, launchT / 0.4f) * Math.PI) : 0f;
        float radius = 18 * dp, sc = (1f - 0.04f * press) * (1f + 0.035f * pulse);
        float cy = rr.top + ((!ok && hint != null) ? 26 * dp : rowH / 2f);

        c.save();
        c.translate(shake, (1f - in) * 30 * dp);
        c.scale(sc, sc, rr.centerX(), rr.centerY());

        if (ok) {
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
            for (int k = 1; k <= 5; k++) {
                float ex = 2f * dp + k * 1.4f * dp;
                p.setColor(((int) (Math.min(255f, 56f * (1f - k / 6f) * (1f + flashK * 3f))) << 24) | a[3]);
                r.set(rr); r.inset(-ex, -ex);
                c.drawRoundRect(r, radius + ex, radius + ex, p);
            }
        }
        r.set(rr); r.inset(-2 * dp, -2 * dp);
        p.reset(); p.setAntiAlias(true);
        if (ok) p.setShader(new LinearGradient(rr.left, 0, rr.right, 0, new int[]{a[1], a[0], a[1]}, null, Shader.TileMode.CLAMP));
        else p.setColor(denyIdx == i ? ((int) (255 * (0.25f + 0.75f * denyK)) << 24) | 0xF43F5E : 0xFF2E2570);
        c.drawRoundRect(r, radius + 2 * dp, radius + 2 * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, rr.top, 0, rr.bottom, ok ? 0xF01B1250 : 0xF2100C2B, ok ? 0xF00B0626 : 0xF20A0820, Shader.TileMode.CLAMP));
        c.drawRoundRect(rr, radius, radius, p);
        if (flashK > 0f) { p.reset(); p.setAntiAlias(true); p.setColor(((int) (120 * flashK) << 24) | a[3]); c.drawRoundRect(rr, radius, radius, p); }
        if (denyIdx == i && denyK > 0.02f) { p.reset(); p.setAntiAlias(true); p.setColor(((int) (90 * denyK) << 24) | 0xF43F5E); c.drawRoundRect(rr, radius, radius, p); }
        if (ok) {
            r.set(rr.left + radius, rr.top + 2 * dp, rr.right - radius, rr.top + rowH * 0.42f);
            p.reset(); p.setAntiAlias(true);
            p.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, radius * 0.6f, radius * 0.6f, p);
        }

        // shine + ripple (clipped)
        path.reset(); path.addRoundRect(rr, radius, radius, Path.Direction.CW);
        c.save(); c.clipPath(path);
        if (ok) {
            float st = clamp((((clock * 0.28f + i * 0.27f) % 1f) - 0.55f) / 0.45f);
            if (st > 0f && st < 1f) {
                float bw = rowH, x = rr.left - bw + st * (rr.width() + 2 * bw);
                p.reset(); p.setAntiAlias(true);
                p.setShader(new LinearGradient(x, 0, x + bw, 0, new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
                Path band = new Path(); float sk = rowH * 0.3f;
                band.moveTo(x, rr.top); band.lineTo(x + bw, rr.top); band.lineTo(x + bw - sk, rr.bottom); band.lineTo(x - sk, rr.bottom); band.close();
                c.drawPath(band, p);
            }
        }
        if (rippleIdx == i && rippleT < 1f) {
            float rad = rowH * (0.3f + 2.6f * rippleT);
            int al = (int) (255 * (1f - rippleT));
            p.reset(); p.setAntiAlias(true);
            p.setShader(new RadialGradient(rpx, rpy, rad, new int[]{((al * 0xAA / 255) << 24) | 0xFFFFFF, ((al * 0x55 / 255) << 24) | a[3], a[3]},
                    new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(rpx, rpy, rad, p);
        }
        c.restore();

        // orb with difficulty stars
        float R = 17 * dp, ox = rr.left + 16 * dp + R;
        drawOrb(c, ox, cy, R, i, ok);

        // badge
        tp.setTypeface(tf); tp.setLetterSpacing(0.1f); tp.setTextSize(11 * sd);
        String badge = ok ? "UNLOCKED" : "LOCKED";
        float bw = tp.measureText(badge) + (ok ? 24 * dp : 40 * dp), bh = 26 * dp;
        float bRight = rr.right - 14 * dp, bx = bRight - bw;
        r.set(bx, cy - bh / 2f, bRight, cy + bh / 2f);
        int bc = ok ? a[1] : 0xFFF43F5E;
        p.reset(); p.setAntiAlias(true); p.setColor(0x30000000 | (bc & 0xFFFFFF)); c.drawRoundRect(r, bh / 2f, bh / 2f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f * dp); p.setColor(bc); c.drawRoundRect(r, bh / 2f, bh / 2f, p);
        tp.setShader(null); tp.setStyle(Paint.Style.FILL); tp.setTextAlign(Paint.Align.LEFT);
        tp.setColor(ok ? a[0] : 0xFFFB7185);
        float tx = bx + 12 * dp;
        if (!ok) { lockIcon(c, bx + 17 * dp, cy, 0xFFFB7185); tx = bx + 28 * dp; }
        c.drawText(badge, tx, cy - (tp.ascent() + tp.descent()) / 2f, tp);

        // title
        float tl = ox + R + 12 * dp, avail = bx - tl - 8 * dp, size = 20 * sd;
        tp.setLetterSpacing(0.08f); tp.setTextSize(size);
        while (tp.measureText(NAMES[i]) > avail && size > 11 * sd) { size -= sd * 0.5f; tp.setTextSize(size); }
        float base = cy - (tp.ascent() + tp.descent()) / 2f;
        if (ok) {
            outlined(c, NAMES[i], tl, base, size, Paint.Align.LEFT,
                    new LinearGradient(0, base - size * 0.8f, 0, base, 0xFFFFFFFF, a[0], Shader.TileMode.CLAMP), 1f, a[3]);
        } else {
            tp.setStyle(Paint.Style.FILL); tp.setShader(null); tp.setTextAlign(Paint.Align.LEFT); tp.setColor(0xFF6B6794);
            c.drawText(NAMES[i], tl, base, tp);
            if (hint != null) {
                tp.setLetterSpacing(0f); tp.setTextSize(12 * sd); tp.setColor(0xFF8F8AB8);
                tp.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                c.drawText(hint, rr.left + 16 * dp, rr.top + 58 * dp, tp);
                tp.setTypeface(tf);
            }
        }
        c.restore();
    }

    private void drawOrb(Canvas c, float ox, float oy, float R, int i, boolean ok) {
        int[] a = ACC[i];
        if (ok) { p.reset(); p.setAntiAlias(true); p.setColor(0x55000000 | a[3]); c.drawCircle(ox, oy, R * 1.45f, p); }
        p.reset(); p.setAntiAlias(true); p.setColor(INK); c.drawCircle(ox, oy, R + 1.8f * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(ok ? new RadialGradient(ox - R * 0.3f, oy - R * 0.4f, R * 1.5f, new int[]{0xFFFFFFFF, a[0], a[1], a[2]}, new float[]{0f, 0.15f, 0.5f, 1f}, Shader.TileMode.CLAMP)
                : new RadialGradient(ox - R * 0.3f, oy - R * 0.4f, R * 1.5f, new int[]{0xFF4A4380, 0xFF2A2460, 0xFF1A1548}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(ox, oy, R, p);
        int n = i + 1;
        float sr = n == 1 ? 6.5f * dp : (n == 2 ? 4.8f * dp : 3.9f * dp);
        for (int k = 0; k < n; k++) {
            float sx = ox + (k - (n - 1) / 2f) * sr * 2.15f;
            float tw = ok ? 0.85f + 0.15f * (float) Math.sin(clock * 3f + k + i) : 1f;
            starPath(sx, oy, sr * tw);
            p.reset(); p.setAntiAlias(true); p.setColor(ok ? 0xFFFFFFFF : 0x66FFFFFF);
            c.drawPath(path, p);
        }
    }

    private void lockIcon(Canvas c, float cx, float cy, int color) {
        float w = 10 * dp, h = 8 * dp;
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp); p.setColor(color);
        r.set(cx - w * 0.36f, cy - h - 0.5f * dp, cx + w * 0.36f, cy - h * 0.15f + 2 * dp);
        c.drawArc(r, 180, 180, false, p);
        p.setStyle(Paint.Style.FILL);
        r.set(cx - w / 2f, cy - h * 0.2f, cx + w / 2f, cy - h * 0.2f + h);
        c.drawRoundRect(r, 2 * dp, 2 * dp, p);
    }

    private void drawCancel(Canvas c) {
        float in = ease((open - 0.5f) / 0.4f);
        if (in <= 0f) return;
        float press = (pressIdx == 3) ? pressS : 0f, rad = cancelH / 2f;
        c.save();
        c.translate(0, (1f - in) * 20 * dp);
        c.scale(1f - 0.04f * press, 1f - 0.04f * press, cancelR.centerX(), cancelR.centerY());
        p.reset(); p.setAntiAlias(true); p.setColor(0xF20F0B2B); c.drawRoundRect(cancelR, rad, rad, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f * dp); p.setColor(press > 0.1f ? 0xFF7C3AED : 0xFF3A2F7A);
        c.drawRoundRect(cancelR, rad, rad, p);
        tp.setTypeface(tf); tp.setShader(null); tp.setStyle(Paint.Style.FILL); tp.setTextAlign(Paint.Align.CENTER);
        tp.setLetterSpacing(0.18f); tp.setTextSize(13 * sd); tp.setColor(press > 0.1f ? 0xFFFFFFFF : 0xFFC4B5FD);
        c.drawText("CANCEL", cancelR.centerX(), cancelR.centerY() - (tp.ascent() + tp.descent()) / 2f, tp);
        c.restore();
    }

    private void starPath(float x, float y, float rad) {
        path.reset();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45);
            float rr = (i % 2 == 0) ? rad : rad * 0.26f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
    }

    /** Logo-style text: glow, dark outline, gradient fill. Set tp.setLetterSpacing before calling. */
    private void outlined(Canvas c, String s, float x, float y, float size, Paint.Align al, Shader fill, float alpha, int glowRgb) {
        alpha = Math.max(0f, Math.min(1f, alpha));
        tp.setTypeface(tf); tp.setTextAlign(al); tp.setTextSize(size); tp.setShader(null);
        int a = (int) (255 * alpha);
        tp.setStyle(Paint.Style.STROKE);
        tp.setStrokeWidth(size * 0.44f); tp.setColor(((int) (0x55 * alpha) << 24) | (glowRgb & 0xFFFFFF));
        c.drawText(s, x, y, tp);
        tp.setStrokeWidth(size * 0.24f); tp.setColor((a << 24) | 0x150F8F);
        c.drawText(s, x, y + size * 0.05f, tp);
        c.drawText(s, x, y, tp);
        tp.setStyle(Paint.Style.FILL); tp.setShader(fill); tp.setAlpha(a);
        c.drawText(s, x, y, tp);
        tp.setShader(null);
    }
}
